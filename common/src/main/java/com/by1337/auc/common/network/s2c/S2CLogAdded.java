package com.by1337.auc.common.network.s2c;

import com.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record S2CLogAdded(AuctionLog log) implements Packet {
    public S2CLogAdded(ByteBuf buf, int protocolVersion) {
        this(AuctionLog.read(buf, protocolVersion));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        log.write(buf, protocolVersion);
    }
}
