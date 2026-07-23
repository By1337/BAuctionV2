package dev.by1337.auc.hook;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.lifecycle.AucLifecycle;
import dev.by1337.auc.util.number.DurationParser;
import dev.by1337.cmd.Command;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PostJoinUseDelay extends AucLifecycle {
    public static final YamlDecoder<PostJoinUseDelay> DECODER = RecordYamlDecoder.mapOf(
            PostJoinUseDelay::new,
            YamlDecoder.BOOL.fieldOf("enable", false),
            DurationParser.DECODER.fieldOf("delay", 10_000L)
    );
    private final Object2LongOpenHashMap<UUID> map = new Object2LongOpenHashMap<>();
    private final boolean enable;
    private final long delay;

    public PostJoinUseDelay(boolean enable, long delay) {
        this.enable = enable;
        this.delay = delay;
    }

    public boolean isEnable() {
        return enable;
    }

    @Override
    public Command<CommandSender> bootUserCommands(Command<CommandSender> base) {
        if (!enable) return base;
        base.getSubCommands().values().forEach(v -> v.requires(s -> {
            if (!(s instanceof Player player)) return true;
            if (player.hasPermission("aha.use")) return true;
            long timestamp = map.getLong(player.getUniqueId());
            if (timestamp == 0) return true;
            long l = timestamp + delay - System.currentTimeMillis();
            return l <= 0;
        }));
        final var original = base.getExecutor();
        base.executor((s, a) -> {
            if (s instanceof Player player && !player.hasPermission("aha.use")) {
                long timestamp = map.getLong(player.getUniqueId());
                if (timestamp != 0) {
                    long l = timestamp + delay - System.currentTimeMillis();
                    if (l > 0) {
                        BAuction.sendMessage("post_join_use_delay", player, PlaceholderResolver.of("time", l / 1000));
                        return;
                    }
                }
            }
            if (original != null)
                original.execute(s, a);
        });
        return base;
    }

    private void onJoin(PlayerJoinEvent e) {
        map.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    private void onQuit(PlayerQuitEvent e) {
        map.removeLong(e.getPlayer().getUniqueId());
    }

    @Override
    public void onPostEnabled(BAuction auction) {
        if (!enable) return;
        auction.eventListener().registerListener(PlayerJoinEvent.class, this::onJoin);
        auction.eventListener().registerListener(PlayerQuitEvent.class, this::onQuit);
    }
}
