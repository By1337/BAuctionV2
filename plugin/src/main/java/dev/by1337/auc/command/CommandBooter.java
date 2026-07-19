package dev.by1337.auc.command;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.GhostLot;
import dev.by1337.auc.command.args.NumberArgument;
import dev.by1337.auc.config.Config;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.lifecycle.AucLifecycle;
import dev.by1337.auc.menu.HomeMenu;
import dev.by1337.auc.search.filter.SearchFilter;
import dev.by1337.auc.search.filter.SearchFilterParser;
import dev.by1337.auc.transaction.AddLotTransaction;
import dev.by1337.bmenu.BMenu;
import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.core.command.bcmd.requires.RequiresPermission;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.packet.ExpectsResponse;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public class CommandBooter {

    private static final Logger log = LoggerFactory.getLogger(CommandBooter.class);

    public static Command<CommandSender> bootUserCommands(Config config, SimpleAuction auction, AucLifecycle lifecycle) {
        var cfg = config.commands;
        var cmd = lifecycle.bootUserCommands(new Command<CommandSender>(cfg.rename("ah")));
        if (cfg.commands.contains("sell")) {
            cmd.sub(new SellCommand(cfg.rename("sell")));
        }
        if (cfg.commands.contains("search")) {
            cmd.sub(new SearchCommand(cfg.rename("search"), config.tags.search));
        }
        cmd.executor(s -> {
            if (s instanceof Player pl) {
                BMenu.menuLoader().create(cfg.ah_menu, pl, null).open();
            }
        });
        return cmd;
    }

    public static Command<CommandSender> bootAdminCommands(Config config, SimpleAuction auction, AucLifecycle lifecycle) {
        var cfg = config.commands;
        var cmd = lifecycle.bootAdminCommands(new Command<CommandSender>(cfg.rename("aha")));
        cmd.requires(new RequiresPermission<>("aha.use"));
        cmd.sub(new Command<CommandSender>("push").executor(
                new NumberArgument<>("price"),
                new NumberArgument<>("count"),
                (s, price, count) -> {
                    if (price == null || count == null) {
                        s.sendMessage("use /aha push <price> <count>");
                        return;
                    }
                    if (s instanceof Player pl) {
                        var item = pl.getInventory().getItemInMainHand();
                        if (item.isEmpty()) {
                            s.sendMessage("Has no item un main hand!");
                            return;
                        }
                        long nanos = System.nanoTime();
                        SimpleAuction.WORKER.execute(() -> {
                            parallel(
                                    new AtomicInteger(count.intValue()),
                                    () -> {
                                        BAuction.plugin().metrics().dump(log);
                                        s.sendMessage("done in " + (System.nanoTime() - nanos) / 1_000_000D);
                                    },
                                    () -> auction.auction().apply(new AddLotTransaction(item.asOne(), pl.getUniqueId(), price.doubleValue(), 1)),
                                    ignored -> {}
                            );
                        });
                    }
                }));        cmd.sub(new Command<CommandSender>("push_rand").executor(
                new NumberArgument<>("price"),
                new NumberArgument<>("count"),
                (s, price, count) -> {
                    if (price == null || count == null) {
                        s.sendMessage("use /aha push_rand <price> <count>");
                        return;
                    }
                    Random random = new Random();
                    List<Material> materials = Registry.MATERIAL.stream().filter(i -> !i.isAir() && i.isItem()).toList();
                    long nanos = System.nanoTime();
                    SimpleAuction.WORKER.execute(() -> {
                        parallel(
                                new AtomicInteger(count.intValue()),
                                () -> {
                                    BAuction.plugin().metrics().dump(log);
                                    s.sendMessage("done in " + (System.nanoTime() - nanos) / 1_000_000D);
                                },
                                () -> {
                                    ItemStack item = new ItemStack(materials.get(random.nextInt(materials.size()-1)));
                                    return auction.auction().apply(new AddLotTransaction(item.asOne(), new UUID(1337, random.nextLong()), price.doubleValue() + random.nextInt(0, 250), 1));
                                },
                                ignored -> {}
                        );
                    });
                }));        cmd.sub(new Command<CommandSender>("tags").executor(s -> {
            if (s instanceof Player pl) {
                var item = pl.getInventory().getItemInMainHand();
                if (item.isEmpty()) {
                    s.sendMessage("Has no item in main hand!");
                    return;
                }
                var tags = config.tagsExtractor.extractTags(item);
                Component c = Component.empty();
                for (String tag : tags) {
                    c = c.append(Component.text(tag).hoverEvent(Component.text(tag)).clickEvent(ClickEvent.copyToClipboard(tag))).append(Component.text(", "));
                }
                pl.sendMessage(c);
            }
        }));        cmd.sub(new Command<CommandSender>("tagsa").executor(s -> {
            if (s instanceof Player pl) {
                for (ItemStack itemStack : pl.getInventory()) {
                    if (itemStack == null || itemStack.isEmpty()) continue;
                    var tags = config.tagsExtractor.extractTags(itemStack);
                    StringBuilder sb = new StringBuilder(itemStack.getType().getKey().asString()).append(": ");
                    Component c = Component.empty();
                    for (String tag : tags) {
                        sb.append(tag).append(", ");
                        c = c.append(Component.text(tag).hoverEvent(Component.text(tag)).clickEvent(ClickEvent.copyToClipboard(tag))).append(Component.text(", "));
                    }
                    pl.sendMessage(c);
                    log.info(sb.toString());
                }
            }
        }));        cmd.sub(new Command<CommandSender>("filter").executor(
                new ArgumentStrings<>("f"),
                (s, f) -> {
                    SearchFilter filter = SearchFilterParser.parse(f);
                    log.info(filter.toString());
                    s.sendMessage(filter.toString());
                    if (s instanceof Player player){
                        var menu = BMenu.menuLoader().create(cfg.ah_menu, player, null);
                        if (menu instanceof HomeMenu h){
                            h.setSearchInput(f);
                            h.setSearch(filter);
                        }
                        menu.open();
                    }
                }));        cmd.sub(new Command<CommandSender>("metrics").executor((s) -> {
            BAuction.plugin().metrics().dump(log);
            s.sendMessage(BAuction.plugin().metrics().dump());
        }));
        return cmd;
    }

    private static <T> void parallel(
            AtomicInteger remaining,
            Runnable done,
            Supplier<ResponseFuture<T>> maker,
            Consumer<@Nullable T> then
    ) {
        SimpleAuction.WORKER.assertThread();

        if (remaining.get() <= 0) {
            done.run();
            return;
        }

        AtomicReference<Runnable> request = new AtomicReference<>();
        request.set(() -> {
            if (remaining.decrementAndGet() < 0) {
                return;
            }

            maker.get().then(v -> {
                then.accept(v);

                SimpleAuction.WORKER.assertThread();

                if (remaining.get() > 0) {
                    SimpleAuction.WORKER.schedule(request.get());
                } else {
                    done.run();
                }
            });
        });

        int initial = Math.min(remaining.get(), 256);
        while (initial-- > 0) {
            request.get().run();
        }
    }
}
