package dev.by1337.auc;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gson.JsonParser;
import dev.by1337.auc.addon.AddonLoader;
import dev.by1337.auc.assets.McLangDownloader;
import dev.by1337.auc.command.CommandBooter;
import dev.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.auc.common.auc.log.AuctionLogBoot;
import dev.by1337.auc.common.network.AucPackets;
import dev.by1337.auc.config.Config;
import dev.by1337.auc.eco.VaultHook;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.handler.remote.AuctionBackendBooter;
import dev.by1337.auc.lifecycle.AucLifecycle;
import dev.by1337.auc.lifecycle.AucLifecycleImpl;
import dev.by1337.auc.listener.BukkitEventListener;
import dev.by1337.auc.menu.MenuBooter;
import dev.by1337.auc.metrics.MetricFormatter;
import dev.by1337.auc.metrics.Metrics;
import dev.by1337.auc.papi.PlaceholderHook;
import dev.by1337.auc.util.libs.LibrariesUtil;
import dev.by1337.auc.util.luckperms.LuckPermsUtil;
import dev.by1337.auc.util.mc.PlayerList;
import dev.by1337.bmenu.BMenu;
import dev.by1337.bmenu.loader.MenuSubLoader;
import dev.by1337.cmd.Command;
import dev.by1337.core.command.bcmd.CommandWrapper;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.sync.common.util.BSUtils;
import dev.by1337.yaml.YamlMap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.UUID;

public class BAuction extends JavaPlugin {
    private static final Logger log = LoggerFactory.getLogger(BAuction.class);
    private static String latestVersion;
    private Config config;
    private SimpleAuction auction;
    private static BAuction plugin;
    private MenuSubLoader subLoader;
    private VaultHook economy;
    private CommandWrapper ah;
    private CommandWrapper aha;
    private PlayerList playerList;
    private BukkitEventListener eventListener;
    private AucLifecycleImpl lifecycle;
    private Metrics metrics;
    private BukkitTask metricsTick;
    private AuctionBackendBooter.Backend backend;
    private boolean disabled = true;
    private LuckPermsUtil luckPermsUtil;
    private PlaceholderHook papiHook;
    private AddonLoader addonLoader;

    public BAuction() {
        plugin = this;
        LibrariesUtil.boot();
    }

    @Override
    public void onLoad() {
        disabled = false;
        lifecycle = new AucLifecycleImpl();
        addonLoader = new AddonLoader(new File(getDataFolder(), "addons"), this);
        addonLoader.findAddons();
        lifecycle.addListeners(addonLoader.addons());
        AuctionLogBoot.boot();
        lifecycle.onLoad(this);
        lifecycle.logRegister(AuctionLog.REGISTRY);

        plugin = this;
        var res = Config.DECODER.decode(ResourceUtil.load("config.yml", this).get(), this, lifecycle);
        config = res.result();
        if (config == null) {
            throw new RuntimeException(res.error());
        } else if (res.hasError()) {
            getSLF4JLogger().error("Failed to load cfg\n{}", res.error());
        }
        config.tags.search.forEach((k, v) -> v.boot(k));

        subLoader = new MenuSubLoader(new File(getDataFolder(), "menus"), this, BMenu.menuLoader());
        MenuBooter.boot(subLoader, lifecycle);
        BMenu.menuLoader().registerSubLoader(this, subLoader);

        File bmHome = BMenu.menuLoader().homeDir();
        if (!new File(bmHome, "bauc").exists()) {
            ResourceUtil.saveIfNotExist("menu.txt", this);
            ResourceUtil.saveIfNotExist("menu/select_category.yml", this, new File(bmHome, "bauc/select_category.yml"));
            ResourceUtil.saveIfNotExist("menu/vault.yml", this, new File(bmHome, "bauc/vault.yml"));
            ResourceUtil.saveIfNotExist("menu/lots_items.yml", this, new File(bmHome, "bauc/lots_items.yml"));
            ResourceUtil.saveIfNotExist("menu/home.yml", this, new File(bmHome, "bauc/home.yml"));
            ResourceUtil.saveIfNotExist("menu/confirm.yml", this, new File(bmHome, "bauc/confirm.yml"));
            ResourceUtil.saveIfNotExist("menu/select_count.yml", this, new File(bmHome, "bauc/select_count.yml"));
            ResourceUtil.saveIfNotExist("menu/container_view.yml", this, new File(bmHome, "bauc/container_view.yml"));
            ResourceUtil.saveIfNotExist("menu/rent/confirm_eco.yml", this, new File(bmHome, "bauc/rent/confirm_eco.yml"));
            ResourceUtil.saveIfNotExist("menu/rent/rent.yml", this, new File(bmHome, "bauc/rent/rent.yml"));
            ResourceUtil.saveIfNotExist("menu/rent/select-currency.yml", this, new File(bmHome, "bauc/rent/select-currency.yml"));
        }
    }

