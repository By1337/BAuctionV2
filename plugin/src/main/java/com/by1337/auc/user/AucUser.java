package com.by1337.auc.user;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.jetbrains.annotations.Nullable;

public class AucUser {
    private final Object2LongMap<String> longs = new Object2LongOpenHashMap<>();
    public long lastSoldLotSeenTimestamp;

    public static AucUser read(ByteBuf buf) {
        var user = new AucUser();
        if (true) return user; //todo remove it
        user.lastSoldLotSeenTimestamp = buf.readLong();

        int longs = buf.readInt();
        for (int i = 0; i < longs; i++) {
            user.longs.put(ByteBufCodecs.readUtf8(buf), buf.readLong());
        }
        return user;
    }

    public void write(ByteBuf buf) {
        buf.writeLong(lastSoldLotSeenTimestamp);
        buf.writeInt(longs.size());
        for (var entry : longs.object2LongEntrySet()) {
            ByteBufCodecs.writeUtf8(buf, entry.getKey());
            buf.writeLong(entry.getLongValue());
        }
    }

    public long getLong(String key) {
        return longs.getLong(key);
    }

    public long removeLong(String key) {
        return longs.removeLong(key);
    }

    public long putLong(String key, long l) {
        return longs.put(key, l);
    }

    public static AucUser read(byte @Nullable [] arr) {
        if (arr == null) return new AucUser();
        return read(Unpooled.wrappedBuffer(arr));
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

    public void acceptMail(String mail) {

    }
}
