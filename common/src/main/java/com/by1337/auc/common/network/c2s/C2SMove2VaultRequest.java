package com.by1337.auc.common.network.c2s;

import com.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public record C2SMove2VaultRequest(int uid, UUID newOwner, long storeDuration) implements Packet, ExpectsResponse<A2AFlagResponse> {
    public C2SMove2VaultRequest(ByteBuf buf, int protocolVersion) {
        this(buf.readInt(), ByteBufCodecs.readUUID(buf), buf.readLong());
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(uid);
        ByteBufCodecs.writeUUID(buf, newOwner);
        buf.writeLong(storeDuration);
    }
}
