package com.by1337.auc.handler.backend;

import com.by1337.auc.common.auc.log.AuctionLog;
import com.by1337.auc.common.handler.GetPostChannelHandler;
import com.by1337.auc.common.network.c2s.C2SLoadLogsRequest;
import com.by1337.auc.common.network.c2s.C2SPublishLog;
import com.by1337.auc.common.network.s2c.S2CLogAdded;
import com.by1337.auc.common.network.s2c.S2CLogsLoadResponse;
import dev.by1337.sync.client.channel.ClientChannelRuntime;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.channel.pipeline.Connection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

public class LogRepositoryBackend extends GetPostChannelHandler {
    private Connection remote;
    private final ConcurrentSkipListSet<AuctionLog> logs = new ConcurrentSkipListSet<>((o1, o2) -> {
        var v = -Long.compare(o1.timestamp(), o2.timestamp());
        if (v == 0) return Integer.compare(o1.hashCode(), o2.hashCode());
        return v;
    });

    public LogRepositoryBackend() {
        registerPost(C2SPublishLog.class, this::publishLog);
        registerGet(C2SLoadLogsRequest.class, this::loadLogs);
    }

    private ResponseFuture<S2CLogsLoadResponse> loadLogs(C2SLoadLogsRequest request) {
        List<AuctionLog> result = new ArrayList<>();
        int limit = request.limit() != -1 ? request.limit() : Integer.MAX_VALUE;
        for (AuctionLog log : logs) {
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

    private void publishLog(C2SPublishLog log) {
        logs.add(log.log());
        remote.write(new S2CLogAdded(log.log())); //broadcast
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
