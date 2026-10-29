package dev.by1337.auc.transaction;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.BuyableLot;
import dev.by1337.auc.auc.util.BatchedLotSubtractor;
import dev.by1337.auc.eco.VaultHook;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.sync.common.callback.ResponseFuture;
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
