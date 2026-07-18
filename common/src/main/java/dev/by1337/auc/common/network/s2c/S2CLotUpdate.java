package dev.by1337.auc.common.network.s2c;

import dev.by1337.auc.common.auc.AucLot;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record S2CLotUpdate(AucLot lot) implements Packet {

    public S2CLotUpdate(ByteBuf buf, int protocolVersion) {
        this(AucLot.read(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        lot.write(buf);
    }
}
