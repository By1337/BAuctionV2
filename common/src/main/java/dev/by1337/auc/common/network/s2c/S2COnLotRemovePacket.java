package dev.by1337.auc.common.network.s2c;

import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record S2COnLotRemovePacket(int uid) implements Packet {

    public S2COnLotRemovePacket(ByteBuf buf, int protocolVersion) {
        this(buf.readInt());
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(uid);
    }
}
