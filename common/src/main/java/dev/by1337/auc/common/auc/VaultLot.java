package dev.by1337.auc.common.auc;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import java.util.UUID;

public class VaultLot implements BaseLot{
    public static final byte VERSION = 1;

    private final int uid;
    private final int item;
    private final UUID owner;
    private final long removalDate;
    private final int count;
    private final long lprice;

    public VaultLot(int uid, int item, UUID owner, long removalDate, int count, long lprice) {
        this.uid = uid;
        this.item = item;
        this.owner = owner;
        this.removalDate = removalDate;
        this.count = count;
        this.lprice = lprice;
    }
    public VaultLot withUid(int uid){
        return new VaultLot(uid, item, owner, removalDate, count, lprice);
    }

    public void write(ByteBuf buf) {
        buf.writeByte(VERSION);
        buf.writeInt(uid);
        buf.writeInt(item);
        ByteBufCodecs.writeUUID(buf, owner);
        buf.writeLong(removalDate);
        buf.writeInt(count);
        buf.writeLong(lprice);
    }

    public static VaultLot read(ByteBuf buf) {
        return read(buf, -1);
    }

    public static VaultLot read(ByteBuf buf, int overrideUID) {
        byte version = buf.readByte();
        if (version != VERSION) throw new DecoderException("Bad version " + version);

        int uid = buf.readInt();
        int item = buf.readInt();
        UUID owner = ByteBufCodecs.readUUID(buf);
        long removalDate = buf.readLong();
        int count = buf.readInt();
        long price = buf.readLong();

        return new VaultLot(overrideUID != -1 ? overrideUID : uid, item, owner, removalDate, count, price);
    }
    public int uid() {
        return uid;
    }

    public int item() {
        return item;
    }

    public UUID owner() {
        return owner;
    }

    public long removalDate() {
        return removalDate;
    }

    public int count() {
        return count;
    }

    public long lprice() {
        return lprice;
    }

    public byte[] asBytes() {
        ByteBuf buf = Unpooled.buffer();
        try {
            write(buf);
            byte[] arr = new byte[buf.readableBytes()];
            buf.readBytes(arr);
            return arr;
        } finally {
            buf.release();
        }
    }
}
