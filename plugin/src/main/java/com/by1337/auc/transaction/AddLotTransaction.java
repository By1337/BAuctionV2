package com.by1337.auc.transaction;

import com.by1337.auc.BAuction;
import com.by1337.auc.auc.GhostLot;
import com.by1337.auc.handler.Auction;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.sync.common.callback.ResponseFuture;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AddLotTransaction implements Transaction<@Nullable GhostLot> {
    private static final ResponseFuture<@Nullable GhostLot> EMPTY = new ResponseFuture<>(null);
    private final ItemStack itemStack;
    private final UUID who;
    private final double price;
    private final int count;

    public AddLotTransaction(ItemStack itemStack, UUID who, double price, int count) {
        this.itemStack = itemStack.getAmount() != 1 ? itemStack.asOne() : itemStack;
        this.who = who;
        this.count = count;
        this.price = price;
    }

    @Override
    public ResponseFuture<@Nullable GhostLot> apply(Auction auction) {
        if (price / count < 1) {
            BAuction.sendMessage("minimum_price", who, PlaceholderResolver.of("min", count));
            return EMPTY;
        }
        //todo checks
        if (itemStack.isEmpty()) {
            BAuction.sendMessage("illegal_item", who);
            return EMPTY;
        }
        long lprice = (long) (price * 100D);
        //todo checks limit
        return auction.addLot(
                itemStack,
                who,
                TimeUnit.DAYS.toMillis(1), //todo config?
                count,
                lprice
        );
    }
}
