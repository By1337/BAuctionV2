package dev.by1337.auc.transaction;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.util.mc.MCUtil;
import dev.by1337.sync.common.callback.ResponseFuture;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class TakeLotTransaction implements Transaction<ActionResult> {
    private static final ResponseFuture<ActionResult> DENY = new ResponseFuture<>(ActionResult.deny());
    private static final Logger log = LoggerFactory.getLogger(TakeLotTransaction.class);
    private final UUID who;
    private final ClientAucLot lot;

    public TakeLotTransaction(UUID who, ClientAucLot lot) {
        this.who = who;
        this.lot = lot;
    }

    @Override
    public ResponseFuture<ActionResult> apply(Auction auction) {
        if (!lot.lot.owner().equals(who)) {
            BAuction.sendMessage("not_owner_lot", who);
            return DENY;
        }
        return auction.removeLot(lot).then(v -> {
            if (v == null || !v.success){
                BAuction.sendMessage("outdated_lot", who);
                return;
            }
            MCUtil.ensureMain(() -> {
                Player player = BAuction.playerList().getPlayer(who);
                if (player != null) {
                    var item = lot.itemStack.asQuantity(lot.lot.count());
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
                    } else {
                        BAuction.sendMessage("item_taken", who, lot.placeholders());
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
