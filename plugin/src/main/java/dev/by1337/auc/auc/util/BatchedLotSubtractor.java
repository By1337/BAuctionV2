package dev.by1337.auc.auc.util;

import dev.by1337.auc.common.auc.log.impl.BuyAuctionLog;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.user.UserMails;
import dev.by1337.sync.common.callback.ResponseFuture;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BatchedLotSubtractor {
    private static final Logger log = LoggerFactory.getLogger(BatchedLotSubtractor.class);
    private final Int2ObjectOpenHashMap<Data> data = new Int2ObjectOpenHashMap<>();
    private long totalSum = 0;
    private final List<ItemStack> extra = new ArrayList<>();

    public BatchedLotSubtractor append(int uid, int count, int itemId, long lprice_for_one, UUID owner) {
        totalSum += lprice_for_one * count;
        data.merge(uid, new Data(uid, count, itemId, lprice_for_one, owner), Data::and);
        return this;
    }

    public void log(Auction auction) {
        for (Data value : data.values()) {
            var item = auction.getLot(value.uid);
            if (item == null) continue;
           // log.info("{}x{}", item.itemStack.material(), value.count);
        }
    }

    public int getUsedLots(int uid){
        var v = data.get(uid);
        if (v == null) return 0;
        return v.count;
    }
    public int[] usedLots(){
        return data.keySet().toIntArray();
    }

    public long centsTotal() {
        return totalSum;
    }
    public void addExtra(ItemStack item, int count) {
        int max = item.getMaxStackSize();
        while (count > 0){
            int x = Math.min(count, max);
            extra.add(item.asQuantity(x));
            count -= x;
        }
    }

    public List<ItemStack> extra() {
        return extra;
    }

    public ResponseFuture<ActionResult> apply(UUID who, Auction auction) {
        if (data.size() == 1) {
            var data = this.data.int2ObjectEntrySet().iterator().next().getValue();
            return auction.subtractOrRemoveLot(data.uid, data.count).ifPresent(r -> {
                if (!r.success) return;
                giveReward(who, auction);
            });
        }
        int[] raw = new int[data.size() * 2];
        int i = 0;
        for (Data d : data.values()) {
            raw[i] = d.uid;
            raw[i + 1] = d.count;
            i += 2;
        }
        return auction.subtractOrRemoveLots(raw).ifPresent(r -> {
            if (!r.success) return;
            giveReward(who, auction);
        });
    }

    private void giveReward(UUID who, Auction auction) {
        for (Data d : data.values()) {
            long sum = d.lprice_for_one * d.count;
            auction.publishLog(new BuyAuctionLog(
                    System.currentTimeMillis(),
                    who,
                    d.owner,
                    sum,
                    d.itemId,
                    d.count
            )).ifPresent(id -> auction.users().pushMail(d.owner, UserMails.makeLotSold(id)));
            auction.users().pushMail(d.owner, UserMails.makeDepositCents(sum));
        }
    }

    private record Data(int uid, int count, int itemId, long lprice_for_one, UUID owner) {
        public Data and(Data o) {
            return new Data(uid, count + o.count, itemId, lprice_for_one, owner);
        }
    }
}
