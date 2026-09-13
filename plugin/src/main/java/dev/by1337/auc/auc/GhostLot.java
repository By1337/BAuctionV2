package dev.by1337.auc.auc;

import dev.by1337.auc.auc.util.LotPricer;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.name.PlayerName;
import dev.by1337.auc.util.DurationFormatter;
import dev.by1337.plc.PlaceholderResolver;

import java.util.UUID;

public class GhostLot implements LotData  {
    private static final PlaceholderResolver<GhostLot> PLACEHOLDERS = LotData.<GhostLot>createPlaceholders()
            .withContext("seller_uuid", v -> v.owner.toString())
            .withContext("seller_name", GhostLot::ownerName)
            .withContext("uid", GhostLot::uid)
            .withContext("expires", v -> DurationFormatter.getFormat(v.removalDate()))
            ;
    private final ClientItemStack itemStack;
    private final UUID owner;
    private final PlayerName ownerName;
    private final int count;
    private final LotPricer pricer;

    public GhostLot(ClientItemStack itemStack, UUID owner, PlayerName ownerName, long lprice, int count) {
        this.itemStack = itemStack;
        this.owner = owner;
        this.ownerName = ownerName;
        pricer = new LotPricer(lprice / count, count);
        this.count = count;
    }

    @Override
    public ClientItemStack itemStack() {
        return itemStack;
    }

    @Override
    public LotData update(Auction auction) {
        return this;
    }

    public ClientItemStack clientItemStack() {
        return itemStack;
    }

   // @Override
    public int uid() {
        return -1;
    }

    public UUID owner() {
        return owner;
    }

    @Override
    public int count() {
        return count;
    }

    public long removalDate() {
        return System.currentTimeMillis();
    }

    @Override
    public LotPricer pricer() {
        return pricer;
    }

    public long lprice() {
        return pricer.centsFor(count);
    }

    public long lprice_for_one() {
        return pricer.centsPriceForOne;
    }

    public double dprice() {
        return pricer.price;
    }

    public double dprice_for_one() {
        return pricer.priceForOne;
    }

    public String ownerName() {
        return ownerName.name();
    }

    @Override
    public <T> PlaceholderResolver<T> placeholders() {
        return PLACEHOLDERS.bindCtx(this);
    }

}
