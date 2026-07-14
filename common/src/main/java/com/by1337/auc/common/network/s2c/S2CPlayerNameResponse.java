package com.by1337.auc.common.network.s2c;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

public record S2CPlayerNameResponse(@Nullable String name) implements Packet {
    public S2CPlayerNameResponse(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readOptional(buf, ByteBufCodecs::readUtf8));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeOptional(buf, name, ByteBufCodecs::writeUtf8);
    }
}
