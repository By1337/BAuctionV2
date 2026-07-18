package dev.by1337.auc.transaction;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.common.auc.log.BuyAuctionLog;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.user.UserMails;
import dev.by1337.auc.util.mc.MCUtil;
import dev.by1337.auc.util.number.NumberFormatter;
import dev.by1337.edsl.context.EventContext;
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
    private final int count;

    public BuyLotTransaction(UUID who, ClientAucLot lot, int count) {
        this.who = who;
        this.lot = lot;
        this.count = count;
    }

    @Override
    public ResponseFuture<ActionResult> apply(Auction auction) {
        if (count <= 0){
            log.error("Bad count {}", count);
            return DENY;
        }
        var eco = BAuction.economy();
        long balanceCents = eco.getCents(who);
        long priceCents = lot.lprice_for_one * count;

        if (balanceCents < priceCents) {
            BAuction.sendMessage("insufficient_balance", who);
            return DENY;
        }
        eco.withdrawCents(who, priceCents);
        Runnable undoBalance = () -> eco.depositCents(who, priceCents);
        return auction.subtractOrRemoveLot(lot, count).then(v -> {
            if (v == null || !v.success) {
                undoBalance.run();
                BAuction.sendMessage("outdated_lot", who);
                return;
            }
            var lotSeller = lot.owner();
            BAuction.sendMessage("buy_success", who,
                    lot.<EventContext>placeholders()
                            .append("price", NumberFormatter.format(priceCents / 100D))
                            .append("count", count)
            );
            auction.publishLog(new BuyAuctionLog(
                    System.currentTimeMillis(),
                    who,
                    lotSeller,
                    priceCents,
                    lot.itemStack.id(),
                    count
            )).ifPresent(id -> auction.users().pushMail(lotSeller, UserMails.makeLotSold(id)));
            auction.users().pushMail(lotSeller, UserMails.makeDepositCents(priceCents));
            MCUtil.ensureMain(() -> {
                Player player = Bukkit.getPlayer(who);
                if (player != null) {
                    var item = lot.itemStack().asQuantity(count);
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
