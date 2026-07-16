package com.by1337.auc;

import com.by1337.auc.command.SearchCommand;
import com.by1337.auc.command.SellCommand;
import com.by1337.auc.command.args.NumberArgument;
import com.by1337.auc.common.auc.log.AuctionLogBoot;
import com.by1337.auc.common.network.AucPackets;
import com.by1337.auc.config.Config;
import com.by1337.auc.eco.VaultHook;
import com.by1337.auc.event.BukkitEventListener;
import com.by1337.auc.handler.Auction;
import com.by1337.auc.handler.SimpleAuction;
import com.by1337.auc.menu.MenuBooter;
import com.by1337.auc.metrics.MetricFormatter;
import com.by1337.auc.metrics.Metrics;
import com.by1337.auc.search.filter.SearchFilterParser;
import com.by1337.auc.transaction.AddLotTransaction;
import com.by1337.auc.util.mc.PlayerList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import dev.by1337.bmenu.BMenu;
import dev.by1337.bmenu.loader.MenuSubLoader;
import dev.by1337.cmd.Command;
import dev.by1337.cmd.argument.ArgumentStrings;
import dev.by1337.core.command.bcmd.CommandWrapper;
import dev.by1337.core.command.bcmd.requires.RequiresPermission;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.yaml.YamlMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

public class BAuction extends JavaPlugin {
    private Config config;
    private SimpleAuction auction;
    private static BAuction plugin;
    private MenuSubLoader subLoader;
    private VaultHook economy;
    private CommandWrapper ah;
    private CommandWrapper aha;
    private PlayerList playerList;
    private BukkitEventListener eventListener;


    @Override
    public void onLoad() {
        AucPackets.boot();
        AuctionLogBoot.boot();
        plugin = this;
        var res = Config.DECODER.decode(ResourceUtil.load("config.yml", this).get(), this);
        config = res.result();
        if (config == null) {
            throw new RuntimeException(res.error());
        } else if (res.hasError()) {
            getSLF4JLogger().error("Failed to load cfg\n{}", res.error());
        }
        subLoader = new MenuSubLoader(new File(getDataFolder(), "menus"), this, BMenu.menuLoader());
        MenuBooter.boot(subLoader);
        BMenu.menuLoader().registerSubLoader(this, subLoader);

        File bmHome = BMenu.menuLoader().homeDir();
        ResourceUtil.saveIfNotExist("menu/vault.yml", this, new File(bmHome, "bauc/vault.yml"));
        ResourceUtil.saveIfNotExist("menu/lots_items.yml", this, new File(bmHome, "bauc/lots_items.yml"));
        ResourceUtil.saveIfNotExist("menu/home.yml", this, new File(bmHome, "bauc/home.yml"));
        ResourceUtil.saveIfNotExist("menu/confirm.yml", this, new File(bmHome, "bauc/confirm.yml"));
    }

