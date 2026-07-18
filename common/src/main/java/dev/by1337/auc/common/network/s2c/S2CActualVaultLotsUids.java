package dev.by1337.auc.common.network.s2c;

import dev.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

public record S2CActualVaultLotsUids(int[] uids) implements Packet, ExpectsResponse<A2AFlagResponse> {

    public S2CActualVaultLotsUids(ByteBuf buf, int protocolVersion) {
        this(readIntArray(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeIntArray(buf, uids);
    }

    private static void writeIntArray(ByteBuf buf, int[] bytes) {
        buf.writeInt(bytes.length);
        for (int i : bytes) {
            buf.writeInt(i);
        }
    }

    private static int[] readIntArray(ByteBuf buf) {
        int length = buf.readInt();
        if (length < 0 || length >= 32 << 20) throw new DecoderException("Invalid array length: " + length);
        int[] arr = new int[length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = buf.readInt();
        }
        return arr;
    }
}
