package dev.by1337.auc.common.auc.log.impl;

import dev.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VaultLotExpirationLog implements AuctionLog {
    public static final String ID = "native:expiration_vault_lot";
    public final long timestamp;
    public final UUID owner;
    public final long lprice;
    public final int item;
    public final int count;

    public VaultLotExpirationLog(long timestamp, UUID owner, long lprice, int item, int count) {
        this.timestamp = timestamp;
        this.owner = owner;
        this.lprice = lprice;
        this.item = item;
        this.count = count;
    }
    public VaultLotExpirationLog(ByteBuf buf) {
        byte version = buf.readByte();
        timestamp = buf.readLong();
        owner = ByteBufCodecs.readUUID(buf);
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
        return owner;
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
        ByteBufCodecs.writeUUID(buf, owner);
        buf.writeLong(lprice);
        buf.writeInt(item);
        buf.writeInt(count);
    }
}
