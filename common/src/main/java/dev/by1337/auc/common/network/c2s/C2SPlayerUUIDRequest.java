package dev.by1337.auc.common.network.c2s;

import dev.by1337.auc.common.network.s2c.S2CPlayerNameUUIDResponse;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SPlayerUUIDRequest(String name) implements Packet, ExpectsResponse<S2CPlayerNameUUIDResponse> {
    public C2SPlayerUUIDRequest(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readUtf8(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeUtf8(buf, name);
    }
}
