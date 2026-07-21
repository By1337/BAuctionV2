package dev.by1337.auc.user;

import dev.by1337.auc.user.pdc.PdcMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AucUser {
    private static final int VERSION = 1;
    public final UUID uuid;

    private final PdcMap pdc;

    public AucUser(UUID uuid, PdcMap pdc) {
        this.uuid = uuid;
        this.pdc = pdc;
    }

    public static AucUser read(ByteBuf buf, UUID key) {
        int version = buf.readInt();
        PdcMap pdcMap = PdcMap.read(buf);
        return new AucUser(key, pdcMap);
    }

    public void write(ByteBuf buf) {
        buf.writeInt(VERSION);
        pdc.write(buf);
    }

    public PdcMap pdc() {
        return pdc;
    }

    public static AucUser read(byte @Nullable [] arr, UUID key) {
        if (arr == null) return new AucUser(key, new PdcMap());
        return read(Unpooled.wrappedBuffer(arr), key);
    }

    public byte[] write() {
        ByteBuf buf = Unpooled.buffer();
        try {
            write(buf);
            byte[] result = new byte[buf.readableBytes()];
            buf.readBytes(result);
            return result;
        } finally {
            buf.release();
        }
    }
}
