package dev.by1337.auc.common.network.a2a;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public record A2ASetPlayerNamePacket(UUID uuid, String name) implements Packet {
    public A2ASetPlayerNamePacket(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readUUID(buf), ByteBufCodecs.readUtf8(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeUUID(buf, uuid);
        ByteBufCodecs.writeUtf8(buf, name);
    }
}
