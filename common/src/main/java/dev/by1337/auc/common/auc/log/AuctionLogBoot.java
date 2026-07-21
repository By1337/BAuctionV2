package dev.by1337.auc.common.auc.log;

import dev.by1337.auc.common.auc.log.impl.*;

public class AuctionLogBoot {

    public static void boot() {
        AuctionLog.REGISTRY.register(BuyAuctionLog.ID, BuyAuctionLog::new);
        AuctionLog.REGISTRY.register(AddLotLog.ID, AddLotLog::new);
        AuctionLog.REGISTRY.register(TakeLotLog.ID, TakeLotLog::new);
        AuctionLog.REGISTRY.register(TakeVaultLog.ID, TakeVaultLog::new);
        AuctionLog.REGISTRY.register(LotExpirationLog.ID, LotExpirationLog::new);
        AuctionLog.REGISTRY.register(VaultLotExpirationLog.ID, VaultLotExpirationLog::new);
    }
}
