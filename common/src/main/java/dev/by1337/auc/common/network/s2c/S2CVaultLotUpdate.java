package dev.by1337.auc.common.network.s2c;

import dev.by1337.auc.common.auc.VaultLot;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record S2CVaultLotUpdate(VaultLot lot) implements Packet {

    public S2CVaultLotUpdate(ByteBuf buf, int protocolVersion) {
        this(VaultLot.read(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        lot.write(buf);
    }
}
