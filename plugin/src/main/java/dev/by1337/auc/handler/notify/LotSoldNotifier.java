package dev.by1337.auc.handler.notify;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.GhostLot;
import dev.by1337.auc.common.auc.log.BuyAuctionLog;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.handler.event.UserMailEvent;
import dev.by1337.auc.handler.log.LogRepository;
import dev.by1337.auc.pipeline.LocalChannelContext;
import dev.by1337.auc.pipeline.LocalChannelHandler;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.pipeline.Remote;
import dev.by1337.auc.user.AucUser;
import dev.by1337.auc.user.UserMails;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.work.EventLoopWorker;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.UUID;

public class LotSoldNotifier implements LocalChannelHandler {
    private LogRepository aucLogs;
    private boolean closing;
    private EventLoopWorker worker;

    private final Object2ObjectOpenHashMap<UUID, SoldData> messageWaiters = new Object2ObjectOpenHashMap<>();

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        aucLogs = pipeline.get(LogRepository.class);
        worker = pipeline.eventLoop();
        tick();
    }

    private void tick() {
        if (!messageWaiters.isEmpty()) {
            long now = System.currentTimeMillis();
            var iterator = messageWaiters.object2ObjectEntrySet().iterator();
            while (iterator.hasNext()) {
                var v = iterator.next();
                if (now - v.getValue().lastAccess < 1000) continue;
                sendMessages(v.getValue().uids);
                iterator.remove();
            }
        }
        if (!closing)
            worker.schedule(this::tick, 1_000);
    }

    private void sendMessages(LongArrayList operations) {
        var iter = operations.longIterator();
        while (iter.hasNext()) {
            sendSoldMessage(iter.nextLong());
        }
    }

    public void sendSoldMessage(long uid) {
        aucLogs.getLog(uid).ifPresent(log -> {
            if (!(log.log() instanceof BuyAuctionLog buy)) return;
            var auction = BAuction.auction();
            if (auction == null) return;
            auction.loadName(buy.buyer).ifPresent(buyer -> auction.loadItem(buy.item).ifPresent(item -> auction.loadName(buy.lotOwner).ifPresent(name -> {
                GhostLot lot = new GhostLot(item, buy.lotOwner, name, buy.lprice, buy.count);
                BAuction.sendMessage("on_sell_lot", buy.lotOwner,
                        lot.<EventContext>placeholders().append("buyer", buyer.name())
                );
            })));
        });
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof UserMailEvent(AucUser user, String mail)) {
            if (UserMails.isLotSold(mail)) {
                long uid = UserMails.getLong(mail);
                if (BAuction.playerList().isOnline(user.uuid)) {
                    sendSoldMessage(uid);
                } else {
                    var v = messageWaiters.computeIfAbsent(user.uuid, k -> new SoldData());
                    v.lastAccess = System.currentTimeMillis();
                    v.uids.add(uid);
                }
            } else {
                ctx.fire(msg);
            }
        } else {
            ctx.fire(msg);
        }
    }

    @Override
    public void close() {
        closing = true;
    }

    private static class SoldData {
        public long lastAccess;
        private final LongArrayList uids = new LongArrayList();
    }
}
