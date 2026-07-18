package dev.by1337.auc.common.network.c2s;

import dev.by1337.auc.common.network.s2c.S2COptionalVaultLot;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SGetVaultLotRequest(int uid) implements Packet, ExpectsResponse<S2COptionalVaultLot> {
    public C2SGetVaultLotRequest(ByteBuf buf, int protocolVersion) {
        this(buf.readInt());
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(uid);
    }
}
