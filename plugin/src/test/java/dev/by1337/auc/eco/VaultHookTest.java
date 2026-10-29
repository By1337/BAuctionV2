package dev.by1337.auc.eco;

import dev.by1337.vault.BVaultApi;
import dev.by1337.vault.log.LogDataWriter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.testng.annotations.Test;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class VaultHookTest {
    @Test
    public void bVaultUsesExactCentsAndDoesNotResolveBukkitPlayersOrVault() {
        var economy = mock(dev.by1337.vault.Economy.class);
        UUID who = UUID.randomUUID();
        long cents = (1L << 53) + 1;
        when(economy.getBalanceIfLoaded(who)).thenReturn(cents);
        Consumer<LogDataWriter> details = writer -> writer.setString("reason", "test");

        try (var api = mockStatic(BVaultApi.class); var bukkit = mockStatic(Bukkit.class)) {
            api.when(BVaultApi::defaultEconomy).thenReturn(economy);
            var hook = new VaultHook();

            assertTrue(hook.isBVault());
            assertEquals(hook.getCents(who), cents);
            hook.withdrawCents(who, cents, details);
            hook.depositCents(who, cents, details);

            verify(economy).withdraw(who, cents, "BAuctionV2", details);
            verify(economy).deposit(who, cents, "BAuctionV2", details);
            bukkit.verifyNoInteractions();
        }
    }

    @Test
    public void bVaultRetainsCoinBasedMethodsAndMethodsWithoutLogDetails() {
        var economy = mock(dev.by1337.vault.Economy.class);
        UUID who = UUID.randomUUID();
        when(economy.getBalanceIfLoaded(who)).thenReturn(1234L);
        try (var api = mockStatic(BVaultApi.class)) {
            api.when(BVaultApi::defaultEconomy).thenReturn(economy);
            var hook = new VaultHook();

            assertEquals(hook.getBalance(who), 12.34);
            hook.withdrawPlayer(who, 2.34);
            hook.depositPlayer(who, 5.67);
            hook.withdrawCents(who, 123);
            hook.depositCents(who, 456);

            verify(economy).withdraw(who, 234L, "BAuctionV2", null);
            verify(economy).deposit(who, 567L, "BAuctionV2", null);
            verify(economy).withdraw(who, 123L, "BAuctionV2", null);
            verify(economy).deposit(who, 456L, "BAuctionV2", null);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void missingBVaultEconomyFallsBackToVaultAndIgnoresLogDetails() {
        UUID who = UUID.randomUUID();
        var server = mock(Server.class);
        var services = mock(ServicesManager.class);
        var registration = (RegisteredServiceProvider<Economy>) mock(RegisteredServiceProvider.class);
        var economy = mock(Economy.class);
        var player = mock(OfflinePlayer.class);
        when(server.getServicesManager()).thenReturn(services);
        when(services.getRegistration(Economy.class)).thenReturn(registration);
        when(registration.getProvider()).thenReturn(economy);
        when(economy.getBalance(player)).thenReturn(12.34);
        Consumer<LogDataWriter> details = writer -> fail("Vault log details must be NOP");

        try (var api = mockStatic(BVaultApi.class); var bukkit = mockStatic(Bukkit.class)) {
            api.when(BVaultApi::defaultEconomy).thenReturn(null);
            bukkit.when(Bukkit::getServer).thenReturn(server);
            bukkit.when(() -> Bukkit.getOfflinePlayer(who)).thenReturn(player);
            var hook = new VaultHook();

            assertFalse(hook.isBVault());
            assertEquals(hook.getCents(who), 1234L);
            assertEquals(hook.getBalance(who), 12.34);
            hook.withdrawCents(who, 234, details);
            hook.depositCents(who, 567, details);
            hook.withdrawPlayer(who, 1.23);
            hook.depositPlayer(who, 4.56);

            verify(economy).withdrawPlayer(player, 2.34);
            verify(economy).depositPlayer(player, 5.67);
            verify(economy).withdrawPlayer(player, 1.23);
            verify(economy).depositPlayer(player, 4.56);
        }
    }
}
