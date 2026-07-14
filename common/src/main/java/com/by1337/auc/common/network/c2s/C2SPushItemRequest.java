package com.by1337.auc.common.network.c2s;

import com.by1337.auc.common.network.s2c.S2CItemIdResponsePacket;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SPushItemRequest(byte[] itemStack) implements Packet, ExpectsResponse<S2CItemIdResponsePacket> {

    public C2SPushItemRequest(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readByteArray(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeByteArray(buf, itemStack);
    }
}
