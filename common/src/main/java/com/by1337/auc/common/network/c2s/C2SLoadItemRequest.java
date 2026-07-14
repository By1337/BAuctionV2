package com.by1337.auc.common.network.c2s;

import com.by1337.auc.common.network.s2c.S2CItemResponsePacket;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SLoadItemRequest(int id) implements Packet, ExpectsResponse<S2CItemResponsePacket> {
    public C2SLoadItemRequest(ByteBuf buf, int protocolVersion) {
        this(buf.readInt());
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(id);
    }
}
