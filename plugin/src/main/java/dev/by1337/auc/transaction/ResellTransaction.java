package dev.by1337.auc.transaction;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.ClientVaultLot;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.util.DurationFormatter;
import dev.by1337.auc.util.mc.InvUtil;
import dev.by1337.auc.util.mc.MCUtil;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.sync.common.callback.ResponseFuture;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ResellTransaction implements Transaction<Boolean> {
    private final UUID who;

    public ResellTransaction(UUID who) {
        this.who = who;
    }

    @Override
    public ResponseFuture<Boolean> apply(Auction auction) {
        var player = BAuction.playerList().getPlayer(who);
        if (player == null) return new ResponseFuture<>(false);

        var now = System.currentTimeMillis();
        var user = auction.users().getUser(who);
        if (user == null) return new ResponseFuture<>(false);
        var time = user.pdc().getLong("resell.cooldown");
        if (time > now) {
            BAuction.sendMessage("resell_failed", who, PlaceholderResolver.of("time", DurationFormatter.getFormat(time)));
            return new ResponseFuture<>(false);
        }
        var vault = auction.playerVaultLots(who);
        var lots = auction.search(who, null, auction.registries().sorting.first());

        if (lots.size() == 0 && vault.size() == 0) {
            BAuction.sendMessage("has_no_items_resell", who);
            return new ResponseFuture<>(false);
        }
        user.pdc().setLong("resell.cooldown", now + BAuction.plugin().config().resell_cooldown);

        int limit = BAuction.plugin().config().slots.collectSlots(player);
        Location loc = player.getLocation();
        if (vault.size() != 0) {
            ClientVaultLot l;
            while (limit-- > 0 && (l = vault.next()) != null) {
                final ClientVaultLot lot = l;
                auction.removeVaultLot(lot).then(r -> {
                    if (r == null || !r.success) return;
                    auction.addLot(lot.itemStack, lot.owner(), BAuction.plugin().config().selling_duration, lot.count(), lot.lprice()).then(s -> {
                        if (s == null) {
                            returnItem(lot.itemStack.asQuantity(lot.count()), loc);
                        }
                    });
                });
            }
        }
        if (lots.size() != 0) {
            ClientAucLot l;
            while (limit-- > 0 && (l = lots.next()) != null) {
                final ClientAucLot lot = l;
                auction.removeLot(lot).then(r -> {
                    if (r == null || !r.success) return;
                    auction.addLot(lot.itemStack, lot.owner(), BAuction.plugin().config().selling_duration, lot.count(), lot.lprice()).then(s -> {
                        if (s == null) {
                            returnItem(lot.itemStack.asQuantity(lot.count()), loc);
                        }
                    });
                });
            }
        }
        BAuction.sendMessage("resell_success", who);
        return new ResponseFuture<>(true);
    }

    private void returnItem(ItemStack itemStack, Location location) {
        MCUtil.ensureMain(() -> {
            var pl = BAuction.playerList().getPlayer(who);
            if (pl != null) {
                InvUtil.giveOrDrop(pl, itemStack);
            } else {
                location.getWorld().dropItemNaturally(location, itemStack);
            }
        });
    }
}
