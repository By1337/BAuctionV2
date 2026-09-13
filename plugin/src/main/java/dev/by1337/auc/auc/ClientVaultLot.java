package dev.by1337.auc.auc;

import dev.by1337.auc.auc.util.LotPricer;
import dev.by1337.auc.common.auc.VaultLot;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.name.PlayerName;
import dev.by1337.auc.util.DurationFormatter;
import dev.by1337.plc.PlaceholderResolver;

import java.util.UUID;

public class ClientVaultLot implements LotData {
    private static final PlaceholderResolver<ClientVaultLot> PLACEHOLDERS = LotData.<ClientVaultLot>createPlaceholders()
            .withContext("seller_uuid", v -> v.lot.owner().toString())
            .withContext("seller_name", ClientVaultLot::ownerName)
            .withContext("uid", ClientVaultLot::uid)
            .withContext("expires", v -> DurationFormatter.getFormat(v.removalDate()));
    public final VaultLot lot;
    public final PlayerName playerName;
    public final ClientItemStack itemStack;
    public final LotPricer pricer;

    public ClientVaultLot(VaultLot lot, PlayerName playerName, ClientItemStack itemStack) {
        this.lot = lot;
        this.playerName = playerName;
        this.itemStack = itemStack;
        pricer = new LotPricer(lot.centsPrice() / lot.count(), lot.count());
    }

    @Override
    public LotData update(Auction auction) {
        return auction.getVaultLot(uid());
    }

    @Override
    public ClientItemStack itemStack() {
        return itemStack;
    }

    @Override
    public boolean isOwner(UUID uuid) {
        return owner().equals(uuid);
    }

    public int uid() {
        return lot.uid();
    }

    public UUID owner() {
        return lot.owner();
    }

    public long removalDate() {
        return lot.removalDate();
    }

    public long centsPrice() {
        return lot.centsPrice();
    }

    @Override
    public LotPricer pricer() {
        return pricer;
    }

    @Override
    public <T> PlaceholderResolver<T> placeholders() {
        return PLACEHOLDERS.bindCtx(this);
    }

    public long centsPriceForOne() {
        return pricer.centsPriceForOne;
    }

    public double price() {
        return pricer.price;
    }


    public String ownerName() {
        return playerName.name();
    }

    public double priceForOne() {
        return pricer.priceForOne;
    }

    public int count() {
        return lot.count();
    }

}
