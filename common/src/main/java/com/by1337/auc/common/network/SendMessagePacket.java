package com.by1337.auc.common.network;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public record SendMessagePacket(UUID player, String key) implements Packet {
    public SendMessagePacket(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readUUID(buf), ByteBufCodecs.readUtf8(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeUUID(buf, player);
        ByteBufCodecs.writeUtf8(buf, key);
    }
}
