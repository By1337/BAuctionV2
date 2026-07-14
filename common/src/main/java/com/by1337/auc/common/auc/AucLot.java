package com.by1337.auc.common.auc;

import dev.by1337.sync.common.packet.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import java.util.UUID;

public class AucLot {
    public static final byte VERSION = 1;
    private final int uid;
    private final int item;
    private final UUID owner;
    private final long createdDate;
    private final long removalDate;
    private int count;
    private long lprice;
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

    public byte[] toBytes(){
        ByteBuf buf = Unpooled.buffer(255, 255);
        write(buf);
        byte[] arr = new byte[buf.readableBytes()];
        buf.readBytes(arr);
        return arr;
    }

    public void write(ByteBuf buf){
        buf.writeByte(VERSION);
        buf.writeInt(uid);
        buf.writeInt(item);
        ByteBufCodecs.writeUUID(buf, owner);
        buf.writeLong(createdDate);
        buf.writeLong(removalDate);
        buf.writeInt(count);
        buf.writeLong(lprice);
    }

    public static AucLot read(byte[] buf){
        return read(Unpooled.wrappedBuffer(buf));
    }

    public static AucLot read(ByteBuf buf){
        byte version = buf.readByte();
        if (version != 1) throw new DecoderException("Bad version " + version);

        int uid = buf.readInt();
        int item = buf.readInt();
        UUID owner = ByteBufCodecs.readUUID(buf);
        long createdDate = buf.readLong();
        long removalDate = buf.readLong();
        int quantity = buf.readInt();
        long price = buf.readLong();

        return new AucLot(uid, item, owner, createdDate, removalDate, quantity, price);
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

    public void setCount(int count) {
        this.count = count;
    }

    public long lprice() {
        return lprice;
    }

    public void setLprice(long lprice) {
        this.lprice = lprice;
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
}
