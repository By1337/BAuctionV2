package dev.by1337.auc.lifecycle;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.auc.common.registry.NetworkRegistry;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.metrics.Metrics;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.registry.AucRegistries;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.loader.MenuSubLoader;
import dev.by1337.cmd.Command;
import dev.by1337.edsl.context.EventContext;
import org.bukkit.command.CommandSender;

public class AucLifecycle {
    public void onLoad(BAuction auction){

    }
    public void onPreEnable(BAuction auction){

    }
    public void onPostEnabled(BAuction auction){

    }
    public void onDisable(BAuction auction){

    }

    public void metricsBoot(Metrics metrics){

    }

    public void auctionBooted(SimpleAuction auction){

    }

    public Command<ExecuteContext> bootMenuCommand(Command<ExecuteContext> base) {
        return base;
    }

    public void menuRegister(MenuSubLoader subLoader) {
    }

    public void logRegister(NetworkRegistry<AuctionLog> r) {

    }

    public Command<EventContext> bootMessagesCommand(Command<EventContext> base) {
        return base;
    }
    public Command<CommandSender> bootUserCommands(Command<CommandSender> base){
        return base;
    }
    public Command<CommandSender> bootAdminCommands(Command<CommandSender> base){
        return base;
    }
    public void bootAucRegistries(AucRegistries registries){
    }
    public void bootAucPipeline(LocalPipeline localPipeline){

    }
}
