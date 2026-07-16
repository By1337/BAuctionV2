package com.by1337.auc.common.network.s2c;

import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record S2CLotCountChange(int uid, int newCount) implements Packet {

    public S2CLotCountChange(ByteBuf buf, int protocolVersion) {
        this(buf.readInt(), buf.readInt());
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(uid);
        buf.writeInt(newCount);
    }
}
