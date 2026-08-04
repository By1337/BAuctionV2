package dev.by1337.auc.command;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.command.args.ArgumentNumber;
import dev.by1337.auc.config.Config;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.lifecycle.AucLifecycle;
import dev.by1337.auc.menu.HomeMenu;
import dev.by1337.auc.menu.SellGuiMenu;
import dev.by1337.auc.search.filter.SearchFilter;
import dev.by1337.auc.search.filter.SearchFilterParser;
import dev.by1337.auc.transaction.AddLotTransaction;
import dev.by1337.auc.transaction.ResellTransaction;
import dev.by1337.bmenu.BMenu;
import dev.by1337.cmd.*;
import dev.by1337.cmd.argument.ArgumentString;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.core.BCore;
import dev.by1337.core.command.bcmd.requires.RequiresPermission;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.yaml.BukkitCodecs;
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

import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CommandBooter {

    private static final Logger log = LoggerFactory.getLogger(CommandBooter.class);

    public static Command<CommandSender> bootUserCommands(Config config, SimpleAuction auction, AucLifecycle lifecycle) {
        var cfg = config.commands;
        var cmd = new Command<CommandSender>(cfg.rename("ah"));
        if (!cfg.disabled_commands.contains("sell")) {
            cmd.sub(new SellCommand(cfg.rename("sell"), false));
        }
        if (!cfg.disabled_commands.contains("dsell")) {
            cmd.sub(new SellCommand(cfg.rename("dsell"), true));
        }
        if (!cfg.disabled_commands.contains("search")) {
            cmd.sub(new SearchCommand(cfg.rename("search"), config.tags.search, config));
        }
        if (!cfg.disabled_commands.contains("help")) {
            cmd.sub(new Command<CommandSender>(cfg.rename("help")).executor(s -> {
                if (s instanceof Player player) {
                    BAuction.sendMessage("ah_help", player);
                }
            }));
        }
        if (!cfg.disabled_commands.contains("resell")) {
            cmd.sub(new Command<CommandSender>(cfg.rename("resell")).executor(s -> {
                if (s instanceof Player player) {
                    BAuction.auction().apply(new ResellTransaction(player.getUniqueId()));
                }
            }));
        }
        if (!cfg.disabled_commands.contains("rent")) {
            cmd.sub(new Command<CommandSender>(cfg.rename("rent")).executor(s -> {
                if (s instanceof Player player) {
                    BMenu.menuLoader().create("auc_rent:home", player, null).open();
                }
            }));
        }
        if (!cfg.disabled_commands.contains("sellgui")) {
            cmd.sub(new Command<CommandSender>(cfg.rename("sellgui")).executor(
                    new ArgumentNumber<>("price"),
                    (s, price) -> {
                        if (s instanceof Player player) {
                            if (price == null) {
                                BAuction.sendMessage("sell_req_price", player);
                                return;
                            }
                            var menu = BMenu.menuLoader().create("bauc:sell_gui", player, null);
                            if (menu instanceof SellGuiMenu sellGuiMenu) {
                                sellGuiMenu.setPrice(price.doubleValue());
                            }
                            menu.open();
                        }
                    }));
        }
        cmd.executor(
                new ArgumentString<>("name") {
                    @Override
                    public void suggest(CommandSender ctx, CommandReader reader, SuggestionsList suggestions, ArgumentMap args) throws CommandMsgError {
                        var input = reader.readString().toLowerCase();
                        if (input.isEmpty()) {
                            suggestions.suggest("{player}");
                            return;
                        }
                        var auc = BAuction.auction();
                        if (auc == null) return;
                        var playerList = BAuction.playerList();
                        var iter = auc.normalPlayerNamesIterator();
                        while (iter.hasNext() && suggestions.hasFree()) {
                            var name = iter.next();
                            if (name.startsWith(input)) {
                                suggestions.suggest(playerList.tryFixName(name));
                            }
                        }
                    }
                },
                (s, name) -> {
                    var auc = BAuction.auction();
                    if (auc == null) return;
                    if (s instanceof Player pl) {
                        var menu = BMenu.menuLoader().create(cfg.ah_menu, pl, null);
                        if (menu instanceof HomeMenu h) {
                            if (name != null) {
                                h.setSearchInput(BAuction.playerList().tryFixName(name));
                                var uuid = auc.name2uuid(name.toLowerCase());
                                if (uuid != null) {
                                    h.setPlayerLots(uuid);
                                } else {
                                    h.setNop(true);
                                }
                            }
                        }
                        menu.open();
                    }
                });
        return lifecycle.bootUserCommands(cmd);
    }

    public static Command<CommandSender> bootAdminCommands(Config config, SimpleAuction auction, AucLifecycle lifecycle) {
        var cfg = config.commands;
        var cmd = new Command<CommandSender>(cfg.rename("aha"));
        cmd.requires(new RequiresPermission<>("aha.use"));
        cmd.sub(new Command<CommandSender>("push").executor(
                new ArgumentNumber<>("price"),
                new ArgumentNumber<>("count"),
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
                                    () -> auction.auction().apply(new AddLotTransaction(item.asOne(), pl.getUniqueId(), price.doubleValue(), 1)
                                            .skipSlotsCheck()
                                            .skipPriceChecks()
                                    ),
                                    ignored -> {}
                            );
                        });
                    }
                }));
        cmd.sub(new Command<CommandSender>("push_rand").executor(
                new ArgumentNumber<>("price"),
                new ArgumentNumber<>("count"),
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
                                    ItemStack item = new ItemStack(materials.get(random.nextInt(materials.size() - 1)));
                                    return auction.auction().apply(new AddLotTransaction(item.asOne(), new UUID(1337, random.nextLong()), price.doubleValue() + random.nextInt(0, 250), 1)
                                            .skipSlotsCheck()
                                            .skipPriceChecks()
                                    );
                                },
                                ignored -> {}
                        );
                    });
                }));
        cmd.sub(new Command<CommandSender>("tags").executor(s -> {
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
        }));
        cmd.sub(new Command<CommandSender>("tagsa").executor(s -> {
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
        }));
        cmd.sub(new Command<CommandSender>("filter").executor(
                new ArgumentStrings<>("f"),
                (s, f) -> {
                    SearchFilter filter = SearchFilterParser.parse(f);
                    log.info(filter.toString());
                    s.sendMessage(filter.toString());
                    if (s instanceof Player player) {
                        var menu = BMenu.menuLoader().create(cfg.ah_menu, player, null);
                        if (menu instanceof HomeMenu h) {
                            h.setSearchInput(f);
                            h.setSearch(filter);
                        }
                        menu.open();
                    }
                }));
        cmd.sub(new Command<CommandSender>("metrics").executor((s) -> {
            BAuction.plugin().metrics().dump(log);
            s.sendMessage(BAuction.plugin().metrics().dump());
        }));
        cmd.sub(new Command<CommandSender>("user").executor((s) -> {
            if (s instanceof Player pl)
                s.sendMessage(String.valueOf(BAuction.plugin().aucManager().users().getUser(pl.getUniqueId())));
        }));
        cmd.sub(new Command<CommandSender>("get_uuid").executor(
                new ArgumentStrings<>("name"),
                (s, name) -> {
                    if (name == null) {
                        s.sendMessage("use /aha get_uuid <name>");
                        return;
                    }
                    BAuction.auction().findUUID(name).then(p -> {
                        if (p == null) {
                            s.sendMessage("null");
                        } else {
                            s.sendMessage(p.getKey() + " " + p.getRight());
                        }
                    });
                }));

        cmd.sub(new Command<CommandSender>("base64").executor((s) -> {
            if (s instanceof Player pl) {
                var item = pl.getInventory().getItemInMainHand();
                if (item.isEmpty()) {
                    s.sendMessage("Has no item in main hand!");
                    return;
                }
                var v = Base64.getEncoder().encodeToString(BCore.getItemStackSerializer().serialize(item, pl.getWorld()));
                s.sendMessage(Component.text(v).hoverEvent(Component.text("copy")).clickEvent(ClickEvent.copyToClipboard(v)));
            }
        }));
        cmd.sub(new Command<CommandSender>("materials_by_filter").executor(
                new ArgumentStrings<>("f"),
                (s, f) -> {
                    var v = BukkitCodecs.material().wildcard().decode(f).getOrThrow();
                    StringBuilder sb = new StringBuilder();
                    for (Material material : v) {
                        sb.append(material.key().value()).append(", ");
                    }
                    if (sb.isEmpty()){
                        s.sendMessage("No material found!");
                        return;
                    }
                    sb.setLength(sb.length() - 2);
                    var res = sb.toString();
                    s.sendMessage(Component.text(res).hoverEvent(Component.text("copy")).clickEvent(ClickEvent.copyToClipboard(res)));
                }));
        return lifecycle.bootAdminCommands(cmd);
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
            var x = remaining.decrementAndGet();
            if (x == 0) {
                done.run();
                return;
            }
            if (x < 0) return;
            maker.get().then(v -> {
                SimpleAuction.WORKER.assertThread();
                then.accept(v);
                SimpleAuction.WORKER.schedule(request.get());
            });
        });

        int initial = Math.min(remaining.get(), 256);
        while (initial-- > 0) {
            request.get().run();
        }
    }
}
