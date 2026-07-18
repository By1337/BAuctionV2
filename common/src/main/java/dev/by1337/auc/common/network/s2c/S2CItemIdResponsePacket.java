package dev.by1337.auc.common.network.s2c;

import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record S2CItemIdResponsePacket(int id) implements Packet {
    public S2CItemIdResponsePacket(ByteBuf buf, int protocolVersion) {
        this(buf.readInt());
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(id);
    }
}
