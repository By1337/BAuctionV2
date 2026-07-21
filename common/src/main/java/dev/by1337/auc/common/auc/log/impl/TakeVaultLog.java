package dev.by1337.auc.common.auc.log.impl;

import dev.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TakeVaultLog implements AuctionLog {
    public static final String ID = "native:take_vault_lot";
    public final long timestamp;
    public final UUID who;
    public final long lprice;
    public final int item;
    public final int count;

    public TakeVaultLog(long timestamp, UUID who, long lprice, int item, int count) {
        this.timestamp = timestamp;
        this.who = who;
        this.lprice = lprice;
        this.item = item;
        this.count = count;
    }
    public TakeVaultLog(ByteBuf buf) {
        byte version = buf.readByte();
        timestamp = buf.readLong();
        who = ByteBufCodecs.readUUID(buf);
        lprice = buf.readLong();
        item = buf.readInt();
        count = buf.readInt();
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public UUID actor() {
        return who;
    }

    @Override
    public @Nullable UUID subject() {
        return null;
    }

    @Override
    public String type() {
        return ID;
    }

    @Override
    public void writePayload(ByteBuf buf) {
        buf.writeByte(1);
        buf.writeLong(timestamp);
        ByteBufCodecs.writeUUID(buf, who);
        buf.writeLong(lprice);
        buf.writeInt(item);
        buf.writeInt(count);
    }
}
