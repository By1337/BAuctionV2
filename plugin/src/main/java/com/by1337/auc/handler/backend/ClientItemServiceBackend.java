package com.by1337.auc.handler.backend;

import com.by1337.auc.common.handler.GetPostChannelHandler;
import com.by1337.auc.common.network.c2s.C2SLoadItemRequest;
import com.by1337.auc.common.network.c2s.C2SPushItemRequest;
import com.by1337.auc.common.network.s2c.S2CItemIdResponsePacket;
import com.by1337.auc.common.network.s2c.S2CItemResponsePacket;
import dev.by1337.sync.client.channel.ClientChannelRuntime;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.channel.pipeline.Connection;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class ClientItemServiceBackend extends GetPostChannelHandler {
    private Connection local;
    private int lastId;
    private final Int2ObjectOpenHashMap<byte[]> items = new Int2ObjectOpenHashMap<>();

    public ClientItemServiceBackend() {
        registerGet(C2SPushItemRequest.class, this::pushItem);
        registerGet(C2SLoadItemRequest.class, this::loadItem);
    }

    @Override
    public void init(ChannelRuntime runtime) {
        if (!(runtime instanceof ClientChannelRuntime ccr)) throw new IllegalArgumentException("Invalid runtime type");
        local = ccr.pipeline().local();
    }

    private ResponseFuture<S2CItemResponsePacket> loadItem(C2SLoadItemRequest load) {
        return new ResponseFuture<>(new S2CItemResponsePacket(items.get(load.id())));
    }

    private ResponseFuture<S2CItemIdResponsePacket> pushItem(C2SPushItemRequest push) {
        int id = lastId++;
        items.put(id, push.itemStack());
        return new ResponseFuture<>(new S2CItemIdResponsePacket(id));
    }

    @Override
    public void close() {
    }
}