package dev.by1337.auc.auc;

import dev.by1337.auc.common.auc.AucLot;
import dev.by1337.auc.handler.name.PlayerName;

import java.util.UUID;

public class ClientAucLot implements LotData {
    public final AucLot lot;
    public final ClientItemStack itemStack;
    public final int shortId;
    public final double dprice;
    public final double dprice_for_one;
    public final long lprice_for_one;
    public final PlayerName playerName;

    public ClientAucLot(AucLot lot, ClientItemStack itemStack, int shortId, PlayerName playerName) {
        this.lot = lot;
        this.itemStack = itemStack;
        this.shortId = shortId;
        dprice = lot.lprice() / 100D;
        dprice_for_one = ((double) lot.lprice() / lot.count()) / 100D;
        lprice_for_one = lot.lprice_for_one;
        this.playerName = playerName;
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

    public int count() {
        return lot.count();
    }

    public long lprice() {
        return lot.lprice();
    }

    public long lprice_for_one() {
        return lprice_for_one;
    }

    public double dprice() {
        return dprice;
    }

    public double dprice_for_one() {
        return dprice_for_one;
    }

    @Override
    public ClientItemStack itemStack() {
        return itemStack;
    }

    public AucLot lot() {
        return lot;
    }


    @Override
    public String ownerName() {
        return playerName.name();
    }

    @Override
    public String toString() {
        return "ClientAucLot{" +
                "lot=" + lot +
                ", itemStack=" + itemStack +
                ", shortId=" + shortId +
                ", price=" + dprice +
                ", price_for_one=" + dprice_for_one +
                ", lprice_for_one=" + lprice_for_one +
                '}';
    }
}