    @Override
    public void onEnable() {
        eventListener = new BukkitEventListener(this);
        getServer().getPluginManager().registerEvents(playerList = new PlayerList(), this);
        economy = new VaultHook();
        auction = new SimpleAuction(config, this);
        Metrics.METRICS.create("loop", MetricFormatter.nanos(), () -> auction.pipeline().eventLoop().busyNanosThenReset());
        plugin.config().tags.search.forEach((k, v) -> v.boot(k));
        //new BukkitRunnable() {
        //    @Override
        //    public void run() {
        //        Metrics.METRICS.dump(getSLF4JLogger());
        //    }
        //}.runTaskTimerAsynchronously(this, 20, 20);

        ah = new CommandWrapper(new Command<CommandSender>("ah")
                .sub(new SellCommand("sell"))
                .sub(new SearchCommand("search", config.tags.search))
                .executor(s -> {
                    if (s instanceof Player pl) {
                        BMenu.menuLoader().create("bauc:home", pl, null).open();
                    }
                })
                , this);
        ah.register();
        aha = new CommandWrapper(new Command<CommandSender>("aha")
                .requires(new RequiresPermission<>("aha.use"))
                .sub(new Command<CommandSender>("push").executor(
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
                                AtomicReference<IntConsumer> ref = new AtomicReference<>();
                                long nanos = System.nanoTime();
                                ref.set(x -> {
                                    if (x <= 0) {
                                        Metrics.METRICS.dump(getSLF4JLogger());
                                        s.sendMessage("done in " + (System.nanoTime() - nanos) / 1_000_000D);
                                        return;
                                    }
                                    auction.auction().apply(new AddLotTransaction(item.asOne(), pl.getUniqueId(), price.doubleValue(), 1))
                                            .then(v -> {
                                                auction.pipeline().eventLoop().schedule(() -> ref.get().accept(x - 1));
                                            });
                                });
                                ref.get().accept(count.intValue());
                            }
                        }))
                .sub(new Command<CommandSender>("tags").executor(s -> {
                    if (s instanceof Player pl) {
                        var item = pl.getInventory().getItemInMainHand();
                        if (item.isEmpty()) {
                            s.sendMessage("Has no item un main hand!");
                            return;
                        }
                        var tags = config.tagsExtractor.extractTags(item);
                        Component c = Component.empty();
                        for (String tag : tags) {
                            c = c.append(Component.text(tag).hoverEvent(Component.text(tag)).clickEvent(ClickEvent.copyToClipboard(tag))).append(Component.text(", "));
                        }
                        pl.sendMessage(c);
                    }
                }))
                .sub(new Command<CommandSender>("tagsa").executor(s -> {
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
                            getSLF4JLogger().info(sb.toString());
                        }
                    }
                }))
                .sub(new Command<CommandSender>("filter").executor(
                        new ArgumentStrings<>("f"),
                        (s, f) -> {
                            System.out.println(SearchFilterParser.parse(f));
                        }))
                , this);
        aha.setPermission("aha.use");
        aha.register();
    }

    @Override
    public void onDisable() {
        eventListener.close();
        BMenu.menuLoader().unregisterSubLoader(this);
        ah.close();
        aha.close();
        HandlerList.unregisterAll(playerList);
    }

    public BukkitEventListener eventListener() {
        return eventListener;
    }

    public static PlayerList playerList() {
        return plugin.playerList;
    }

    public static BAuction plugin() {
        return plugin;
    }

    public Config config() {
        return config;
    }

    public static @Nullable Auction auction() {
        var v = plugin.auction;
        if (v == null) return null;
        return v.auction();
    }

    public @Nullable SimpleAuction aucManager() {
        return auction;
    }


    public static VaultHook economy() {
        return plugin.economy;
    }

    public static void sendMessage(String key, UUID player, PlaceholderResolver<EventContext> c) {
        var pl = Bukkit.getPlayer(player);
        if (pl != null) sendMessage(key, pl, c);
    }

    public static void sendMessage(String key, Player player, PlaceholderResolver<EventContext> c) {
        plugin().config.eventCtx.call(key, plugin().config.eventCtx.newContext().source(player).placeholders(c).build());
    }

    public static void sendMessage(String key, UUID player) {
        var pl = Bukkit.getPlayer(player);
        if (pl != null) sendMessage(key, pl);
    }

    public static void sendMessage(String key, Player player) {
        plugin().config.eventCtx.call(key, player);
    }

    public static class ResourceUtil {

        @NotNull
        @CanIgnoreReturnValue
        public static File saveIfNotExist(@NotNull String path, @NotNull Plugin plugin, File outFile) {
            path = path.replace('\\', '/');
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (!outFile.exists()) {
                outFile.getParentFile().mkdirs();
                try (
                        FileOutputStream fw = new FileOutputStream(outFile);
                        var in = plugin.getResource(path)
                ) {
                    in.transferTo(fw);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return outFile;
        }

        @NotNull
        @CanIgnoreReturnValue
        public static File saveIfNotExist(@NotNull String path, @NotNull Plugin plugin) {
            path = path.replace('\\', '/');
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            var f = new File(plugin.getDataFolder(), path);
            if (!f.exists()) {
                plugin.saveResource(path, false);
            }
            return f;
        }

        public static YamlMap load(@NotNull String path, @NotNull Plugin plugin) {
            return YamlMap.load(saveIfNotExist(path, plugin));
        }
    }

}