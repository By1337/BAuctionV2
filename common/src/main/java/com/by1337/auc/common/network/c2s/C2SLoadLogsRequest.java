package com.by1337.auc.common.network.c2s;

import com.by1337.auc.common.network.s2c.S2CLogsLoadResponse;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record C2SLoadLogsRequest(long after, @Nullable UUID actor, @Nullable UUID subject,
                                 @Nullable String type, int limit) implements Packet, ExpectsResponse<S2CLogsLoadResponse> {

    public static C2SLoadLogsRequest read(ByteBuf buf, int protocolVersion) {
        long after = buf.readLong();
        var actor = ByteBufCodecs.readOptional(buf, ByteBufCodecs::readUUID);
        var subject = ByteBufCodecs.readOptional(buf, ByteBufCodecs::readUUID);
        var type = ByteBufCodecs.readOptional(buf, ByteBufCodecs::readUtf8);
        int limit = buf.readInt();
        return new C2SLoadLogsRequest(after, actor, subject, type, limit);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeLong(after);
        ByteBufCodecs.writeOptional(buf, actor, ByteBufCodecs::writeUUID);
        ByteBufCodecs.writeOptional(buf, subject, ByteBufCodecs::writeUUID);
        ByteBufCodecs.writeOptional(buf, type, ByteBufCodecs::writeUtf8);
        buf.writeInt(limit);
    }
}
