package com.by1337.auc.common.backend.lot;

import com.by1337.auc.common.auc.AucLot;
import com.by1337.auc.common.auc.BaseLot;
import com.by1337.auc.common.auc.VaultLot;
import com.by1337.auc.common.handler.BAucRuntime;
import com.by1337.auc.common.handler.GetPostChannelHandler;
import com.by1337.auc.common.network.a2a.A2AFlagResponse;
import com.by1337.auc.common.network.c2s.*;
import com.by1337.auc.common.network.s2c.*;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.pipeline.ChannelContext;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.channel.pipeline.Connection;
import dev.by1337.sync.common.channel.pipeline.Pipeline;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.util.BSUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LotsRepositoryBackend extends GetPostChannelHandler {
    private static final Logger log = LoggerFactory.getLogger(LotsRepositoryBackend.class);
    private BAucRuntime channel;
    private Pipeline pipeline;

    private AucLotRepo<AucLot> lots;
    private AucLotRepo<VaultLot> vault;

    public LotsRepositoryBackend() {
        registerGet(C2SSubtractLotRequest.class, this::onSubtractLot);
        registerGet(C2SAddNewLotRequest.class, this::newNewItem);
        registerGet(C2SRemoveLotRequest.class, this::removeLot);
        registerGet(C2SMove2VaultRequest.class, this::move2vault);
        registerGet(C2SAddNewVaultRequest.class, this::addNewVault);
        registerGet(C2SRemoveVaultLotRequest.class, this::removeVaultLot);
        registerGet(C2SGetLotRequest.class, this::getLot);
        registerGet(C2SGetVaultLotRequest.class, this::getVaultLot);
        registerPost(C2SGetAllLotsRequest.class, this::onGetAllLots);
        registerPost(C2SGetAllVaultLotsRequest.class, this::onGetAllVaultLots);
    }

    private void onGetAllVaultLots(ChannelContext ctx, C2SGetAllVaultLotsRequest r) {
        int[] uids = vault.lots.keySet().toIntArray();
        sendUids(uids, 0, 4096, ctx.connection(), S2CActualVaultLotsUids::new);
    }
    private ResponseFuture<S2COptionalVaultLot> getVaultLot(C2SGetVaultLotRequest r) {
        return new ResponseFuture<>(new S2COptionalVaultLot(vault.get(r.uid())));
    }

    private void onGetAllLots(ChannelContext ctx, C2SGetAllLotsRequest r) {
        int[] uids = lots.lots.keySet().toIntArray();
        sendUids(uids, 0, 4096, ctx.connection(), S2CActualLotsUids::new);
    }

    private void sendUids(int[] uids, int from, int limit, Connection connection, Function<int[], ExpectsResponse<A2AFlagResponse>> f) {
        if (from >= uids.length) return;
        int len = Math.min(uids.length - from, limit);
        if (len <= 0) return;
        log.info("SEND {}-{} of {}", from, from+len, uids.length);
        int[] arr = new int[len];
        System.arraycopy(uids, from, arr, 0, len);
        f.apply(arr).request(pipeline, connection, 60_000).ifPresent(flag -> {
            if (!flag.flag()) return;
            sendUids(uids, from + len, limit, connection, f);
        });
    }

    private ResponseFuture<S2COptionalLot> getLot(C2SGetLotRequest r) {
        return new ResponseFuture<>(new S2COptionalLot(lots.get(r.uid())));
    }

    @Override
    public void init(ChannelRuntime runtime) {
        if (!(runtime instanceof BAucRuntime server)) throw new IllegalArgumentException("Invalid runtime type");
        channel = server;
        pipeline = server.pipeline();
        lots = new AucLotRepo<>(AucLot::read, new BlobRepository(server.database().dataSource(), server.name() + "_lots_repository"));
        vault = new AucLotRepo<>(VaultLot::read, new BlobRepository(server.database().dataSource(), server.name() + "_vault_repository"));
        try {
            lots.loadAll();
            vault.loadAll();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load lots!", e);
        }
    }

    private ResponseFuture<A2AFlagResponse> onSubtractLot(C2SSubtractLotRequest r) {
        var lot = lots.get(r.uid());
        if (lot == null || lot.count() < r.count()) return new ResponseFuture<>(new A2AFlagResponse(false));
        int newCount = lot.count() - r.count();
        if (newCount <= 0) {
            lots.remove(r.uid());
            channel.broadcast(new S2CRemoveVaultLotPacket(r.uid())); //broadcast
            return new ResponseFuture<>(new A2AFlagResponse(true));
        }
        AucLot newLot = new AucLot(
                lot.uid(),
                lot.item(),
                lot.owner(),
                lot.createdDate(),
                lot.removalDate(),
                newCount,
                lot.lprice_for_one * newCount
        );
        lots.update(newLot);
        channel.broadcast(new S2CLotCountChange(r.uid(), newCount)); //broadcast
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }

    private ResponseFuture<A2AFlagResponse> removeVaultLot(C2SRemoveVaultLotRequest rm) {
        VaultLot removed = vault.remove(rm.uid());
        if (removed == null) return new ResponseFuture<>(new A2AFlagResponse(false));
        channel.broadcast(new S2CRemoveVaultLotPacket(rm.uid())); //broadcast
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }


    private ResponseFuture<A2AFlagResponse> addNewVault(C2SAddNewVaultRequest packet) {
        var now = System.currentTimeMillis();
        var vaultLot = vault.insert(new VaultLot(
                -1,
                packet.itemId,
                packet.owner,
                now + packet.storeDuration,
                packet.count,
                packet.price
        ));
        channel.broadcast(new S2CVaultLotUpdate(vaultLot)); //broadcast
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }

    private ResponseFuture<A2AFlagResponse> move2vault(C2SMove2VaultRequest move) {
        var lot = lots.remove(move.uid());
        if (lot == null) return new ResponseFuture<>(new A2AFlagResponse(false));
        var vaultLot = vault.insert(new VaultLot(
                -1,
                lot.item(),
                move.newOwner(),
                System.currentTimeMillis() + move.storeDuration(),
                lot.count(),
                lot.lprice()
        ));
        channel.broadcast(new S2CRemoveLotPacket(lot.uid())); //broadcast
        channel.broadcast(new S2CVaultLotUpdate(vaultLot)); //broadcast
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }


    private ResponseFuture<A2AFlagResponse> removeLot(C2SRemoveLotRequest remove) {
        var flag = lots.remove(remove.uid()) != null;
        if (flag) {
            channel.broadcast(new S2CRemoveLotPacket(remove.uid())); //broadcast
        }
        return new ResponseFuture<>(new A2AFlagResponse(flag));
    }

    private ResponseFuture<A2AFlagResponse> newNewItem(C2SAddNewLotRequest packet) {
        var now = System.currentTimeMillis();
        var lot = lots.insert(new AucLot(
                -1,
                packet.itemId,
                packet.owner,
                now,
                now + packet.sellingDuration,
                packet.count,
                packet.price
        ));
        channel.broadcast(new S2CLotUpdate(lot)); //broadcast
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }


    @Override
    public void close() {
    }

    private static class AucLotRepo<T extends BaseLot> {
        private final Int2ObjectOpenHashMap<T> lots = new Int2ObjectOpenHashMap<>();
        private final BiFunction<ByteBuf, Integer, T> reader;
        private final BlobRepository lost_repo;

        private AucLotRepo(BiFunction<ByteBuf, Integer, T> reader, BlobRepository lostRepo) {
            this.reader = reader;
            lost_repo = lostRepo;
        }

        private void loadAll() throws SQLException {
            lots.clear();
            for (BlobRepository.Record record : lost_repo.loadAll()) {
                int uid = record.id();
                T lot = reader.apply(Unpooled.wrappedBuffer(record.data()), uid);
                lots.put(uid, lot);
            }
        }

        public @Nullable T get(int uid) {
            return lots.get(uid);
        }

        public @Nullable T insert(T lot0) {
            try {
                int id = lost_repo.insert(lot0.asBytes());
                T lot = (T) lot0.withUid(id);
                lots.put(id, lot);
                return lot;
            } catch (SQLException e) {
                log.error("Failed to insert lot", e);
                return null;
            }
        }

        public @Nullable T remove(int id) {
            var v = lots.remove(id);
            if (v != null) BSUtils.safe(() -> lost_repo.remove(id));
            return v;
        }

        public void update(T lot) {
            lots.put(lot.uid(), lot);
            BSUtils.safe(() -> lost_repo.update(lot.uid(), lot.asBytes()));
        }
    }
}