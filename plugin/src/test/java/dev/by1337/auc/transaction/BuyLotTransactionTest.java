package dev.by1337.auc.transaction;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.BuyableLot;
import dev.by1337.auc.auc.util.BatchedLotSubtractor;
import dev.by1337.auc.common.auc.log.impl.BuyAuctionLog;
import dev.by1337.auc.eco.VaultHook;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.user.AucUser;
import dev.by1337.auc.user.UserMails;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.k2v.PlayerDataRepository;
import dev.by1337.vault.log.LogDataWriter;
import it.unimi.dsi.fastutil.ints.IntList;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class BuyLotTransactionTest {
    @DataProvider
    public Object[][] purchasesWithoutSellerReward() {
        return new Object[][]{
                { false, false }, { false, true },
                { true, false }, { true, true }
        };
    }

    @Test(dataProvider = "purchasesWithoutSellerReward")
    @SuppressWarnings("unchecked")
    public void purchaseFlagsSkipPaymentAndItemDeliveryButKeepSaleNotifications(boolean bVault, boolean batch) {
        UUID buyer = UUID.randomUUID();
        UUID seller = UUID.randomUUID();
        UUID secondSeller = UUID.randomUUID();
        var auction = mock(Auction.class);
        var economy = mock(VaultHook.class);
        var lot = mock(BuyableLot.class);
        var users = (PlayerDataRepository<AucUser>) mock(PlayerDataRepository.class);
        when(economy.isBVault()).thenReturn(bVault);
        when(auction.users()).thenReturn(users);
        when(auction.publishLog(any(BuyAuctionLog.class))).thenReturn(new ResponseFuture<>(42L));
        when(lot.<EventContext>placeholders()).thenReturn(PlaceholderResolver.of("item", "test"));
        when(lot.addToSubtractor(any(), eq(3), same(auction))).thenAnswer(invocation -> {
            BatchedLotSubtractor subtractor = invocation.getArgument(0);
            if (batch) {
                subtractor.append(10, 1, 20, 123, seller);
                subtractor.append(11, 2, 21, 234, secondSeller);
            } else {
                subtractor.append(10, 3, 20, 123, seller);
            }
            return 3;
        });
        var success = new ResponseFuture<>(new ActionResult(true));
        if (batch) {
            when(auction.subtractOrRemoveLots(any(int[].class))).thenReturn(success);
        } else {
            when(auction.subtractOrRemoveLot(10, 3)).thenReturn(success);
        }

        try (var plugin = mockStatic(BAuction.class)) {
            plugin.when(BAuction::economy).thenReturn(economy);
            new BuyLotTransaction(buyer, lot, 3)
                    .infinityBalance()
                    .noGiveItem()
                    .noSellerReward()
                    .apply(auction);

            verifyNoInteractions(economy);
            verify(lot, never()).bukkitItem(anyInt());
            verify(auction, times(batch ? 2 : 1)).publishLog(any(BuyAuctionLog.class));
            verify(users).pushMail(seller, UserMails.makeLotSold(42));
            if (batch) {
                verify(users).pushMail(secondSeller, UserMails.makeLotSold(42));
            }
            verifyNoMoreInteractions(users);
        }
    }

    @DataProvider
    public Object[][] failedRemovals() {
        return new Object[][]{ { new ActionResult(false) }, { null } };
    }

    @Test(dataProvider = "failedRemovals")
    @SuppressWarnings("unchecked")
    public void failedPurchaseRefundsBuyerWithTheOriginalLotDetails(ActionResult result) {
        UUID buyer = UUID.randomUUID();
        UUID seller = UUID.randomUUID();
        var auction = mock(Auction.class);
        var economy = mock(VaultHook.class);
        var lot = mock(BuyableLot.class);
        when(economy.getCents(buyer)).thenReturn(1000L);
        when(lot.addToSubtractor(any(), eq(3), same(auction))).thenAnswer(invocation -> {
            invocation.<BatchedLotSubtractor>getArgument(0).append(10, 3, 20, 123, seller);
            return 3;
        });
        when(auction.subtractOrRemoveLot(10, 3)).thenReturn(new ResponseFuture<>(result));

        try (var plugin = mockStatic(BAuction.class)) {
            plugin.when(BAuction::economy).thenReturn(economy);
            new BuyLotTransaction(buyer, lot, 3).apply(auction);

            ArgumentCaptor<Consumer<LogDataWriter>> purchaseDetails = ArgumentCaptor.forClass(Consumer.class);
            ArgumentCaptor<Consumer<LogDataWriter>> refundDetails = ArgumentCaptor.forClass(Consumer.class);
            verify(economy).withdrawCents(eq(buyer), eq(369L), purchaseDetails.capture());
            verify(economy).depositCents(eq(buyer), eq(369L), refundDetails.capture());
            var purchase = mock(LogDataWriter.class, RETURNS_SELF);
            var refund = mock(LogDataWriter.class, RETURNS_SELF);
            purchaseDetails.getValue().accept(purchase);
            refundDetails.getValue().accept(refund);
            verify(purchase).setString("reason", "purchase");
            verify(refund).setString("reason", "purchase_refund");
            verify(purchase).setIntList("lots", IntList.of(10));
            verify(refund).setIntList("lots", IntList.of(10));
            verify(purchase).setInt("count", 3);
            verify(refund).setInt("count", 3);
            verify(auction, never()).users();
            verify(auction, never()).publishLog(any());
        }
    }
}
