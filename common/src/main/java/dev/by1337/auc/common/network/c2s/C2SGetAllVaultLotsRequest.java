package dev.by1337.auc.common.network.c2s;

import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SGetAllVaultLotsRequest() implements Packet {
    public C2SGetAllVaultLotsRequest(ByteBuf buf, int protocolVersion) {
        this();
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
    }
}
