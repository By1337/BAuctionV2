package com.by1337.auc.handler;

import com.by1337.auc.BAuction;
import com.by1337.auc.auc.GhostLot;
import com.by1337.auc.common.auc.log.AuctionLog;
import com.by1337.auc.common.auc.log.BuyAuctionLog;
import com.by1337.auc.handler.event.OfflineProfitNotifyEvent;
import com.by1337.auc.pipeline.LocalChannelContext;
import com.by1337.auc.pipeline.LocalChannelHandler;
import com.by1337.auc.pipeline.LocalPipeline;
import com.by1337.auc.pipeline.Remote;
import com.by1337.auc.user.AucUser;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.sync.common.channel.ChannelMessage;

import java.util.UUID;

public class OfflineProfitCollector implements LocalChannelHandler {
    private SimpleAuction auction;

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.auction = auction;
        auction.logRepo().registerLogListener(this::onLog);
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof OfflineProfitNotifyEvent(UUID player)) {
            onJoin(player);
        } else {
            ctx.fire(msg);
        }
    }

    private void onJoin(UUID player) {
        AucUser user = auction.users().getUser(player);
        if (user == null) return;
        auction.logRepo().loadLogs(user.lastSoldLotSeenTimestamp, null, player, BuyAuctionLog.ID, 100).ifPresent(list -> {
            list.forEach(this::onLog);
        });
    }

    private void onLog(AuctionLog log) {
        if (!(log instanceof BuyAuctionLog buy)) return;
        var auction = BAuction.auction();
        if (auction == null) return;
        if (!BAuction.playerList().isOnline(buy.lotOwner)) return;
        auction.loadName(buy.buyer).ifPresent(buyer -> auction.loadItem(buy.item).ifPresent(item -> auction.loadName(buy.lotOwner).ifPresent(name -> {
            AucUser user = auction.users().getUser(buy.lotOwner);
            if (user == null) return;
            GhostLot lot = new GhostLot(item, buy.lotOwner, name, buy.lprice, buy.count);
            user.lastSoldLotSeenTimestamp = buy.timestamp;
            BAuction.sendMessage("on_sell_lot", buy.lotOwner,
                    lot.<EventContext>placeholders().append("buyer", buyer.name())
            );
        })));
    }

    @Override
    public void close() {

    }
}
