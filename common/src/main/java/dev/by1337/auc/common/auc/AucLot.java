package dev.by1337.auc.common.auc;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import java.util.Objects;
import java.util.UUID;

public class AucLot implements BaseLot{
    public static final byte VERSION = 1;
    private final int uid;
    private final int item;
    private final UUID owner;
    private final long createdDate;
    private final long removalDate;
    private final int count;
    private final long lprice;
    public final transient long lprice_for_one;

    public AucLot(int uid, int item, UUID owner, long createdDate, long removalDate, int count, long lprice) {
        this.uid = uid;
        this.item = item;
        this.owner = owner;
        this.createdDate = createdDate;
        this.removalDate = removalDate;
        this.count = count;
        this.lprice = lprice;
        lprice_for_one = lprice / count;
    }

    public AucLot withUid(int uid) {
        return new AucLot(uid, item, owner, createdDate, removalDate, count, lprice);
    }

    public void write(ByteBuf buf) {
        buf.writeByte(VERSION);
        buf.writeInt(uid);
        buf.writeInt(item);
        ByteBufCodecs.writeUUID(buf, owner);
        buf.writeLong(createdDate);
        buf.writeLong(removalDate);
        buf.writeInt(count);
        buf.writeLong(lprice);
    }

    public static AucLot read(byte[] buf) {
        return read(Unpooled.wrappedBuffer(buf));
    }

    public static AucLot read(ByteBuf buf) {
        return read(buf, -1);
    }

    public static AucLot read(ByteBuf buf, int overrideUID) {
        byte version = buf.readByte();
        if (version != 1) throw new DecoderException("Bad version " + version);

        int uid = buf.readInt();
        int item = buf.readInt();
        UUID owner = ByteBufCodecs.readUUID(buf);
        long createdDate = buf.readLong();
        long removalDate = buf.readLong();
        int quantity = buf.readInt();
        long price = buf.readLong();

        return new AucLot(overrideUID != -1 ? overrideUID : uid, item, owner, createdDate, removalDate, quantity, price);
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

    public long createdDate() {
        return createdDate;
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

    @Override
    public String toString() {
        return "AucLot{" +
                "uid=" + uid +
                ", item=" + item +
                ", owner=" + owner +
                ", createdDate=" + createdDate +
                ", removalDate=" + removalDate +
                ", count=" + count +
                ", price=" + lprice +
                ", lprice_for_one=" + lprice_for_one +
                '}';
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AucLot aucLot = (AucLot) o;
        return uid == aucLot.uid && item == aucLot.item && createdDate == aucLot.createdDate && removalDate == aucLot.removalDate && count == aucLot.count && lprice == aucLot.lprice && lprice_for_one == aucLot.lprice_for_one && Objects.equals(owner, aucLot.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, item, owner, createdDate, removalDate, count, lprice, lprice_for_one);
    }
}
