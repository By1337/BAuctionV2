package dev.by1337.auc.transaction;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.GhostLot;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.util.DurationFormatter;
import dev.by1337.auc.util.mc.InvUtil;
import dev.by1337.auc.util.mc.MCUtil;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.sync.common.callback.ResponseFuture;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
        var activeLots = activeLots(auction);
        if (activeLots.isEmpty() && vault.size() == 0) {
            BAuction.sendMessage("has_no_items_resell", who);
            return new ResponseFuture<>(false);
        }
        var config = BAuction.plugin().config();
        int freeSlots = vault.size() == 0 ? 0 : config.slots.collectSlots(player) - auction.getPlayerOwnedLotsCount(who);
        if (activeLots.isEmpty() && freeSlots <= 0) {
            BAuction.sendMessage("slots_limited", who);
            return new ResponseFuture<>(false);
        }
        user.pdc().setLong("resell.cooldown", now + config.resell_cooldown);

        var location = player.getLocation();
        var relisted = new ArrayList<ResponseFuture<GhostLot>>();
        for (int i = 0; i < freeSlots; i++) {
            var lot = vault.next();
            if (lot == null) break;
            relisted.add(relist(auction, auction.removeVaultLot(lot), lot.itemStack, lot.owner(),
                    config.selling_duration, lot.count(), lot.centsPrice(), location));
        }
        for (ClientAucLot lot : activeLots) {
            relisted.add(relist(auction, auction.removeLot(lot), lot.itemStack, lot.owner(),
                    config.selling_duration, lot.count(), lot.centsPrice(), location));
        }
        if (relisted.isEmpty()) {
            BAuction.sendMessage("outdated_lot", who);
            return new ResponseFuture<>(false);
        }

        var result = new ResponseFuture<Boolean>();
        var remaining = new AtomicInteger(relisted.size());
        var anySuccess = new AtomicBoolean();
        for (var request : relisted) {
            request.then(lot -> {
                if (lot != null) anySuccess.set(true);
                if (remaining.decrementAndGet() == 0) {
                    MCUtil.ensureMain(() -> {
                        boolean success = anySuccess.get();
                        BAuction.sendMessage(success ? "resell_success" : "outdated_lot", who);
                        result.complete(success);
                    });
                }
            });
        }
        return result;
    }

    private List<ClientAucLot> activeLots(Auction auction) {
        var lots = auction.search(who, null, auction.registries().sorting.first());
        try {
            var active = new ArrayList<ClientAucLot>();
            ClientAucLot lot;
            while ((lot = lots.next()) != null) {
                active.add(lot);
            }
            return active;
        } finally {
            lots.release();
        }
    }

    private ResponseFuture<GhostLot> relist(Auction auction, ResponseFuture<ActionResult> removal, ClientItemStack item,
        UUID owner, long duration, int count, long centsPrice, Location location) {
        return removal.flatMap(result -> {
            if (!result.success) return new ResponseFuture<>(null);
            return auction.addLot(item, owner, duration, count, centsPrice)
                    .ifEmpty(() -> returnItem(item.asQuantity(count), location));
        });
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

    public UUID who() {
        return who;
    }
}
