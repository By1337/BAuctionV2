package dev.by1337.auc.common.network.c2s;

import dev.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

public record C2SMassSubtractLotRequest(int[] payload) implements Packet, ExpectsResponse<A2AFlagResponse> {

    public C2SMassSubtractLotRequest(ByteBuf buf, int protocolVersion) {
        this(readIntArray(buf));
    }

    public static C2SMassSubtractLotRequest create(int uid, int count, int uid2, int count2) {
        return create(new int[]{uid, uid2, count, count2});
    }

    public static C2SMassSubtractLotRequest create(int... payload) {
        if (payload.length % 2 != 0) throw new IllegalArgumentException();
        return new C2SMassSubtractLotRequest(payload);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeIntArray(buf, payload);
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
