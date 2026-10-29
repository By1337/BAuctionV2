package dev.by1337.auc.auc.util;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.auc.eco.VaultHook;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.user.AucUser;
import dev.by1337.auc.user.UserMails;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.k2v.PlayerDataRepository;
import dev.by1337.vault.log.LogDataWriter;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class BatchedLotSubtractorTest {
    @Test
    @SuppressWarnings("unchecked")
    public void bVaultPaysOfflineSellerAfterRemovalAndKeepsNotificationInMailbox() {
        UUID buyer = UUID.randomUUID();
        UUID seller = UUID.randomUUID();
        var auction = mock(Auction.class);
        var economy = mock(VaultHook.class);
        var users = (PlayerDataRepository<AucUser>) mock(PlayerDataRepository.class);
        var removal = new ResponseFuture<ActionResult>();
        var saleLog = new ResponseFuture<Long>();
        when(economy.isBVault()).thenReturn(true);
        when(auction.users()).thenReturn(users);
        when(auction.subtractOrRemoveLot(10, 3)).thenReturn(removal);
        when(auction.publishLog(any(AuctionLog.class))).thenReturn(saleLog);
        var subtractor = new BatchedLotSubtractor().append(10, 3, 20, 123, seller);

        try (var plugin = mockStatic(BAuction.class)) {
            plugin.when(BAuction::economy).thenReturn(economy);
            subtractor.apply(buyer, auction);
            verifyNoInteractions(economy, users);

            removal.complete(new ActionResult(true));

            ArgumentCaptor<Consumer<LogDataWriter>> details = ArgumentCaptor.forClass(Consumer.class);
            verify(economy).depositCents(eq(seller), eq(369L), details.capture());
            verifyNoInteractions(users);
            plugin.verify(BAuction::playerList, never());
            var writer = mock(LogDataWriter.class, RETURNS_SELF);
            details.getValue().accept(writer);
            verify(writer).setString("reason", "sale");
            verify(writer).setUUID("buyer", buyer);
            verify(writer).setInt("lot", 10);
            verify(writer).setInt("item", 20);
            verify(writer).setInt("count", 3);

            saleLog.complete(42L);
            verify(users).pushMail(seller, UserMails.makeLotSold(42));
            verifyNoMoreInteractions(users);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void vaultQueuesBothPaymentAndNotificationInsteadOfPayingDirectly() {
        UUID buyer = UUID.randomUUID();
        UUID seller = UUID.randomUUID();
        var auction = mock(Auction.class);
        var economy = mock(VaultHook.class);
        var users = (PlayerDataRepository<AucUser>) mock(PlayerDataRepository.class);
        when(auction.users()).thenReturn(users);
        when(auction.subtractOrRemoveLot(10, 3)).thenReturn(new ResponseFuture<>(new ActionResult(true)));
        when(auction.publishLog(any(AuctionLog.class))).thenReturn(new ResponseFuture<>(42L));
        var subtractor = new BatchedLotSubtractor().append(10, 3, 20, 123, seller);

        try (var plugin = mockStatic(BAuction.class)) {
            plugin.when(BAuction::economy).thenReturn(economy);
            subtractor.apply(buyer, auction);

            verify(economy, never()).depositCents(any(), anyLong(), any());
            verify(users).pushMail(seller, UserMails.makeDepositCents(369));
            verify(users).pushMail(seller, UserMails.makeLotSold(42));
            verifyNoMoreInteractions(users);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void batchPaysEachSellerForTheirMergedQuantities() {
        UUID firstSeller = UUID.randomUUID();
        UUID secondSeller = UUID.randomUUID();
        var auction = mock(Auction.class);
        var economy = mock(VaultHook.class);
        var users = (PlayerDataRepository<AucUser>) mock(PlayerDataRepository.class);
        when(economy.isBVault()).thenReturn(true);
        when(auction.users()).thenReturn(users);
        when(auction.subtractOrRemoveLots(any(int[].class))).thenReturn(new ResponseFuture<>(new ActionResult(true)));
        when(auction.publishLog(any(AuctionLog.class))).thenReturn(new ResponseFuture<>(42L));
        var subtractor = new BatchedLotSubtractor()
                .append(10, 2, 20, 101, firstSeller)
                .append(10, 3, 20, 101, firstSeller)
                .append(11, 4, 21, 202, secondSeller);

        try (var plugin = mockStatic(BAuction.class)) {
            plugin.when(BAuction::economy).thenReturn(economy);
            subtractor.apply(UUID.randomUUID(), auction);

            verify(economy).depositCents(eq(firstSeller), eq(505L), any());
            verify(economy).depositCents(eq(secondSeller), eq(808L), any());
            verify(economy, times(2)).depositCents(any(), anyLong(), any());
            verify(users).pushMail(firstSeller, UserMails.makeLotSold(42));
            verify(users).pushMail(secondSeller, UserMails.makeLotSold(42));
            verifyNoMoreInteractions(users);
        }
    }

    @DataProvider
    public Object[][] failedRemovals() {
        return new Object[][]{
                { false, new ActionResult(false) }, { false, null },
                { true, new ActionResult(false) }, { true, null }
        };
    }

    @Test(dataProvider = "failedRemovals")
    public void failedRemovalDoesNotPayOrSendNotifications(boolean batch, ActionResult result) {
        var auction = mock(Auction.class);
        var economy = mock(VaultHook.class);
        var subtractor = new BatchedLotSubtractor().append(10, 3, 20, 123, UUID.randomUUID());
        if (batch) {
            subtractor.append(11, 2, 21, 234, UUID.randomUUID());
            when(auction.subtractOrRemoveLots(any(int[].class))).thenReturn(new ResponseFuture<>(result));
        } else {
            when(auction.subtractOrRemoveLot(10, 3)).thenReturn(new ResponseFuture<>(result));
        }

        try (var plugin = mockStatic(BAuction.class)) {
            plugin.when(BAuction::economy).thenReturn(economy);
            subtractor.apply(UUID.randomUUID(), auction);

            verifyNoInteractions(economy);
            verify(auction, never()).users();
            verify(auction, never()).publishLog(any());
        }
    }
}
