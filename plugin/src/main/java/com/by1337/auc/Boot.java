package com.by1337.auc;

import com.by1337.auc.handler.OfflineProfitCollector;
import com.by1337.auc.handler.SimpleAuction;
import com.by1337.auc.handler.event.OfflineProfitNotifyEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

class Boot {

    public static void boot(BAuction plugin, SimpleAuction auction) {
        plugin.config().tags.search.forEach((k, v) -> v.boot(auction.tag2id(), k));
        auction.pipeline().addLast("offline_profit_collector", new OfflineProfitCollector());

        BAuction.eventListener().registerListener(PlayerJoinEvent.class, join -> {
            UUID uuid = join.getPlayer().getUniqueId();
            auction.pipeline().execute(new OfflineProfitNotifyEvent(uuid));
        });
    }

}
