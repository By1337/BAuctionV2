package com.by1337.auc.common.auc.log;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record LogQuery(
        @Nullable Long afterId,
        @Nullable Long beforeId,
        @Nullable Long afterTimestamp,
        @Nullable Long beforeTimestamp,
        @Nullable UUID actor,
        @Nullable UUID subject,
        @Nullable String type,
        int limit
) {
    public static LogQuery read(ByteBuf buf) {
        return new LogQuery(
                ByteBufCodecs.readOptional(buf, ByteBuf::readLong),
                ByteBufCodecs.readOptional(buf, ByteBuf::readLong),
                ByteBufCodecs.readOptional(buf, ByteBuf::readLong),
                ByteBufCodecs.readOptional(buf, ByteBuf::readLong),
                ByteBufCodecs.readOptional(buf, ByteBufCodecs::readUUID),
                ByteBufCodecs.readOptional(buf, ByteBufCodecs::readUUID),
                ByteBufCodecs.readOptional(buf, ByteBufCodecs::readUtf8),
                buf.readInt()
        );
    }

    public void write(ByteBuf buf) {
        ByteBufCodecs.writeOptional(buf, afterId, ByteBuf::writeLong);
        ByteBufCodecs.writeOptional(buf, beforeId, ByteBuf::writeLong);
        ByteBufCodecs.writeOptional(buf, afterTimestamp, ByteBuf::writeLong);
        ByteBufCodecs.writeOptional(buf, beforeTimestamp, ByteBuf::writeLong);
        ByteBufCodecs.writeOptional(buf, actor, ByteBufCodecs::writeUUID);
        ByteBufCodecs.writeOptional(buf, subject, ByteBufCodecs::writeUUID);
        ByteBufCodecs.writeOptional(buf, type, ByteBufCodecs::writeUtf8);
        buf.writeInt(limit);
    }
}