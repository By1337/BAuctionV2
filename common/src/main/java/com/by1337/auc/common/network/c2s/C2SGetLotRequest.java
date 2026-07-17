package com.by1337.auc.common.network.c2s;

import com.by1337.auc.common.network.s2c.S2COptionalLot;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SGetLotRequest(int uid) implements Packet, ExpectsResponse<S2COptionalLot> {
    public C2SGetLotRequest(ByteBuf buf, int protocolVersion) {
        this(buf.readInt());
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(uid);
    }
}
