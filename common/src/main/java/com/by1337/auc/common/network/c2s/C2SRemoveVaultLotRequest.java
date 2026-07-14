package com.by1337.auc.common.network.c2s;

import com.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SRemoveVaultLotRequest(int uid) implements Packet, ExpectsResponse<A2AFlagResponse> {
    public C2SRemoveVaultLotRequest(ByteBuf buf, int protocolVersion) {
        this(buf.readInt());
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(uid);
    }
}
