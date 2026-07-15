package com.by1337.auc.transaction;

import com.by1337.auc.BAuction;
import com.by1337.auc.auc.ClientAucLot;
import com.by1337.auc.common.auc.log.BuyAuctionLog;
import com.by1337.auc.handler.Auction;
import com.by1337.auc.handler.event.ActionResult;
import com.by1337.auc.util.mc.MCUtil;
import dev.by1337.sync.common.callback.ResponseFuture;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class BuyLotTransaction implements Transaction<ActionResult> {
    private static final ResponseFuture<ActionResult> DENY = new ResponseFuture<>(ActionResult.deny());
    private static final Logger log = LoggerFactory.getLogger(BuyLotTransaction.class);
    private final UUID who;
    private final ClientAucLot lot;

    public BuyLotTransaction(UUID who, ClientAucLot lot) {
        this.who = who;
        this.lot = lot;
    }

    @Override
    public ResponseFuture<ActionResult> apply(Auction auction) {
        var eco = BAuction.economy();
        double balance = eco.getBalance(who);
        if (balance < lot.dprice) {
            BAuction.sendMessage("insufficient_balance", who);
            return DENY;
        }
        eco.withdrawPlayer(who, lot.dprice);
        Runnable undoBalance = () -> eco.depositPlayer(who, lot.dprice);
        return auction.removeLot(lot).then(v -> {
            if (v == null || !v.success) {
                undoBalance.run();
                BAuction.sendMessage("outdated_lot", who);
                return;
            }
            BAuction.sendMessage("buy_success", who, lot.placeholders());
            eco.depositPlayer(lot.owner(), lot.dprice); //todo некоторые смешные экономики не умеют обрабатывать deposit с нескольких серверов
            auction.publishLog(new BuyAuctionLog(
                    System.currentTimeMillis(),
                    who,
                    lot.owner(),
                    lot.lprice(),
                    lot.itemStack.id(),
                    lot.count()
            ));

            MCUtil.ensureMain(() -> {
                Player player = Bukkit.getPlayer(who);
                if (player != null) {
                    var item = lot.itemStack().asQuantity(lot.count());
                    var items = player.getInventory().addItem(item).values();
                    if (!items.isEmpty()) {
                        items.forEach(i -> auction.addToVault(
                                i,
                                i.getAmount(),
                                who,
                                lot.lprice_for_one * i.getAmount()
                        ).then(r -> {
                            if (r == null || !r.success) {
                                log.error("Не удалось переместить ItemStack в хранилище!{} {}", lot, i);
                            }
                        }));
                        BAuction.sendMessage("err_full_inv_added_to_vault", player);
                    }
                } else {
                    auction.moveToVault(lot, who)
                            .then(b -> {
                                if (b == null || !b) {
                                    log.error("Сервер не ответил на C2SMove2VaultRequest {}", lot);
                                }
                            });
                }
            });
        });
    }
}
