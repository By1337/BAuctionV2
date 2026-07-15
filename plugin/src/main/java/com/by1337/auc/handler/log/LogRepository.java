package com.by1337.auc.handler.log;

import com.by1337.auc.common.auc.log.AuctionLog;
import com.by1337.auc.common.network.c2s.C2SLoadLogsRequest;
import com.by1337.auc.common.network.c2s.C2SPublishLog;
import com.by1337.auc.common.network.s2c.S2CLogAdded;
import com.by1337.auc.handler.SimpleAuction;
import com.by1337.auc.pipeline.LocalChannelContext;
import com.by1337.auc.pipeline.LocalChannelHandler;
import com.by1337.auc.pipeline.LocalPipeline;
import com.by1337.auc.pipeline.Remote;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class LogRepository implements LocalChannelHandler {
    private static final Logger log = LoggerFactory.getLogger(LogRepository.class);
    private LocalPipeline pipeline;
    private Remote remote;
    private SimpleAuction auction;
    private CopyOnWriteArrayList<Consumer<AuctionLog>> logListeners = new CopyOnWriteArrayList<>();
    private final ConcurrentSkipListSet<AuctionLog> logs = new ConcurrentSkipListSet<>((o1, o2) -> {
        var v = -Long.compare(o1.timestamp(), o2.timestamp());
        if (v == 0) return Integer.compare(o1.hashCode(), o2.hashCode());
        return v;
    });

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.pipeline = pipeline;
        this.remote = remote;
        this.auction = auction;
    }

    public void publishLog(AuctionLog log) {
        remote.write(new C2SPublishLog(log));
    }

    public ResponseFuture<@Nullable List<AuctionLog>> loadLogs(long after, @Nullable UUID actor, @Nullable UUID subject, @Nullable String type, int limit) {
        var oldestCachedLog = logs.isEmpty() ? null : logs.getLast();
        if (oldestCachedLog != null && oldestCachedLog.timestamp() <= after) {
            List<AuctionLog> result = new ArrayList<>();
            int limit1 = limit != -1 ? limit : Integer.MAX_VALUE;
            for (AuctionLog log : logs) {
                if (log.timestamp() < after) break;
                if (actor != null && !Objects.equals(actor, log.actor())) continue;
                if (subject != null && !Objects.equals(subject, log.subject())) continue;
                if (type != null && !Objects.equals(type, log.type())) continue;
                result.add(log);
                if (result.size() >= limit1) {
                    break;
                }
            }
            return new ResponseFuture<>(result);
        }
        return remote.request(new C2SLoadLogsRequest(after, actor, subject, type, limit)).map(result -> {
            logs.addAll(result.logs());
            return result.logs();
        });
    }


    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof S2CLogAdded(AuctionLog event)) {
            logs.add(event);
            if (logs.size() >= 5_000) { //мб красивее хз
                logs.pollLast();
            }
            for (Consumer<AuctionLog> listener : logListeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    log.error("Failed to accept log event {}", event, e);
                }
            }
        } else {
            ctx.fire(msg);
        }
    }

    public void registerLogListener(Consumer<AuctionLog> c) {
        logListeners.add(c);
    }

    @Override
    public void close() {
    }
}
