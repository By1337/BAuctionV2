/*
package com.by1337.auc.handler.item;

import com.by1337.auc.pipeline.LocalChannelContext;
import com.by1337.auc.pipeline.LocalChannelHandler;
import com.by1337.auc.pipeline.LocalPipeline;
import com.by1337.auc.pipeline.Remote;
import dev.by1337.sync.common.channel.ChannelMessage;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class ItemGiverService implements LocalChannelHandler, Listener {
    private LocalPipeline pipeline;
    private Remote remote;
    private ItemStackRepository itemService;
    private final Plugin plugin;

    public ItemGiverService(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void init(LocalPipeline pipeline, Remote remote) {
        this.pipeline = pipeline;
        this.remote = remote;
        itemService = pipeline.get(ItemStackRepository.class);
    }



    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {

    }

    @Override
    public void close() {
        HandlerList.unregisterAll(this);
    }
}
*/
