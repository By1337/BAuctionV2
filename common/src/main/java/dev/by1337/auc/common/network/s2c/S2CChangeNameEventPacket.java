package dev.by1337.auc.common.network.s2c;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public record S2CChangeNameEventPacket(UUID uuid, String name) implements Packet {
    public S2CChangeNameEventPacket(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readUUID(buf), ByteBufCodecs.readUtf8(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeUUID(buf, uuid);
        ByteBufCodecs.writeUtf8(buf, name);
    }
}
