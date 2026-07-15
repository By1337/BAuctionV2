package com.by1337.auc.common.network.s2c;

import com.by1337.auc.common.auc.log.LogRecord;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

public record S2COptionalLogRecord(@Nullable LogRecord log) implements Packet {
    public S2COptionalLogRecord(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readOptional(buf, v -> LogRecord.read(v, protocolVersion)));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeOptional(buf, log, (v, v1) -> v1.write(v, protocolVersion));
    }
}
