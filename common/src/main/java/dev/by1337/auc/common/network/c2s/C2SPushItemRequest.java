package dev.by1337.auc.common.network.c2s;

import dev.by1337.auc.common.network.s2c.S2CItemIdResponsePacket;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.Objects;

public record C2SPushItemRequest(byte[] itemStack, byte[] sha256) implements Packet, ExpectsResponse<S2CItemIdResponsePacket> {

    public C2SPushItemRequest(ByteBuf buf, int protocolVersion) {
        this(ByteBufCodecs.readByteArray(buf), ByteBufCodecs.readByteArray(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        ByteBufCodecs.writeByteArray(buf, itemStack);
        ByteBufCodecs.writeByteArray(buf, sha256);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        C2SPushItemRequest that = (C2SPushItemRequest) o;
        return Objects.deepEquals(sha256, that.sha256) && Objects.deepEquals(itemStack, that.itemStack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(itemStack), Arrays.hashCode(sha256));
    }
}