    @Override
    public void onEnable() {
        if (backend != null) {
            throw new IllegalStateException("has old backend");
        }
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            luckPermsUtil = new LuckPermsUtil();
        }
        addonLoader.enableAll();
        papiHook = new PlaceholderHook();
        papiHook.register();
        lifecycle.onPreEnable(this);
        metrics = new Metrics();
        metrics.create("loop", MetricFormatter.nanos(), () -> auction.worker().busyNanosThenReset());
        metrics.create("loop-io", MetricFormatter.nanos(), () -> auction.ioWorker().busyNanosThenReset());
        lifecycle.metricsBoot(metrics);

        playerList = new PlayerList(this);
        eventListener = new BukkitEventListener(this);
        eventListener.registerListener(PlayerJoinEvent.class, this::onJoin);
        economy = new VaultHook();
        auction = new SimpleAuction(config, this, lifecycle);
        backend = auction.boot(lifecycle, this, config, () -> {
            if (disabled) {
                if (!isEnabled()) return;
                if (backend != null) {
                    BSUtils.safe(() -> backend.close());
                    backend = null;
                }
                getSLF4JLogger().info("Connected to backend server rebooting...");
                enable();
                return;
            }
            lifecycle.auctionBooted(auction);
            metricsTick = getServer().getScheduler().runTaskTimerAsynchronously(this, metrics::tick, 20, 20);
            ah = new CommandWrapper(CommandBooter.bootUserCommands(config, auction, lifecycle), this);
            ah.register();
            aha = new CommandWrapper(
                    CommandBooter.bootAdminCommands(config, auction, lifecycle)
                            .sub(new Command<CommandSender>("reload").executor(s -> {
                                long nanos = System.nanoTime();
                                disable();
                                BSUtils.safe(() -> backend.close());
                                backend = null;
                                enable();
                                BMenu.menuLoader().reload();
                                s.sendMessage("done in " + (System.nanoTime() - nanos) / 1_000_000D);
                            }))
                    , this);
            aha.setPermission("aha.use");
            aha.register();
            lifecycle.onPostEnabled(this);
        }, () -> {
            System.out.println("on backend disconnected");
            if (!isEnabled()) return;
            getSLF4JLogger().warn("backend connection lost! disable plugin");
            disable();
        });

    }
    private void onJoin(PlayerJoinEvent e){
        if (latestVersion == null) return;
        if (!e.getPlayer().hasPermission("aha.notify")) return;
        if (Objects.equals(latestVersion, getPluginMeta().getVersion())) return;
        BAuction.sendMessage("outdated_plugin", e.getPlayer(), PlaceholderResolver.of("latest", latestVersion));
    }

    public void disable() {
        onDisable();
    }

    public void enable() {
        onLoad();
        onEnable();
    }

    @Override
    public void onDisable() {
        disabled = true;
        BSUtils.safe(() -> addonLoader.disableAll());
        BSUtils.safe(() -> papiHook.unregister());
        BSUtils.safe(() -> lifecycle.onDisable(this));
        BSUtils.safe(() -> metricsTick.cancel());
        metricsTick = null;
        BSUtils.safe(() -> eventListener.close());
        eventListener = null;
        BSUtils.safe(() -> playerList.close());
        playerList = null;
        BSUtils.safe(() -> BMenu.menuLoader().unregisterSubLoader(this));
        BSUtils.safe(() -> ah.close());
        BSUtils.safe(() -> aha.close());
        BSUtils.safe(() -> auction.close());
        if (!isEnabled()) {
            System.out.println("disable backend");
            BSUtils.safe(() -> backend.close());
            backend = null;
        }
        auction = null;

        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            for (RegisteredListener listener : handlerList.getRegisteredListeners()) {
                if (listener.getPlugin() == this) {
                    log.error("registered listener {}", listener);
                }
            }
        }
        for (BukkitTask task : Bukkit.getScheduler().getPendingTasks()) {
            if (task.getOwner() == this) {
                log.error("bukkit task {}", task);
            }
        }
    }

    public LuckPermsUtil luckPermsUtil() {
        return luckPermsUtil;
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

    public Metrics metrics() {
        return metrics;
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
        var pl = plugin.playerList.getPlayer(player);
        if (pl != null) sendMessage(key, pl, c);
    }

    public static void sendMessage(String key, Player player, PlaceholderResolver<EventContext> c) {
        plugin().config.eventCtx.call(key, plugin().config.eventCtx.newContext().source(player).placeholders(c).build());
    }

    public static void sendMessage(String key, UUID player) {
        var pl = plugin.playerList.getPlayer(player);
        if (pl != null) sendMessage(key, pl);
    }

    public static void sendMessage(String key, Player player) {
        plugin().config.eventCtx.call(key, player);
    }

    static {
        //static?
        AucPackets.boot();
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


    static {
        Thread.startVirtualThread(() -> {
           try {
               var json = McLangDownloader.parsePage("https://apiv1.bdev.space/version?id=bauctionv2");
               //{"status":"ok","version":"1.0"}
               var v = JsonParser.parseString(json);
               if (v.isJsonObject()){
                   var obj = v.getAsJsonObject();
                   if (obj.has("version")){
                       latestVersion = obj.get("version").getAsString();
                   }
               }
           } catch (Exception ignored){
           }
        });
    }

}