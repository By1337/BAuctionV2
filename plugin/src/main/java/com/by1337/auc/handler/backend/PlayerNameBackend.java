package com.by1337.auc.handler.backend;

import com.by1337.auc.common.handler.GetPostChannelHandler;
import com.by1337.auc.common.network.a2a.A2ASetPlayerNamePacket;
import com.by1337.auc.common.network.c2s.C2SLoadItemRequest;
import com.by1337.auc.common.network.c2s.C2SPlayerNameRequest;
import com.by1337.auc.common.network.c2s.C2SPushItemRequest;
import com.by1337.auc.common.network.s2c.S2CItemIdResponsePacket;
import com.by1337.auc.common.network.s2c.S2CItemResponsePacket;
import com.by1337.auc.common.network.s2c.S2CPlayerNameResponse;
import dev.by1337.sync.client.channel.ClientChannelRuntime;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.channel.pipeline.Connection;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.UUID;

public class PlayerNameBackend extends GetPostChannelHandler {
    private Connection remote;
    private final Object2ObjectOpenHashMap<UUID, String> uuid2name = new Object2ObjectOpenHashMap<>();

    public PlayerNameBackend() {
        registerGet(C2SPlayerNameRequest.class, this::getName);
        registerPost(A2ASetPlayerNamePacket.class,this::setName);
    }

    private void setName(A2ASetPlayerNamePacket packet) {
        uuid2name.put(packet.uuid(), packet.name());
        remote.write(packet); //broadcast
    }

    private ResponseFuture<S2CPlayerNameResponse> getName(C2SPlayerNameRequest packet) {
        return new ResponseFuture<>(new S2CPlayerNameResponse(uuid2name.putIfAbsent(packet.uuid(), "NoName")));
    }

    @Override
    public void init(ChannelRuntime runtime) {
        if (!(runtime instanceof ClientChannelRuntime ccr)) throw new IllegalArgumentException("Invalid runtime type");
        remote = ccr.remote();
    }


    @Override
    public void close() {
    }
}