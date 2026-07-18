package dev.by1337.auc.common.network.s2c;

import dev.by1337.auc.common.auc.VaultLot;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

public record S2COptionalVaultLot(@Nullable VaultLot lot) implements Packet {
    public S2COptionalVaultLot(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readOptional(buf, VaultLot::read));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeOptional(buf, lot, (v, v1) -> v1.write(v));
    }
}
