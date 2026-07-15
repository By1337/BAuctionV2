package com.by1337.auc.auc;

import com.by1337.auc.handler.name.PlayerName;

import java.util.UUID;

public class GhostLot implements LotData {
    private final ClientItemStack itemStack;
    private final UUID owner;
    private final PlayerName ownerName;
    private final long lprice;
    private final int count;
    private final double dprice;
    public final double dprice_for_one;
    public final long lprice_for_one;

    public GhostLot(ClientItemStack itemStack, UUID owner, PlayerName ownerName, long lprice, int count) {
        this.itemStack = itemStack;
        this.owner = owner;
        this.ownerName = ownerName;
        this.lprice = lprice;
        dprice = lprice / 100D;
        lprice_for_one = lprice / count;
        dprice_for_one = lprice_for_one / 100D;
        this.count = count;
    }

    @Override
    public ClientItemStack itemStack() {
        return itemStack;
    }

    @Override
    public int uid() {
        return -1;
    }

    @Override
    public UUID owner() {
        return owner;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public long removalDate() {
        return System.currentTimeMillis();
    }

    @Override
    public long lprice() {
        return lprice;
    }

    @Override
    public double dprice() {
        return dprice;
    }

    @Override
    public double dprice_for_one() {
        return dprice_for_one;
    }

    @Override
    public String ownerName() {
        return ownerName.name();
    }
}
