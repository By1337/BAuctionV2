package dev.by1337.auc.eco;

import dev.by1337.auc.util.number.EconomyUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Objects;
import java.util.UUID;

public class VaultHook {
    private final Economy econ;

    public VaultHook() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        econ = Objects.requireNonNull(rsp, "Economy not found!").getProvider();
    }

    public long getCents(UUID uuid){
        return EconomyUtil.toCents(getBalance(Bukkit.getOfflinePlayer(uuid)));
    }
    public void withdrawCents(UUID uuid, long count) {
        withdrawPlayer(Bukkit.getOfflinePlayer(uuid), EconomyUtil.fromCents(count));
    }
    public void depositCents(UUID uuid, long count) {
        depositPlayer(Bukkit.getOfflinePlayer(uuid), EconomyUtil.fromCents(count));
    }


    public double getBalance(UUID uuid) {
        return getBalance(Bukkit.getOfflinePlayer(uuid));
    }

    public void withdrawPlayer(UUID uuid, double count) {
        withdrawPlayer(Bukkit.getOfflinePlayer(uuid), count);
    }

    public void depositPlayer(UUID uuid, double count) {
        depositPlayer(Bukkit.getOfflinePlayer(uuid), count);
    }

    private double getBalance(OfflinePlayer offlinePlayer) {
        return econ.getBalance(offlinePlayer);
    }

    private void withdrawPlayer(OfflinePlayer offlinePlayer, double count) {
        econ.withdrawPlayer(offlinePlayer, count);
    }

    private void depositPlayer(OfflinePlayer offlinePlayer, double count) {
        econ.depositPlayer(offlinePlayer, count);
    }
}
