package com.by1337.auc.common.network.a2a;

import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record A2AI32Response(int uid) implements Packet {
    public A2AI32Response(ByteBuf buf, int protocolVersion) {
        this(buf.readInt());
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(uid);
    }
}
