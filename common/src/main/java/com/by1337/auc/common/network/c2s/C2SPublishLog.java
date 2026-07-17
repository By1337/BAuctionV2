package com.by1337.auc.common.network.c2s;

import com.by1337.auc.common.auc.log.AuctionLog;
import com.by1337.auc.common.network.a2a.A2ALongResponse;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SPublishLog(AuctionLog log) implements Packet, ExpectsResponse<A2ALongResponse> {
    public C2SPublishLog(ByteBuf buf, int protocolVersion) {
        this(AuctionLog.read(buf, protocolVersion));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        log.write(buf, protocolVersion);
    }
}
