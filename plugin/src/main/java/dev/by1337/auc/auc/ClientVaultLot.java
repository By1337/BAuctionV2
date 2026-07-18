package dev.by1337.auc.auc;

import dev.by1337.auc.common.auc.VaultLot;
import dev.by1337.auc.handler.name.PlayerName;

import java.util.UUID;

public class ClientVaultLot implements LotData {
    /*    public static final PlaceholderResolver<ClientVaultLot> PLACEHOLDERS = Placeholders.<ClientVaultLot>create()
                //  .withContext("expires", v -> TimeUtil.getFormat(v.removalDate))
                //.and(ClientAucLot.PLACEHOLDERS.map(v -> v.lot))
                ;*/
    public final VaultLot lot;
    public final PlayerName playerName;
    public final ClientItemStack itemStack;
    public final double dprice;
    public final double dprice_for_one;

    public ClientVaultLot(VaultLot lot, PlayerName playerName, ClientItemStack itemStack) {
        this.lot = lot;
        this.playerName = playerName;
        this.itemStack = itemStack;
        dprice = lot.lprice() / 100D;
        dprice_for_one = ((double) lot.lprice() / lot.count()) / 100D;
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
