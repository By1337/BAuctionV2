package dev.by1337.auc.common.auc.log.impl;

import dev.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class BuyAuctionLog implements AuctionLog, WithItemStackLog, WithLPriceLog {
    public static final String ID = "native:buy";
    public final long timestamp;
    public final UUID buyer;
    public final UUID lotOwner;
    public final long lprice;
    public final int item;
    public final int count;


    public BuyAuctionLog(long timestamp, UUID buyer, UUID lotOwner, long lprice, int item, int count) {
        this.timestamp = timestamp;
        this.buyer = buyer;
        this.lotOwner = lotOwner;
        this.lprice = lprice;
        this.item = item;
        this.count = count;
    }

    public BuyAuctionLog(ByteBuf buf) {
        byte version = buf.readByte();
        timestamp = buf.readLong();
        buyer = ByteBufCodecs.readUUID(buf);
        lotOwner = ByteBufCodecs.readUUID(buf);
        lprice = buf.readLong();
        item = buf.readInt();
        count = buf.readInt();
    }

    @Override
    public void writePayload(ByteBuf buf) {
        buf.writeByte(1);
        buf.writeLong(timestamp);
        ByteBufCodecs.writeUUID(buf, buyer);
        ByteBufCodecs.writeUUID(buf, lotOwner);
        buf.writeLong(lprice);
        buf.writeInt(item);
        buf.writeInt(count);
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public UUID actor() {
        return buyer;
    }

    @Override
    public @Nullable UUID subject() {
        return lotOwner;
    }

    @Override
    public String type() {
        return ID;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BuyAuctionLog that = (BuyAuctionLog) o;
        return timestamp == that.timestamp && lprice == that.lprice && item == that.item && count == that.count && Objects.equals(buyer, that.buyer) && Objects.equals(lotOwner, that.lotOwner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, buyer, lotOwner, lprice, item, count);
    }

    @Override
    public int item() {
        return item;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public long lprice() {
        return lprice;
    }
}
