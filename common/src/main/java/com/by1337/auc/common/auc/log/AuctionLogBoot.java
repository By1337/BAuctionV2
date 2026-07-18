package com.by1337.auc.common.auc.log;

public class AuctionLogBoot {

    public static void boot() {
        AuctionLog.REGISTRY.register(BuyAuctionLog.ID, BuyAuctionLog::new);
    }
}
