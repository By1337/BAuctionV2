package com.by1337.auc.common.network.c2s;

import com.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class C2SAddNewLotRequest implements Packet, ExpectsResponse<A2AFlagResponse> {

    public int itemId;
    public UUID owner;
    public long sellingDuration;
    public int count;
    public long price;

    public C2SAddNewLotRequest(int itemId, UUID owner, long sellingDuration, int count, long price) {
        this.itemId = itemId;
        this.owner = owner;
        this.sellingDuration = sellingDuration;
        this.count = count;
        this.price = price;
    }

    public C2SAddNewLotRequest(ByteBuf buf, int protocolVersion) {
        this.itemId = buf.readInt();
        this.owner = ByteBufCodecs.readUUID(buf);
        this.sellingDuration = buf.readLong();
        this.count = buf.readInt();
        this.price = buf.readLong();
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(itemId);
        ByteBufCodecs.writeUUID(buf, owner);
        buf.writeLong(sellingDuration);
        buf.writeInt(count);
        buf.writeLong(price);
    }
}
