package com.by1337.auc.util.mc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerList implements Listener {
    private final Map<UUID, Player> map = new ConcurrentHashMap<>();

    @EventHandler
    public void onQ(PlayerQuitEvent event){
        map.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onJ(PlayerJoinEvent event){
        map.put(event.getPlayer().getUniqueId(), event.getPlayer());
    }
    public @Nullable Player getPlayer(UUID uuid){
        return map.get(uuid);
    }

    public boolean isOnline(UUID uuid){
        return map.containsKey(uuid);
    }
}
