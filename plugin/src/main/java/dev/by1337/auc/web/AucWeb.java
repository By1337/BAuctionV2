package dev.by1337.auc.web;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.auc.common.auc.log.LogQuery;
import dev.by1337.auc.common.auc.log.LogRecord;
import dev.by1337.auc.common.auc.log.impl.WithItemStackLog;
import dev.by1337.auc.common.auc.log.impl.WithLPriceLog;
import dev.by1337.auc.config.WebConfig;
import dev.by1337.core.util.misc.Pair;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.web.client.RequestRouter;
import dev.by1337.web.client.WebEndpoint;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AucWeb {
    private final WebConfig webConfig;
    private final WebEndpoint webEndpoint;

    public AucWeb(WebConfig webConfig) {
        this.webConfig = webConfig;
        webEndpoint = new WebEndpoint(
                createRouter(),
                false,
                webConfig.url,
                webConfig.staticContent,
                webConfig.secret,
                webConfig.description
        );
    }

    public CompletableFuture<WebEndpoint.@Nullable Connection> getConnection() {
        return webEndpoint.connect();
    }

    public void close() {
        webEndpoint.close();
    }

    private RequestRouter createRouter() {
        return new RequestRouter().route("/find_logs", h -> {
            var params = h.params();
            resolveUUID(params.getString("actor")).then(actor -> resolveUUID(params.getString("subject")).then(subject -> {
                LogQuery query = new LogQuery(
                        params.getLong("afterId", null),
                        params.getLong("beforeId", null),
                        params.getLong("afterTimestamp", null),
                        params.getLong("beforeTimestamp", null),
                        actor,
                        subject,
                        params.getString("type", null),
                        100
                );
                BAuction.auction().log().loadLogs(query).then(list -> {
                    var writer = h.writer();
                    List<LogRecord> l = list == null ? List.of() : list;
                    try (var a = writer.startArray(null)) {
                        for (LogRecord log : l) {
                            try (var o = writer.startObject(null)) {
                                writer.putLong("uid", log.uid());
                                writer.putString("type", log.type());
                                writer.putLong("timestamp", log.timestamp());
                                writer.putString("actor", log.actor().toString());
                                var v = log.subject();
                                if (v != null) {
                                    writer.putString("subject", v.toString());
                                }
                                if (log.log() instanceof WithItemStackLog item) {
                                    writer.putInt("item", item.item());
                                    writer.putInt("count", item.count());
                                }
                                if (log.log() instanceof WithLPriceLog item) {
                                    writer.putLong("cents", item.lprice());
                                }
                            }
                        }
                    }
                    h.send();
                });
            }));
        }).route("/logs_types", h -> {
            // /logs_types -> ["type:a", "type:b"]
            try (var a = h.writer().startArray(null)) {
                for (String type : AuctionLog.REGISTRY.types()) {
                    h.writer().putString(null, type);
                }
            }
            h.send();
        }).route("/find_nickname", h -> {
            //
            var uuids = h.params().getStringList("uuid");
            if (uuids == null || uuids.isEmpty()) {
                h.writer().array(null, v -> {});
                h.send();
            } else {
                BAuction.auction().parallelMap(
                        uuids.stream().map(AucWeb::parseUUID).filter(Objects::nonNull).iterator(),
                        uuid -> BAuction.auction().loadName(uuid),
                        Pair::of
                ).then(list -> {
                    h.writer().array(null, v -> {
                        for (var pair : list) {
                            v.object(null, o -> {
                                o.putString("uuid", pair.getKey().toString());
                                o.putString("name", pair.getValue().name());
                            });
                        }
                    });
                    h.send();
                });
            }
        });
    }

    private static ResponseFuture<@Nullable UUID> resolveUUID(String input) {
        ResponseFuture<@Nullable UUID> f = new ResponseFuture<>();
        resolveUUID(input, f::complete);
        return f;
    }

    private static void resolveUUID(String input, Consumer<@Nullable UUID> c) {
        if (input == null) {
            c.accept(null);
            return;
        }
        var pl = Bukkit.getPlayerExact(input);
        if (pl != null) {
            c.accept(pl.getUniqueId());
        } else if (input.length() > 16) {
            try {
                c.accept(UUID.fromString(input));
            } catch (Exception e1) {
                try {
                    c.accept(parseMysqlUuid(input));
                    return;
                } catch (Exception e2) {
                    c.accept(null);
                }
            }
        } else {
            BAuction.auction().findUUID(input).then(p -> {
                if (p == null) c.accept(null);
                else c.accept(p.getLeft());
            });
        }
    }

    private static @Nullable UUID parseUUID(String input) {
        if (input == null) return null;
        try {
            return UUID.fromString(input);
        } catch (Exception e1) {
            try {
                return parseMysqlUuid(input);
            } catch (Exception e2) {
            }
        }
        return null;
    }

    static UUID parseMysqlUuid(String value) {
        if (value.startsWith("0x")) {
            value = value.substring(2);
        }

        if (value.length() != 32) {
            throw new IllegalArgumentException("Invalid UUID hex: " + value);
        }

        long most = Long.parseUnsignedLong(value.substring(0, 16), 16);
        long least = Long.parseUnsignedLong(value.substring(16, 32), 16);

        return new UUID(most, least);
    }
}
