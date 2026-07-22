package dev.by1337.auc.handler.name;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.common.network.a2a.A2ASetPlayerNamePacket;
import dev.by1337.auc.common.network.c2s.C2SPlayerNameRequest;
import dev.by1337.auc.common.network.c2s.C2SPlayerUUIDRequest;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.handler.event.PlayerChangeNameEvent;
import dev.by1337.auc.pipeline.LocalChannelContext;
import dev.by1337.auc.pipeline.LocalChannelHandler;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.pipeline.Remote;
import dev.by1337.core.util.misc.Pair;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlayerNameService implements LocalChannelHandler {
    private LocalPipeline pipeline;
    private Remote remote;
    private final Plugin plugin;

    private final Object2ObjectOpenHashMap<UUID, PlayerName> uuid2name = new Object2ObjectOpenHashMap<>();

    public PlayerNameService(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.pipeline = pipeline;
        this.remote = remote;
        BAuction.plugin().eventListener().registerListener(PlayerJoinEvent.class, this::onJoin);
    }

    public ResponseFuture<@Nullable Pair<UUID, String>> findUUID(String name) {
        return remote.request(new C2SPlayerUUIDRequest(name)).map(v -> Pair.of(v.uuid(), v.name()));
    }

    public ResponseFuture<@NotNull PlayerName> loadName(UUID uuid) {
        return pipeline.submit(() -> loadName0(uuid));
    }

    private ResponseFuture<@NotNull PlayerName> loadName0(UUID uuid) {
        var name = uuid2name.get(uuid);
        if (name != null) return new ResponseFuture<>(name);
        Player player = BAuction.playerList().getPlayer(uuid);
        if (player != null) {
            var res = new PlayerName(player.getName());
            uuid2name.put(uuid, res);
            remote.write(new A2ASetPlayerNamePacket(uuid, res.name()));
            return new ResponseFuture<>(res);
        }
        return remote.request(new C2SPlayerNameRequest(uuid))
                .map(v -> new PlayerName(v.name())).orElse(() -> new PlayerName("NoName"))
                .then(v -> {
                    uuid2name.putIfAbsent(uuid, v);
                });
    }


    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof A2ASetPlayerNamePacket(UUID uuid, String name)) {
            var n = uuid2name.computeIfAbsent(uuid, k -> new PlayerName("$empty"));
            var old = n.name();
            if (n.setName(name) && !old.equals("$empty")) {
                pipeline.execute(new PlayerChangeNameEvent(uuid, name, old));
            }
        } else {
            ctx.fire(msg);
        }
    }

    public void setName(UUID uuid, String name) {
        pipeline.eventLoop().execute(() -> {
            var n = uuid2name.computeIfAbsent(uuid, k -> new PlayerName("$empty"));
            var old = n.name();
            if (n.setName(name)) {
                remote.write(new A2ASetPlayerNamePacket(uuid, name));
                if (!old.equals("$empty")) {
                    pipeline.execute(new PlayerChangeNameEvent(uuid, name, old));
                }
            }
        });
    }

    private void onJoin(PlayerJoinEvent event) {
        String name = event.getPlayer().getName();
        UUID uuid = event.getPlayer().getUniqueId();
        setName(uuid, name);
    }

    @Override
    public void close() {
    }
}
