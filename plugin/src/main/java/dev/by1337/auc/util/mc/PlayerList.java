package dev.by1337.auc.util.mc;

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

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerList implements Listener {
    private final Map<UUID, Player> uuid2player = new ConcurrentHashMap<>();
    private final Map<String, Player> name2player = new ConcurrentHashMap<>();

    public PlayerList(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            uuid2player.put(player.getUniqueId(), player);
            name2player.put(player.getName().toLowerCase(Locale.ROOT), player);
        }
    }

    public void close() {
        uuid2player.clear();
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    protected void onQ(PlayerQuitEvent event) {
        uuid2player.remove(event.getPlayer().getUniqueId());
        name2player.remove(event.getPlayer().getName().toLowerCase(Locale.ROOT));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    protected void onJ(PlayerJoinEvent event) {
        uuid2player.put(event.getPlayer().getUniqueId(), event.getPlayer());
        name2player.put(event.getPlayer().getName().toLowerCase(Locale.ROOT), event.getPlayer());
    }

    public @Nullable Player getPlayer(String name) {
        return name2player.get(name.toLowerCase(Locale.ROOT));
    }
    public String tryFixName(String name){
        var pl = getPlayer(name);
        if (pl != null) return pl.getName();
        return name;
    }

    public @Nullable Player getPlayer(UUID uuid) {
        return uuid2player.get(uuid);
    }

    public boolean isOnline(UUID uuid) {
        return uuid2player.containsKey(uuid);
    }
}
