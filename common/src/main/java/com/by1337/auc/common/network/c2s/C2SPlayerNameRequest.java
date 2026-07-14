package com.by1337.auc.common.network.c2s;

import com.by1337.auc.common.network.s2c.S2CPlayerNameResponse;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public record C2SPlayerNameRequest(UUID uuid) implements Packet, ExpectsResponse<S2CPlayerNameResponse> {
    public C2SPlayerNameRequest(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readUUID(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeUUID(buf, uuid);
    }
}
