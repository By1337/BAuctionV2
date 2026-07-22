package dev.by1337.auc.common.network.s2c;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record S2CPlayerNameUUIDResponse(@Nullable String name, @Nullable UUID uuid) implements Packet {
    public S2CPlayerNameUUIDResponse(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readOptional(buf, ByteBufCodecs::readUtf8), ByteBufCodecs.readOptional(buf, ByteBufCodecs::readUUID));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeOptional(buf, name, ByteBufCodecs::writeUtf8);
        ByteBufCodecs.writeOptional(buf, uuid, ByteBufCodecs::writeUUID);
    }
}
