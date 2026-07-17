package com.by1337.auc.common.network.s2c;

import com.by1337.auc.common.auc.AucLot;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

public record S2COptionalLot(@Nullable AucLot lot) implements Packet {
    public S2COptionalLot(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readOptional(buf, AucLot::read));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeOptional(buf, lot, (v, v1) -> v1.write(v));
    }
}
