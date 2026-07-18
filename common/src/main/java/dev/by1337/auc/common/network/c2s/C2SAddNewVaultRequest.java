package dev.by1337.auc.common.network.c2s;

import dev.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class C2SAddNewVaultRequest implements Packet, ExpectsResponse<A2AFlagResponse> {

    public int itemId;
    public UUID owner;
    public long storeDuration;
    public int count;
    public long price;

    public C2SAddNewVaultRequest(int itemId, UUID owner, long storeDuration, int count, long price) {
        this.itemId = itemId;
        this.owner = owner;
        this.storeDuration = storeDuration;
        this.count = count;
        this.price = price;
    }

    public C2SAddNewVaultRequest(ByteBuf buf, int protocolVersion) {
        this.itemId = buf.readInt();
        this.owner = ByteBufCodecs.readUUID(buf);
        this.storeDuration = buf.readLong();
        this.count = buf.readInt();
        this.price = buf.readLong();
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(itemId);
        ByteBufCodecs.writeUUID(buf, owner);
        buf.writeLong(storeDuration);
        buf.writeInt(count);
        buf.writeLong(price);
    }
}
