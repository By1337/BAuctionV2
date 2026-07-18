package dev.by1337.auc.handler.log;

import dev.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.auc.common.auc.log.LogQuery;
import dev.by1337.auc.common.auc.log.LogRecord;
import dev.by1337.auc.common.network.a2a.A2ALongResponse;
import dev.by1337.auc.common.network.c2s.C2SGetLogRecordRequest;
import dev.by1337.auc.common.network.c2s.C2SLoadLogsRequest;
import dev.by1337.auc.common.network.c2s.C2SPublishLog;
import dev.by1337.auc.common.network.s2c.S2CLogAdded;
import dev.by1337.auc.common.network.s2c.S2COptionalLogRecord;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.pipeline.LocalChannelContext;
import dev.by1337.auc.pipeline.LocalChannelHandler;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.pipeline.Remote;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class LogRepository implements LocalChannelHandler {
    private static final Logger log = LoggerFactory.getLogger(LogRepository.class);
    private LocalPipeline pipeline;
    private Remote remote;
    private SimpleAuction auction;
    private CopyOnWriteArrayList<Consumer<LogRecord>> logListeners = new CopyOnWriteArrayList<>();

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.pipeline = pipeline;
        this.remote = remote;
        this.auction = auction;
    }

    public ResponseFuture<@Nullable LogRecord> getLog(long uid) {
        return remote.request(new C2SGetLogRecordRequest(uid)).map(S2COptionalLogRecord::log);
    }

    public ResponseFuture<@Nullable Long> publishLog(AuctionLog log) {
        return remote.request(new C2SPublishLog(log)).map(A2ALongResponse::uid);
    }

    public ResponseFuture<@Nullable List<LogRecord>> loadLogs(LogQuery query) {
        return remote.request(new C2SLoadLogsRequest(query)).map(result -> {
            return result.logs();
        });
    }


    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof S2CLogAdded(LogRecord event)) {
            for (Consumer<LogRecord> listener : logListeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    log.error("Failed to accept lot event {}", event, e);
                }
            }
        } else {
            ctx.fire(msg);
        }
    }

    public void registerLogListener(Consumer<LogRecord> c) {
        logListeners.add(c);
    }

    @Override
    public void close() {
    }
}
