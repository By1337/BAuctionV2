package dev.by1337.auc.auc;

import dev.by1337.auc.auc.util.BatchedLotSubtractor;
import dev.by1337.auc.auc.util.LotPricer;
import dev.by1337.auc.common.auc.AucLot;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.name.PlayerName;
import dev.by1337.auc.util.DurationFormatter;
import dev.by1337.item.ItemModel;
import dev.by1337.plc.PlaceholderResolver;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ClientAucLot implements LotData, BuyableLot {
    private static final PlaceholderResolver<ClientAucLot> PLACEHOLDERS = LotData.<ClientAucLot>createPlaceholders()
            .withContext("seller_uuid", v -> v.lot.owner().toString())
            .withContext("seller_name", ClientAucLot::ownerName)
            .withContext("uid", ClientAucLot::uid)
            .withContext("expires", v -> DurationFormatter.getFormat(v.removalDate()));
    public final AucLot lot;
    public final ClientItemStack itemStack;
    public final int shortId;
    public final PlayerName playerName;
    public final LotPricer pricer;

    public ClientAucLot(AucLot lot, ClientItemStack itemStack, int shortId, PlayerName playerName) {
        this.lot = lot;
        this.itemStack = itemStack;
        this.shortId = shortId;
        pricer = new LotPricer(lot.lprice_for_one, lot.count());
        this.playerName = playerName;
    }

    public int shortIdOr(int def){
        return shortId;
    }

    @Override
    public int addToSubtractor(BatchedLotSubtractor subtractor, int requested, Auction auction) {
        var updated = auction.getLot(uid());
        if (updated == null) return 0;
        int used = subtractor.getUsedLots(uid());
        int maxCount = updated.count() - used;
        if (maxCount <= 0) return 0;
        int x = Math.min(requested, maxCount);
        subtractor.append(uid(), x, itemStack.id(), pricer.centsPriceForOne, owner());
        return x;
    }

    @Override
    public ItemStack bukkitItem(int count) {
        return itemStack.asQuantity(count);
    }

    @Override
    public ItemModel itemModel() {
        return itemStack.itemModel();
    }

    @Override
    public LotData update(Auction auction) {
        return auction.getLot(uid());
    }

    @Override
    public boolean isOwner(UUID uuid) {
        return owner().equals(uuid);
    }

    @Override
    public int count() {
        return lot.count();
    }

    public int uid() {
        return lot.uid();
    }

    public UUID owner() {
        return lot.owner();
    }

    public long createdDate() {
        return lot.createdDate();
    }

    public long removalDate() {
        return lot.removalDate();
    }

    @Override
    public LotPricer pricer() {
        return pricer;
    }

    public long centsPrice() {
        return lot.cents();
    }

    public long centsPriceForOne() {
        return pricer.centsPriceForOne;
    }

    public double price() {
        return pricer.price;
    }

    public double priceForOne() {
        return pricer.priceForOne;
    }

    @Override
    public ClientItemStack itemStack() {
        return itemStack;
    }

    public AucLot lot() {
        return lot;
    }

    @Override
    public <T> PlaceholderResolver<T> placeholders() {
        return PLACEHOLDERS.bindCtx(this);
    }


    public String ownerName() {
        return playerName.name();
    }

    @Override
    public String toString() {
        return "ClientAucLot{" +
                "lot=" + lot +
                ", itemStack=" + itemStack +
                ", shortId=" + shortId +
                '}';
    }
}
