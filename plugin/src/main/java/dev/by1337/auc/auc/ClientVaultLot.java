package dev.by1337.auc.auc;

import dev.by1337.auc.common.auc.VaultLot;
import dev.by1337.auc.handler.name.PlayerName;
import dev.by1337.auc.util.number.EconomyUtil;

import java.util.UUID;

public class ClientVaultLot implements LotData {
    public final VaultLot lot;
    public final PlayerName playerName;
    public final ClientItemStack itemStack;
    public final double dprice;
    public final double dprice_for_one;
    public final long lprice_for_one;

    public ClientVaultLot(VaultLot lot, PlayerName playerName, ClientItemStack itemStack) {
        this.lot = lot;
        this.playerName = playerName;
        this.itemStack = itemStack;
        dprice = EconomyUtil.fromCents(lot.lprice());
        lprice_for_one = lot.lprice() / lot.count();
        dprice_for_one = EconomyUtil.fromCents(lot.lprice() / lot.count());
    }

    @Override
    public ClientItemStack itemStack() {
        return itemStack;
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

    public long lprice() {
        return lot.lprice();
    }

    @Override
    public long lprice_for_one() {
        return lprice_for_one;
    }

    public double dprice() {
        return dprice;
    }

    @Override
    public String ownerName() {
        return playerName.name();
    }

    public double dprice_for_one() {
        return dprice_for_one;
    }

    public int count() {
        return lot.count();
    }

}
