package dev.by1337.auc.handler.event;

import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.packet.ExpectsResponse;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class TryAddLotRequest implements ChannelMessage, ExpectsResponse<ActionResult> {
    public final ItemStack item;
    public final UUID playerUUID;
    public final String playerName;
    public final long sellingDuration;
    public final int count;
    public final long price;

    public TryAddLotRequest(ItemStack item, UUID playerUUID, String playerName, long sellingDuration, int count, long price) {
        this.item = item;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.sellingDuration = sellingDuration;
        this.count = count;
        this.price = price;
    }

    @Override
    public String toString() {
        return "TryAddLotEvent{" +
                "item=" + item +
                ", playerUUID=" + playerUUID +
                ", playerName='" + playerName + '\'' +
                ", sellingDuration=" + sellingDuration +
                ", count=" + count +
                ", price=" + price +
                '}';
    }

}
