package com.by1337.auc.common.backend.log;

import com.by1337.auc.common.auc.log.LogRecord;
import com.by1337.auc.common.handler.BAucRuntime;
import com.by1337.auc.common.handler.GetPostChannelHandler;
import com.by1337.auc.common.network.a2a.A2ALongResponse;
import com.by1337.auc.common.network.c2s.C2SGetLogRecordRequest;
import com.by1337.auc.common.network.c2s.C2SLoadLogsRequest;
import com.by1337.auc.common.network.c2s.C2SPublishLog;
import com.by1337.auc.common.network.s2c.S2CLogAdded;
import com.by1337.auc.common.network.s2c.S2CLogsLoadResponse;
import com.by1337.auc.common.network.s2c.S2COptionalLogRecord;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.util.BSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Duration;

public class LogRepositoryBackend extends GetPostChannelHandler {
    private static final Logger log = LoggerFactory.getLogger(LogRepositoryBackend.class);
    private BAucRuntime channel;
    private final Cache<Long, LogRecord> cache;
    private AuctionLogRepository repository;

    public LogRepositoryBackend() {
        registerGet(C2SGetLogRecordRequest.class, this::getLog);
        registerGet(C2SPublishLog.class, this::publishLog);
        registerGet(C2SLoadLogsRequest.class, this::loadLogs);
        cache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(Duration.ofHours(2))
                .build();

    }

    @Override
    public void init(ChannelRuntime runtime) {
        if (!(runtime instanceof BAucRuntime server)) throw new IllegalArgumentException("Invalid runtime type");
        channel = server;
        repository = new AuctionLogRepository(server.database().dataSource(), server.name() + "_logs_repository");
    }

    @Override
    public void close() {
        repository = null;
        cache.invalidateAll();
    }

    private ResponseFuture<S2COptionalLogRecord> getLog(C2SGetLogRecordRequest request) {
        return new ResponseFuture<>(new S2COptionalLogRecord(cache.get(request.uid(), k -> BSUtils.safe(() -> repository.getById(k).orElse(null)))));
    }

    private ResponseFuture<S2CLogsLoadResponse> loadLogs(C2SLoadLogsRequest request) {
        try {
            var list = repository.findByFilter(request.query());
            return new ResponseFuture<>(new S2CLogsLoadResponse(list));
        } catch (SQLException e) {
            LogRepositoryBackend.log.error("Failed to findByFilter logs", e);
            return new ResponseFuture<>(null);
        }
    }

    private ResponseFuture<A2ALongResponse> publishLog(C2SPublishLog log) {
        try {
            long id = repository.insert(log.log());
            var record = new LogRecord(id, log.log());
            cache.put(id, record);
            channel.broadcast(new S2CLogAdded(record));
            return new ResponseFuture<>(new A2ALongResponse(record.uid()));
        } catch (SQLException e) {
            LogRepositoryBackend.log.error("Failed to insert lot", e);
            return new ResponseFuture<>(null);
        }
    }
}
