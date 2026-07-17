package com.by1337.auc.common.network.c2s;

import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SGetAllLotsRequest() implements Packet {
    public C2SGetAllLotsRequest(ByteBuf buf, int protocolVersion) {
        this();
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
    }
}
