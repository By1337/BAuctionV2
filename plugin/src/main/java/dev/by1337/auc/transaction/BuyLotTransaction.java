package dev.by1337.auc.transaction;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.BuyableLot;
import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.util.BatchedLotSubtractor;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.util.mc.InvUtil;
import dev.by1337.auc.util.mc.MCUtil;
import dev.by1337.auc.util.number.EconomyUtil;
import dev.by1337.auc.util.number.NumberFormatter;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.sync.common.callback.ResponseFuture;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class BuyLotTransaction implements Transaction<ActionResult> {
    private static final ResponseFuture<ActionResult> DENY = new ResponseFuture<>(ActionResult.deny());
    private static final Logger log = LoggerFactory.getLogger(BuyLotTransaction.class);
    private final UUID who;
    private final BuyableLot lot;
    private final int count;
    private boolean infinityBalance = false;
    private boolean noGiveItem = false;

    public BuyLotTransaction(UUID who, ClientAucLot lot, int count) {
        this(who, (BuyableLot) lot, count);
    }

    public BuyLotTransaction(UUID who, BuyableLot lot, int count) {
        this.who = who;
        this.lot = lot;
        this.count = count;
    }

    @Override
    public ResponseFuture<ActionResult> apply(Auction auction) {
        if (count <= 0) {
            log.error("Bad count {}", count);
            return DENY;
        }
        var eco = BAuction.economy();
        BatchedLotSubtractor subtractor = new BatchedLotSubtractor();
        if (lot.addToSubtractor(subtractor, count, auction) != count) {
            BAuction.sendMessage("outdated_lot", who);
            return DENY;
        }
        long priceCents = subtractor.centsTotal();
        long centForOne = priceCents / count;
        if (!infinityBalance) {
            long balanceCents = eco.getCents(who);
            if (balanceCents < priceCents) {
                BAuction.sendMessage("insufficient_balance", who);
                return DENY;
            }
            eco.withdrawCents(who, priceCents);
        }
        Runnable undoBalance = () -> {
            if (!infinityBalance) eco.depositCents(who, priceCents);
        };
        subtractor.log(auction);
        return subtractor.apply(who, auction).then(v -> {
            if (v == null || !v.success) {
                undoBalance.run();
                BAuction.sendMessage("outdated_lot", who);
                return;
            }
            BAuction.sendMessage("buy_success", who,
                    lot.<EventContext>placeholders()
                            .append("price", NumberFormatter.format(EconomyUtil.fromCents(priceCents)))
                            .append("count", count)
            );
            if (!noGiveItem) {
                MCUtil.ensureMain(() -> {
                    var item = lot.bukkitItem(count);
                    Player player = BAuction.playerList().getPlayer(who);
                    if (player != null) {
                        InvUtil.giveOrDrop(player, subtractor.extra().toArray(new ItemStack[0]));
                        var items = player.getInventory().addItem(item).values();
                        if (!items.isEmpty()) {
                            items.forEach(i -> auction.addToVault(
                                    i,
                                    i.getAmount(),
                                    who,
                                    centForOne * i.getAmount()
                            ).then(r -> {
                                if (r == null || !r.success) {
                                    log.error("Не удалось переместить ItemStack в хранилище!{} {}", lot, i);
                                }
                            }));
                            BAuction.sendMessage("err_full_inv_added_to_vault", player);
                        }
                    } else {
                        auction.addToVault(item, count, who, priceCents)
                                .then(b -> {
                                    if (b == null || !b.success) {
                                        log.error("Не удалось переместить ItemStack в хранилище!{} {}", lot, item);
                                    }
                                });
                    }
                });
            }
        });
    }

    public BuyLotTransaction infinityBalance() {
        this.infinityBalance = log.isErrorEnabled();
        return this;
    }

    public BuyLotTransaction noGiveItem() {
        this.noGiveItem = true;
        return this;
    }

    public UUID who() {
        return who;
    }

    public BuyableLot lot() {
        return lot;
    }

    public int count() {
        return count;
    }
}
