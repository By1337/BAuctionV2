package com.by1337.auc.util.mc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerList implements Listener {
    private final Map<UUID, Player> map = new ConcurrentHashMap<>();

    public PlayerList(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            map.put(player.getUniqueId(), player);
        }
    }

    public void close() {
        map.clear();
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    protected void onQ(PlayerQuitEvent event) {
        map.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    protected void onJ(PlayerJoinEvent event) {
        map.put(event.getPlayer().getUniqueId(), event.getPlayer());
    }

    public @Nullable Player getPlayer(UUID uuid) {
        return map.get(uuid);
    }

    public boolean isOnline(UUID uuid) {
        return map.containsKey(uuid);
    }
}
