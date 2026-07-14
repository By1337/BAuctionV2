package com.by1337.auc.handler.backend;

import com.by1337.auc.common.auc.AucLot;
import com.by1337.auc.common.auc.VaultLot;
import com.by1337.auc.common.handler.GetPostChannelHandler;
import com.by1337.auc.common.network.a2a.A2AFlagResponse;
import com.by1337.auc.common.network.c2s.*;
import com.by1337.auc.common.network.s2c.S2CLotUpdate;
import com.by1337.auc.common.network.s2c.S2CRemoveLotPacket;
import com.by1337.auc.common.network.s2c.S2CRemoveVaultLotPacket;
import com.by1337.auc.common.network.s2c.S2CVaultLotUpdate;
import dev.by1337.sync.client.channel.ClientChannelRuntime;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.channel.pipeline.Connection;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class ClientLotsRepositoryBackend extends GetPostChannelHandler {
    private Connection local;
    private Connection remote;
    private int lastLotId;
    private int lastVaultId;
    private final Int2ObjectOpenHashMap<AucLot> lots = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<VaultLot> vault = new Int2ObjectOpenHashMap<>();

    public ClientLotsRepositoryBackend() {
        registerGet(C2SAddNewLotRequest.class, this::newNewItem);
        registerGet(C2SRemoveLotRequest.class, this::removeLot);
        registerGet(C2SMove2VaultRequest.class, this::move2vault);
        registerGet(C2SAddNewVaultRequest.class, this::addNewVault);
        registerGet(C2SRemoveVaultLotRequest.class, this::removeVaultLot);
    }

    private ResponseFuture<A2AFlagResponse> removeVaultLot(C2SRemoveVaultLotRequest rm) {
        VaultLot removed = vault.remove(rm.uid());
        if (removed == null) return new ResponseFuture<>(new A2AFlagResponse(false));
        remote.write(new S2CRemoveVaultLotPacket(rm.uid())); //broadcast
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }


    private ResponseFuture<A2AFlagResponse> addNewVault(C2SAddNewVaultRequest packet) {
        var now = System.currentTimeMillis();
        VaultLot vaultLot = new VaultLot(
                lastVaultId++,
                packet.itemId,
                packet.owner,
                now + packet.storeDuration,
                packet.count,
                packet.price
        );
        vault.put(vaultLot.uid(), vaultLot);
        remote.write(new S2CVaultLotUpdate(vaultLot)); //broadcast
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }

    private ResponseFuture<A2AFlagResponse> move2vault(C2SMove2VaultRequest move) {
        var lot = lots.remove(move.uid());
        if (lot == null) return new ResponseFuture<>(new A2AFlagResponse(false));
        VaultLot vaultLot = new VaultLot(
                lastVaultId++,
                lot.item(),
                move.newOwner(),
                System.currentTimeMillis() + move.storeDuration(),
                lot.count(),
                lot.lprice()
        );
        vault.put(vaultLot.uid(), vaultLot);
        remote.write(new S2CRemoveLotPacket(lot.uid())); //broadcast
        remote.write(new S2CVaultLotUpdate(vaultLot)); //broadcast
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }


    private ResponseFuture<A2AFlagResponse> removeLot(C2SRemoveLotRequest remove) {
        var flag = lots.remove(remove.uid()) != null;
        if (flag) {
            remote.write(new S2CRemoveLotPacket(remove.uid())); //broadcast
        }
        return new ResponseFuture<>(new A2AFlagResponse(flag));
    }

    private ResponseFuture<A2AFlagResponse> newNewItem(C2SAddNewLotRequest packet) {
        var now = System.currentTimeMillis();
        AucLot lot = new AucLot(
                lastLotId++,
                packet.itemId,
                packet.owner,
                now,
                now + packet.sellingDuration,
                packet.count,
                packet.price
        );
        lots.put(lot.uid(), lot);
        remote.write(new S2CLotUpdate(lot)); //broadcast
        return new ResponseFuture<>(new A2AFlagResponse(true));
    }

    @Override
    public void init(ChannelRuntime runtime) {
        if (!(runtime instanceof ClientChannelRuntime ccr)) throw new IllegalArgumentException("Invalid runtime type");
        local = ccr.pipeline().local();
        remote = ccr.remote();
    }


    @Override
    public void close() {
    }
}