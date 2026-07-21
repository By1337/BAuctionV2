package dev.by1337.auc.transaction;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.ClientVaultLot;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.util.mc.InvUtil;
import dev.by1337.auc.util.mc.MCUtil;
import dev.by1337.sync.common.callback.ResponseFuture;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class TakeVaultLotTransaction implements Transaction<ActionResult> {
    private static final ResponseFuture<ActionResult> DENY = new ResponseFuture<>(ActionResult.deny());
    private static final Logger log = LoggerFactory.getLogger(TakeVaultLotTransaction.class);
    private final ClientVaultLot lot;
    private final UUID who;

    public TakeVaultLotTransaction(UUID who, ClientVaultLot lot) {
        this.who = who;
        this.lot = lot;
    }

    @Override
    public ResponseFuture<ActionResult> apply(Auction auction) {
        if (!lot.lot.owner().equals(who)) {
            BAuction.sendMessage("not_owner_lot", who);
            return DENY;
        }
        Player pl = BAuction.playerList().getPlayer(who);
        if (pl == null) return DENY;
        ItemStack itemStack = lot.itemStack().asQuantity(lot.count());
        if (!InvUtil.tryAddFully(itemStack, pl.getInventory(), true)) {
            BAuction.sendMessage("vault_take_inv_is_full", who);
            return DENY;
        }
        return auction.removeVaultLot(lot).then(res -> {
            if (res == null || !res.success) {
                BAuction.sendMessage("outdated_lot", who);
                return;
            }
            MCUtil.ensureMain(() -> {
                Player player = BAuction.playerList().getPlayer(who);
                if (player == null) {
                    auction.readdVaultLot(lot).then(v -> {
                        if (v == null || !v.success) {
                            log.error("VaultLot потерян! не могу выдать и вернуть его! {}", lot);
                        }
                    });
                    return;
                }
                if (!InvUtil.giveOrDrop(player, itemStack)) {
                    BAuction.sendMessage("vault_take_but_dropped", who);
                }
            });
        });
    }
}
