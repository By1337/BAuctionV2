package dev.by1337.auc.lifecycle;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.auc.common.registry.NetworkRegistry;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.metrics.Metrics;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.registry.AucRegistries;
import dev.by1337.auc.transaction.Transaction;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.loader.MenuSubLoader;
import dev.by1337.cmd.Command;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.sync.common.util.BSUtils;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AucLifecycleImpl extends AucLifecycle {
    private final List<AucLifecycle> listeners = new CopyOnWriteArrayList<>();

    public void addListener(AucLifecycle lifecycle){
        listeners.add(lifecycle);
    }
    public void addListeners(Collection<? extends AucLifecycle> lifecycle){
        listeners.addAll(lifecycle);
    }


    @Override
    public <T> boolean doSkipTransaction(Transaction<T> transaction) {
        for (AucLifecycle listener : listeners) {
            if (Boolean.TRUE.equals(BSUtils.safe(() -> listener.doSkipTransaction(transaction)))) return true;
        }
        return false;
    }

    @Override
    public void onLoad(BAuction auction) {
        for (AucLifecycle listener : listeners) {
            BSUtils.safe(() -> listener.onLoad(auction));
        }
    }

    @Override
    public void onPreEnable(BAuction auction) {
        for (AucLifecycle listener : listeners) {
            BSUtils.safe(() -> listener.onPreEnable(auction));
        }
    }

    @Override
    public void onPostEnabled(BAuction auction) {
        for (AucLifecycle listener : listeners) {
            BSUtils.safe(() -> listener.onPostEnabled(auction));
        }
    }

    @Override
    public void onDisable(BAuction auction) {
        for (AucLifecycle listener : listeners) {
            BSUtils.safe(() -> listener.onDisable(auction));
        }
    }

    @Override
    public void metricsBoot(Metrics metrics) {
        for (AucLifecycle listener : listeners) {
            BSUtils.safe(() -> listener.metricsBoot(metrics));
        }
    }

    @Override
    public void auctionBooted(SimpleAuction auction) {
        for (AucLifecycle listener : listeners) {
            BSUtils.safe(() -> listener.auctionBooted(auction));
        }
    }

    @Override
    public void menuRegister(MenuSubLoader subLoader) {
        for (AucLifecycle listener : listeners) {
            BSUtils.safe(() -> listener.menuRegister(subLoader));
        }
    }

    @Override
    public void logRegister(NetworkRegistry<AuctionLog> r) {
        for (AucLifecycle listener : listeners) {
            BSUtils.safe(() -> listener.logRegister(r));
        }
    }

    @Override
    public Command<ExecuteContext> bootMenuCommand(Command<ExecuteContext> base) {
        for (AucLifecycle listener : listeners) {
            Command<ExecuteContext> finalBase = base;
            var v = BSUtils.safe(() -> listener.bootMenuCommand(finalBase));
            base = v == null ? base : v;
        }
        return base;
    }

    @Override
    public Command<EventContext> bootMessagesCommand(Command<EventContext> base) {
        for (AucLifecycle listener : listeners) {
            var finalBase = base;
            var v = BSUtils.safe(() -> listener.bootMessagesCommand(finalBase));
            base = v == null ? base : v;
        }
        return base;
    }

    @Override
    public Command<CommandSender> bootUserCommands(Command<CommandSender> base) {
        for (AucLifecycle listener : listeners) {
            var finalBase = base;
            var v = BSUtils.safe(() -> listener.bootUserCommands(finalBase));
            base = v == null ? base : v;
        }
        return base;
    }

    @Override
    public Command<CommandSender> bootAdminCommands(Command<CommandSender> base) {
        for (AucLifecycle listener : listeners) {
            var finalBase = base;
            var v = BSUtils.safe(() -> listener.bootAdminCommands(finalBase));
            base = v == null ? base : v;
        }
        return base;
    }

    @Override
    public void bootAucRegistries(AucRegistries registries) {
        for (AucLifecycle listener : listeners) {
            BSUtils.safe(() -> listener.bootAucRegistries(registries));
        }
    }

    @Override
    public void bootAucPipeline(LocalPipeline localPipeline) {
        for (AucLifecycle listener : listeners) {
            BSUtils.safe(() -> listener.bootAucPipeline(localPipeline));
        }
    }
}
