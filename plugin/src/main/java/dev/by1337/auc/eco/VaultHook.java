package dev.by1337.auc.eco;

import dev.by1337.auc.util.number.EconomyUtil;
import dev.by1337.vault.BVaultApi;
import dev.by1337.vault.log.LogDataWriter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class VaultHook {
    private static final String LOG_SOURCE = "BAuctionV2";
    private final dev.by1337.vault.Economy bVault;
    private final Economy econ;

    public VaultHook() {
        bVault = BVaultApi.defaultEconomy();
        if (bVault == null) {
            RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
            econ = Objects.requireNonNull(rsp, "Economy not found!").getProvider();
        } else {
            econ = null;
        }
    }

    public boolean isBVault() {
        return bVault != null;
    }

    public long getCents(UUID uuid) {
        if (bVault != null) return bVault.getBalanceIfLoaded(uuid);
        return EconomyUtil.toCents(econ.getBalance(Bukkit.getOfflinePlayer(uuid)));
    }

    public void withdrawCents(UUID uuid, long count) {
        withdrawCents(uuid, count, null);
    }

    public void withdrawCents(UUID uuid, long count, @Nullable Consumer<LogDataWriter> details) {
        if (bVault != null) {
            bVault.withdraw(uuid, count, LOG_SOURCE, details);
        } else {
            econ.withdrawPlayer(Bukkit.getOfflinePlayer(uuid), EconomyUtil.fromCents(count));
        }
    }

    public void depositCents(UUID uuid, long count) {
        depositCents(uuid, count, null);
    }

    public void depositCents(UUID uuid, long count, @Nullable Consumer<LogDataWriter> details) {
        if (bVault != null) {
            bVault.deposit(uuid, count, LOG_SOURCE, details);
        } else {
            econ.depositPlayer(Bukkit.getOfflinePlayer(uuid), EconomyUtil.fromCents(count));
        }
    }

    public double getBalance(UUID uuid) {
        if (bVault != null) return EconomyUtil.fromCents(bVault.getBalanceIfLoaded(uuid));
        return econ.getBalance(Bukkit.getOfflinePlayer(uuid));
    }

    public void withdrawPlayer(UUID uuid, double count) {
        if (bVault != null) {
            withdrawCents(uuid, EconomyUtil.toCents(count));
        } else {
            econ.withdrawPlayer(Bukkit.getOfflinePlayer(uuid), count);
        }
    }

    public void depositPlayer(UUID uuid, double count) {
        if (bVault != null) {
            depositCents(uuid, EconomyUtil.toCents(count));
        } else {
            econ.depositPlayer(Bukkit.getOfflinePlayer(uuid), count);
        }
    }
}
