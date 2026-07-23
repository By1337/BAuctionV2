package dev.by1337.auc.transaction;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.GhostLot;
import dev.by1337.auc.common.auc.log.impl.AddLotLog;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.util.number.NumberFormatter;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.sync.common.callback.ResponseFuture;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AddLotTransaction implements Transaction<@Nullable GhostLot> {
    private static final ResponseFuture<@Nullable GhostLot> EMPTY = new ResponseFuture<>(null);
    private final ItemStack itemStack;
    private final UUID who;
    private final double price;
    private final int count;
    private boolean skipSlotsCheck;
    private boolean skipPriceChecks;
    private long sellingDuration;

    public AddLotTransaction(ItemStack itemStack, UUID who, double price, int count) {
        this.itemStack = itemStack.getAmount() != 1 ? itemStack.asOne() : itemStack;
        this.who = who;
        this.count = count;
        this.price = price;
        sellingDuration = BAuction.plugin().config().selling_duration;
    }

    @Override
    public ResponseFuture<@Nullable GhostLot> apply(Auction auction) {
        if (itemStack.isEmpty()) {
            BAuction.sendMessage("illegal_item", who);
            return EMPTY;
        }

        if (!skipSlotsCheck) {
            var player = BAuction.playerList().getPlayer(who);
            if (player == null) return EMPTY;
            int limit = BAuction.plugin().config().slots.collectSlots(player);
            int used = auction.getPlayerOwnedLotsCount(who);
            if (limit - used <= 0) {
                BAuction.sendMessage("slots_limited", who);
                return EMPTY;
            }
        }
        if (!skipPriceChecks) {
            var cfg = BAuction.plugin().config();
            if (cfg.priceLimiter.enabled()) {
                var max = cfg.priceLimiter.getMaxPrice(itemStack.asQuantity(count));
                if (price > max) {
                    BAuction.sendMessage("maximum_price", who, PlaceholderResolver.of("max", NumberFormatter.format(max)));
                    return EMPTY;
                }
            }
        }
        long lprice = (long) (price * 100D);
        return auction.makeGhostLot(itemStack, who, count, lprice)
                .ifEmpty(() -> BAuction.sendMessage("auction_is_disabled", who))
                .map(ghostLot -> {
                    if (price / count < 0.01D) {
                        BAuction.sendMessage("minimum_price", who, ghostLot.<EventContext>placeholders()
                                .append("min", 0.01D * count));
                        return null;
                    }
                    return ghostLot;
                }).flatMap(ignored -> auction.addLot(
                        itemStack,
                        who,
                        sellingDuration,
                        count,
                        lprice
                ).then(l -> {
                    if (l != null) {
                        auction.publishLog(new AddLotLog(
                                System.currentTimeMillis(),
                                who,
                                lprice,
                                l.itemStack().id(),
                                count
                        ));
                    }
                }));
    }

    public AddLotTransaction setSellingDuration(long sellingDuration) {
        this.sellingDuration = sellingDuration;
        return this;
    }

    public AddLotTransaction dsell(boolean flag) {
        if (flag) {
            sellingDuration = 0;
        }
        return this;
    }

    public AddLotTransaction skipSlotsCheck() {
        this.skipSlotsCheck = true;
        return this;
    }

    public AddLotTransaction skipPriceChecks() {
        this.skipPriceChecks = true;
        return this;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    public UUID who() {
        return who;
    }

    public double price() {
        return price;
    }

    public int count() {
        return count;
    }
}
