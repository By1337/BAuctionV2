package dev.by1337.auc.common.network.c2s;

import dev.by1337.auc.common.network.s2c.S2COptionalLogRecord;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SGetLogRecordRequest(long uid) implements Packet, ExpectsResponse<S2COptionalLogRecord> {
    public C2SGetLogRecordRequest(ByteBuf buf, int protocolVersion) {
        this(buf.readLong());
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeLong(uid);
    }
}
