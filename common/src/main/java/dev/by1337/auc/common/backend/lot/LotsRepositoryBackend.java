package dev.by1337.auc.common.backend.lot;

import dev.by1337.auc.common.auc.AucLot;
import dev.by1337.auc.common.auc.BaseLot;
import dev.by1337.auc.common.auc.VaultLot;
import dev.by1337.auc.common.auc.log.impl.LotExpirationLog;
import dev.by1337.auc.common.auc.log.impl.VaultLotExpirationLog;
import dev.by1337.auc.common.backend.log.LogRepositoryBackend;
import dev.by1337.auc.common.db.DataBatcher;
import dev.by1337.auc.common.handler.BAucRuntime;
import dev.by1337.auc.common.handler.GetPostChannelHandler;
import dev.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.auc.common.network.c2s.*;
import dev.by1337.auc.common.network.s2c.*;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.pipeline.ChannelContext;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.channel.pipeline.Connection;
import dev.by1337.sync.common.channel.pipeline.Pipeline;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.work.EventLoopWorker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LotsRepositoryBackend extends GetPostChannelHandler {
    private static final long ONE_DAY_MS = TimeUnit.DAYS.toMillis(1);
    private static final Logger log = LoggerFactory.getLogger(LotsRepositoryBackend.class);
    private BAucRuntime channel;
    private Pipeline pipeline;
    private EventLoopWorker worker;

    private AucLotRepo<AucLot> lots;
    private AucLotRepo<VaultLot> vault;
    private volatile boolean closing;
    private LogRepositoryBackend logs;

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

    private void removalTick() {
        if (closing) return;
        var now = System.currentTimeMillis();
        int limit = 1000;
        while (limit-- > 0 && !lots.set.isEmpty()) {
            var v = lots.set.getFirst();
            if (v.removalDate() > now) break;
            logs.publishLog(new LotExpirationLog(now, v.owner(), v.lprice(), v.item(), v.count()));
            move2vault(new C2SMove2VaultRequest(v.uid(), v.owner(), ONE_DAY_MS));
        }
        limit = 1000;
        while (limit-- > 0 && !vault.set.isEmpty()) {
            var v = vault.set.getFirst();
            if (v.removalDate() > now) break;
            logs.publishLog(new VaultLotExpirationLog(now, v.owner(), v.lprice(), v.item(), v.count()));
            removeVaultLot(new C2SRemoveVaultLotRequest(v.uid()));
        }
        worker.schedule(this::removalTick, 100);
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
        log.info("SEND {}-{} of {}", from, from + len, uids.length);
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
        logs = pipeline.get(LogRepositoryBackend.class);
        worker = runtime.eventLoop();
        lots = new AucLotRepo<>(
                AucLot::read,
                new BlobRepository(server.database().dataSource(), server.name() + "_lots_repository"),
                server.ioWorker(),
                (o1, o2) -> {
                    var l = Long.compare(o1.removalDate(), o2.removalDate());
                    if (l == 0) return Integer.compare(o1.uid(), o2.uid());
                    return l;
                }
        );
        vault = new AucLotRepo<>(
                VaultLot::read,
                new BlobRepository(server.database().dataSource(), server.name() + "_vault_repository"),
                server.ioWorker(),
                (o1, o2) -> {
                    var l = Long.compare(o1.removalDate(), o2.removalDate());
                    if (l == 0) return Integer.compare(o1.uid(), o2.uid());
                    return l;
                }
        );
        try {
            lots.loadAll();
            vault.loadAll();
            log.info("loaded {} lots & {} vault lots", lots.lots.size(), vault.lots.size());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load lots!", e);
        }
        worker.schedule(this::removalTick, 100);
    }

    private ResponseFuture<A2AFlagResponse> onSubtractLot(C2SSubtractLotRequest r) {
        var lot = lots.get(r.uid());
        if (lot == null || lot.count() < r.count()) return new ResponseFuture<>(new A2AFlagResponse(false));
        int newCount = lot.count() - r.count();
        if (newCount <= 0) {
            lots.remove(r.uid());
            channel.broadcast(new S2COnVaultLotRemovePacket(r.uid())); 
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
        channel.broadcast(new S2CLotCountChange(r.uid(), newCount)); 
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }

    private ResponseFuture<A2AFlagResponse> removeVaultLot(C2SRemoveVaultLotRequest rm) {
        VaultLot removed = vault.remove(rm.uid());
        if (removed == null) return new ResponseFuture<>(new A2AFlagResponse(false));
        channel.broadcast(new S2COnVaultLotRemovePacket(rm.uid())); 
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
        channel.broadcast(new S2CVaultLotUpdate(vaultLot)); 
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }

    private ResponseFuture<A2AFlagResponse> move2vault(C2SMove2VaultRequest move) {
        return move2vault(move, false);
    }
    private ResponseFuture<A2AFlagResponse> move2vault(C2SMove2VaultRequest move, boolean silentRemove) {
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
        if (!silentRemove){
            channel.broadcast(new S2COnLotRemovePacket(lot.uid())); 
        }
        channel.broadcast(new S2CVaultLotUpdate(vaultLot)); 
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }


    private ResponseFuture<A2AFlagResponse> removeLot(C2SRemoveLotRequest remove) {
        var flag = lots.remove(remove.uid()) != null;
        if (flag) {
            channel.broadcast(new S2COnLotRemovePacket(remove.uid())); 
        }
        return new ResponseFuture<>(new A2AFlagResponse(flag));
    }

    private ResponseFuture<A2AFlagResponse> newNewItem(C2SAddNewLotRequest packet) {
        var now = System.currentTimeMillis();
        var v = lots.insert(new AucLot(
                -1,
                packet.itemId,
                packet.owner,
                now,
                now + packet.sellingDuration,
                packet.count,
                packet.price
        ));
        if (packet.sellingDuration <= 0) {
            logs.publishLog(new LotExpirationLog(now, v.owner(), v.lprice(), v.item(), v.count()));
            move2vault(new C2SMove2VaultRequest(v.uid(), v.owner(), ONE_DAY_MS), true);
        } else {
            channel.broadcast(new S2CLotUpdate(v)); 
        }
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }


    @Override
    public void close() {
        closing = true;
        lots.close();
        vault.close();
    }

    private static class AucLotRepo<T extends BaseLot> {
        private final Int2ObjectOpenHashMap<T> lots = new Int2ObjectOpenHashMap<>();
        private final BiFunction<ByteBuf, Integer, T> reader;
        private final BlobRepository lost_repo;
        private int lastUid;
        private final DataBatcher<BlobRepository.Record> insertBatch;
        private final DataBatcher<Integer> removeBatch;
        private final TreeSet<T> set;

        private AucLotRepo(BiFunction<ByteBuf, Integer, T> reader, BlobRepository lostRepo, EventLoopWorker ioWorker, Comparator<T> comparator) {
            this.set = new TreeSet<>(comparator);
            this.reader = reader;
            lost_repo = lostRepo;
            insertBatch = new DataBatcher<>(4096, lost_repo::putAll, ioWorker);
            removeBatch = new DataBatcher<>(4096, lost_repo::removeAll, ioWorker);
        }

        private void loadAll() throws SQLException {
            lots.clear();
            for (BlobRepository.Record record : lost_repo.loadAll()) {
                int uid = record.id();
                lastUid = Math.max(lastUid, uid);
                T lot = reader.apply(Unpooled.wrappedBuffer(record.data()), uid);
                lots.put(uid, lot);
                set.add(lot);
            }
        }

        public @Nullable T get(int uid) {
            return lots.get(uid);
        }

        public T insert(T lot0) {
            int id = ++lastUid;
            insert2Db(id, lot0);
            T lot = (T) lot0.withUid(id);
            lots.put(id, lot);
            set.add(lot);
            return lot;
        }

        private void insert2Db(int id, T lot) {
            insertBatch.offer(new BlobRepository.Record(id, lot.asBytes()));
        }

        public @Nullable T remove(int id) {
            var v = lots.remove(id);
            if (v != null) {
                set.remove(v);
                removeBatch.offer(id);
            }
            return v;
        }

        public void update(T lot) {
            lots.put(lot.uid(), lot);
            set.add(lot);
            insert2Db(lot.uid(), lot);
        }

        public void close() {
            insertBatch.close();
            removeBatch.close();
        }
    }
}