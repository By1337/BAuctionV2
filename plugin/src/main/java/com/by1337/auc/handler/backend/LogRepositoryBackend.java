package com.by1337.auc.handler.backend;

import com.by1337.auc.common.auc.log.AuctionLog;
import com.by1337.auc.common.auc.log.LogRecord;
import com.by1337.auc.common.handler.GetPostChannelHandler;
import com.by1337.auc.common.network.a2a.A2AI32Response;
import com.by1337.auc.common.network.c2s.C2SGetLogRequest;
import com.by1337.auc.common.network.c2s.C2SLoadLogsRequest;
import com.by1337.auc.common.network.c2s.C2SPublishLog;
import com.by1337.auc.common.network.s2c.S2CLogAdded;
import com.by1337.auc.common.network.s2c.S2CLogsLoadResponse;
import com.by1337.auc.common.network.s2c.S2COptionalLogRecord;
import dev.by1337.sync.client.channel.ClientChannelRuntime;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.channel.pipeline.Connection;
import dev.by1337.sync.common.packet.ExpectsResponse;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

public class LogRepositoryBackend extends GetPostChannelHandler {
    private Connection remote;
    private int lastId;
    private final ConcurrentSkipListSet<LogRecord> logs = new ConcurrentSkipListSet<>((o1, o2) -> {
        var v = -Long.compare(o1.log().timestamp(), o2.log().timestamp());
        if (v == 0) return Integer.compare(o1.uid(), o2.uid());
        return v;
    });
    private final Int2ObjectOpenHashMap<LogRecord> id2log = new Int2ObjectOpenHashMap<>();

    public LogRepositoryBackend() {
        registerGet(C2SGetLogRequest.class, this::getLog);
        registerGet(C2SPublishLog.class, this::publishLog);
        registerGet(C2SLoadLogsRequest.class, this::loadLogs);
    }

    private ResponseFuture<S2COptionalLogRecord> getLog(C2SGetLogRequest request) {
        return new ResponseFuture<>(new S2COptionalLogRecord(id2log.get(request.uid())));
    }

    private ResponseFuture<S2CLogsLoadResponse> loadLogs(C2SLoadLogsRequest request) {
        List<LogRecord> result = new ArrayList<>();
        int limit = request.limit() != -1 ? request.limit() : Integer.MAX_VALUE;
        for (LogRecord log : logs) {
            if (log.timestamp() < request.after()) break;
            if (request.actor() != null && !Objects.equals(request.actor(), log.actor())) continue;
            if (request.subject() != null && !Objects.equals(request.subject(), log.subject())) continue;
            if (request.type() != null && !Objects.equals(request.type(), log.type())) continue;
            result.add(log);
            if (result.size() >= limit) {
                break;
            }
        }
        return new ResponseFuture<>(new S2CLogsLoadResponse(result));
    }

    private ResponseFuture<A2AI32Response> publishLog(C2SPublishLog log) {
        LogRecord record = new LogRecord(lastId++, log.log());
        logs.add(record);
        id2log.put(record.uid(), record);
        remote.write(new S2CLogAdded(record)); //broadcast
        return new ResponseFuture<>(new A2AI32Response(record.uid()));
    }

    @Override
    public void init(ChannelRuntime runtime) {
        if (!(runtime instanceof ClientChannelRuntime ccr)) throw new IllegalArgumentException("Invalid runtime type");
        remote = ccr.remote();
    }

    @Override
    public void close() {

    }
}
