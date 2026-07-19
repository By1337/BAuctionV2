package dev.by1337.auc.common.network.s2c;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public record S2CItemResponsePacket(byte @Nullable [] itemStack) implements Packet {
    public S2CItemResponsePacket(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readOptional(buf, ByteBufCodecs::readByteArray));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeOptional(buf, itemStack, ByteBufCodecs::writeByteArray);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        S2CItemResponsePacket that = (S2CItemResponsePacket) o;
        return Objects.deepEquals(itemStack, that.itemStack);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(itemStack);
    }
}
