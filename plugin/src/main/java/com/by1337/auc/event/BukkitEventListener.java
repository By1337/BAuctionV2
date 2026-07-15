package com.by1337.auc.event;

import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class BukkitEventListener implements Listener {
    private static final Logger log = LoggerFactory.getLogger(BukkitEventListener.class);
    private final Plugin plugin;
    private final Map<Class<? extends Event>, ListEventExecutors<?>> listeners = new ConcurrentHashMap<>();

    public BukkitEventListener(Plugin plugin) {
        this.plugin = plugin;
    }
    public void close(){
        HandlerList.unregisterAll(this);
    }

    public <T extends Event> void registerListener(Class<T> t, Consumer<T> c) {
        ListEventExecutors<T> v = (ListEventExecutors<T>) listeners.computeIfAbsent(t, k -> new ListEventExecutors<>(t));
        v.list.add(c);
        plugin.getServer().getPluginManager().registerEvent(t, this, EventPriority.NORMAL,
                v,
                plugin, false
        );
    }

    private static class ListEventExecutors<T extends Event> implements EventExecutor {
        private final List<Consumer<T>> list = new CopyOnWriteArrayList<>();
        private final Class<T> type;

        private ListEventExecutors(Class<T> type) {
            this.type = type;
        }

        @Override
        public void execute(@NotNull Listener listener, @NotNull Event event) throws EventException {
            if (!type.isInstance(event)) return;
            T t = type.cast(event);
            for (Consumer<T> c : list) {
                try {
                    c.accept(t);
                } catch (Exception e) {
                    log.error("Failed to accept event", e);
                }
            }
        }
    }
}
