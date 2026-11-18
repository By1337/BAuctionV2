package dev.by1337.auc.transaction;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.ClientVaultLot;
import dev.by1337.auc.auc.GhostLot;
import dev.by1337.auc.auc.sort.SortingBoot;
import dev.by1337.auc.common.auc.AucLot;
import dev.by1337.auc.common.auc.VaultLot;
import dev.by1337.auc.config.Config;
import dev.by1337.auc.config.SlotsConf;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.registry.AucRegistries;
import dev.by1337.auc.search.LotsResult;
import dev.by1337.auc.search.PlayerVaultResult;
import dev.by1337.auc.search.SearchResult;
import dev.by1337.auc.user.AucUser;
import dev.by1337.auc.user.pdc.PdcMap;
import dev.by1337.auc.util.mc.PlayerList;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.k2v.PlayerDataRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.mockito.MockedStatic;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ResellTransactionTest {
    @Test
    public void fullAuctionRelistsActiveLotWithoutMovingItemsFromVault() {
        var fixture = new Fixture(1);
        var item = mock(ClientItemStack.class);
        var activeLot = new ClientAucLot(new AucLot(1, 2, fixture.owner, 0, 100, 3, 1500), item, 0, null);
        fixture.activeLots(activeLot);
        var vaultLot = mock(ClientVaultLot.class);
        when(fixture.auction.playerVaultLots(fixture.owner)).thenReturn(new PlayerVaultResult(List.of(vaultLot).iterator(), 1));
        when(fixture.auction.getPlayerOwnedLotsCount(fixture.owner)).thenReturn(1);
        var removal = new ResponseFuture<ActionResult>();
        var addition = new ResponseFuture<GhostLot>();
        when(fixture.auction.removeLot(activeLot)).thenReturn(removal);
        when(fixture.auction.addLot(item, fixture.owner, Fixture.SELLING_DURATION, 3, 1500)).thenReturn(addition);

        try (var plugin = mockStatic(BAuction.class); var bukkit = mockStatic(Bukkit.class)) {
            fixture.stubPlugin(plugin);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            var result = new ResellTransaction(fixture.owner).apply(fixture.auction);
            verify(fixture.auction).removeLot(activeLot);
            verify(fixture.auction, never()).removeVaultLot(any());
            plugin.verify(() -> BAuction.sendMessage("slots_limited", fixture.owner), never());
            plugin.verify(() -> BAuction.sendMessage("resell_success", fixture.owner), never());
            assertFalse(result.hasResult());

            removal.complete(new ActionResult(true));
            verify(fixture.auction).addLot(item, fixture.owner, Fixture.SELLING_DURATION, 3, 1500);
            assertFalse(result.hasResult());

            addition.complete(mock(GhostLot.class));
            var success = new AtomicReference<Boolean>();
            result.then(success::set);
            assertEquals(success.get(), Boolean.TRUE);
            plugin.verify(() -> BAuction.sendMessage("resell_success", fixture.owner));
        }
    }

    @Test
    public void vaultLotRequiresAFreeSlot() {
        var fixture = new Fixture(0);
        fixture.activeLots();
        var vaultLot = mock(ClientVaultLot.class);
        when(fixture.auction.playerVaultLots(fixture.owner)).thenReturn(new PlayerVaultResult(List.of(vaultLot).iterator(), 1));

        try (var plugin = mockStatic(BAuction.class)) {
            fixture.stubPlugin(plugin);
            var success = new AtomicReference<Boolean>();
            new ResellTransaction(fixture.owner).apply(fixture.auction).then(success::set);
            assertEquals(success.get(), Boolean.FALSE);
            plugin.verify(() -> BAuction.sendMessage("slots_limited", fixture.owner));
            verify(fixture.auction, never()).removeVaultLot(any());
        }
    }

    @Test
    public void rejectedRemovalDoesNotReportSuccess() {
        var fixture = new Fixture(1);
        var item = mock(ClientItemStack.class);
        var lot = new ClientAucLot(new AucLot(1, 2, fixture.owner, 0, 100, 3, 1500), item, 0, null);
        fixture.activeLots(lot);
        when(fixture.auction.playerVaultLots(fixture.owner)).thenReturn(new PlayerVaultResult(List.<ClientVaultLot>of().iterator(), 0));
        when(fixture.auction.removeLot(lot)).thenReturn(new ResponseFuture<>(ActionResult.deny()));

        try (var plugin = mockStatic(BAuction.class); var bukkit = mockStatic(Bukkit.class)) {
            fixture.stubPlugin(plugin);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            var success = new AtomicReference<Boolean>();
            new ResellTransaction(fixture.owner).apply(fixture.auction).then(success::set);

            assertEquals(success.get(), Boolean.FALSE);
            verify(fixture.auction, never()).addLot(eq(item), any(), anyLong(), anyInt(), anyLong());
            plugin.verify(() -> BAuction.sendMessage("outdated_lot", fixture.owner));
            plugin.verify(() -> BAuction.sendMessage("resell_success", fixture.owner), never());
        }
    }

    @Test
    public void oneFreeSlotRestoresOnlyOneVaultLot() {
        var fixture = new Fixture(2);
        var activeItem = mock(ClientItemStack.class);
        var activeLot = new ClientAucLot(new AucLot(1, 2, fixture.owner, 0, 100, 3, 1500), activeItem, 0, null);
        fixture.activeLots(activeLot);
        when(fixture.auction.getPlayerOwnedLotsCount(fixture.owner)).thenReturn(1);
        var vaultItem = mock(ClientItemStack.class);
        var firstVaultLot = new ClientVaultLot(new VaultLot(2, 3, fixture.owner, 100, 2, 1000), null, vaultItem);
        var secondVaultLot = mock(ClientVaultLot.class);
        var vaultLots = List.of(firstVaultLot, secondVaultLot);
        when(fixture.auction.playerVaultLots(fixture.owner)).thenReturn(new PlayerVaultResult(vaultLots.iterator(), vaultLots.size()));

        when(fixture.auction.removeLot(activeLot)).thenReturn(new ResponseFuture<>(new ActionResult(true)));
        when(fixture.auction.removeVaultLot(firstVaultLot)).thenReturn(new ResponseFuture<>(new ActionResult(true)));
        var activeAddition = new ResponseFuture<GhostLot>();
        when(fixture.auction.addLot(activeItem, fixture.owner, Fixture.SELLING_DURATION, 3, 1500)).thenReturn(activeAddition);
        when(fixture.auction.addLot(vaultItem, fixture.owner, Fixture.SELLING_DURATION, 2, 1000))
                .thenReturn(new ResponseFuture<>(mock(GhostLot.class)));

        try (var plugin = mockStatic(BAuction.class); var bukkit = mockStatic(Bukkit.class)) {
            fixture.stubPlugin(plugin);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            var result = new ResellTransaction(fixture.owner).apply(fixture.auction);
            verify(fixture.auction).removeVaultLot(firstVaultLot);
            verify(fixture.auction, never()).removeVaultLot(secondVaultLot);
            verify(fixture.auction).removeLot(activeLot);
            assertFalse(result.hasResult());

            activeAddition.complete(mock(GhostLot.class));
            assertTrue(result.hasResult());
            plugin.verify(() -> BAuction.sendMessage("resell_success", fixture.owner));
        }
    }

    private static class Fixture {
        private static final long SELLING_DURATION = 60_000;
        private final UUID owner = UUID.randomUUID();
        private final Auction auction = mock(Auction.class);
        private final PlayerList players = mock(PlayerList.class);
        private final BAuction plugin = mock(BAuction.class);
        private final AucRegistries registries = new AucRegistries();

        @SuppressWarnings("unchecked")
        private Fixture(int slotLimit) {
            var player = mock(Player.class);
            when(player.hasPermission("bauc.test")).thenReturn(true);
            when(players.getPlayer(owner)).thenReturn(player);
            var slots = new SlotsConf(List.of(new SlotsConf.SlotsPermPair(slotLimit, "bauc.test")), List.of());
            var config = mock(Config.class);
            setConfigField(config, "slots", slots);
            setConfigField(config, "selling_duration", SELLING_DURATION);
            when(plugin.config()).thenReturn(config);
            var users = (PlayerDataRepository<AucUser>) mock(PlayerDataRepository.class);
            when(users.getUser(owner)).thenReturn(new AucUser(owner, new PdcMap()));
            when(auction.users()).thenReturn(users);
            SortingBoot.boot(registries.sorting, List.of("newest"));
            when(auction.registries()).thenReturn(registries);
        }

        private void activeLots(ClientAucLot... lots) {
            when(auction.search(owner, null, registries.sorting.first()))
                    .thenReturn(new SearchResult(LotsResult.memoized(List.of(lots))));
        }

        private void stubPlugin(MockedStatic<BAuction> mock) {
            mock.when(BAuction::plugin).thenReturn(plugin);
            mock.when(BAuction::playerList).thenReturn(players);
        }

        private static void setConfigField(Config config, String name, Object value) {
            try {
                var field = Config.class.getField(name);
                field.setAccessible(true);
                field.set(config, value);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
    }
}
