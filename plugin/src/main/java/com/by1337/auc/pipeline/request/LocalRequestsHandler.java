package com.by1337.auc.pipeline.request;

import com.by1337.auc.handler.SimpleAuction;
import com.by1337.auc.pipeline.LocalChannelContext;
import com.by1337.auc.pipeline.LocalChannelHandler;
import com.by1337.auc.pipeline.LocalPipeline;
import com.by1337.auc.pipeline.Remote;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.channel.handler.request.IncomingRequest;
import dev.by1337.sync.common.channel.handler.request.RequestMsg;
import dev.by1337.sync.common.packet.impl.AckRequest;
import dev.by1337.sync.common.packet.impl.RequestPacket;
import dev.by1337.sync.common.packet.impl.ResponsePacket;
import dev.by1337.sync.common.work.EventLoopWorker;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PriorityQueue;

public class LocalRequestsHandler implements LocalChannelHandler {
    private static final Logger log = LoggerFactory.getLogger(LocalRequestsHandler.class);
    private LocalPipeline pipeline;
    private final Int2ObjectMap<RequestHolder> requests = new Int2ObjectOpenHashMap<>();
    private final PriorityQueue<RequestHolder> requestsQueue = new PriorityQueue<>(256);
    private int requestId;
    private EventLoopWorker eventLoop;
    private boolean closing;
    private Remote remote;

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.pipeline = pipeline;
        eventLoop = pipeline.eventLoop();
        this.remote = remote;
        requestsTimeOuter();
    }

    private void requestsTimeOuter() {
        if (closing) return;
        if (!requests.isEmpty()) {
            long now = System.currentTimeMillis();
            while (true) {
                var request = requestsQueue.peek();
                if (request == null || request.timeoutAt > now) break;
                requestsQueue.poll();
                if (requests.remove(request.id) != null) {
                    try {
                        request.callback.complete(null);
                    } catch (Exception err) {
                        log.error("Failed to accept response", err);
                    }
                }
            }
        }
        if (!requestsQueue.isEmpty()) {
            long delay = Math.max(1, requestsQueue.peek().timeoutAt - System.currentTimeMillis());
            eventLoop.schedule(this::requestsTimeOuter, Math.min(250, delay));
        } else {
            eventLoop.schedule(this::requestsTimeOuter, 250);
        }
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof RequestMsg<?> r) {
            if (closing) {
                try {
                    r.consumer().complete(null);
                } catch (Exception e) {
                    log.error("Failed to accept empty response cuz closing", e);
                }
            } else {
                remote.write(
                        new RequestPacket(newRequest(r.consumer(), r.timeoutMs()).id, r.packet())
                );
            }
        } else if (msg instanceof ResponsePacket r) {
            var v = requests.remove(r.uid());
            if (v != null) {
                // noinspection unchecked
                ResponseFuture<ChannelMessage> callback = (ResponseFuture<ChannelMessage>) v.callback;
                try {
                    callback.complete(r.payload());
                } catch (ClassCastException e) {
                    log.error("Bad response type", e);
                    try {
                        callback.complete(null);
                    } catch (Exception e1) {
                        log.error("Failed to accept response", e1);
                    }
                } catch (Exception e) {
                    log.error("Failed to accept response", e);
                }
            }
        } else if (msg instanceof RequestPacket r) {
            if (r.payload() instanceof AckRequest ack) {
                pipeline.execute(ack.payload());
                remote.write(new ResponsePacket(r.uid(), AckRequest.AckResponse.INSTANCE));
            } else {
                pipeline.execute(
                        new IncomingRequest(r.payload(),
                                result -> remote.write(new ResponsePacket(r.uid(), result))
                        )
                );
            }
        } else {
            ctx.fire(msg);
        }
    }

    @Override
    public void close() {
        closing = true;
        requests.values().forEach(v -> {
            try {
                v.callback.complete(null);
            } catch (Exception e) {
                log.error("Failed to accept empty response cuz closing", e);
            }
        });
        requestsQueue.clear();
        requests.clear();
    }

    private RequestHolder newRequest(ResponseFuture<?> consumer, long timeoutMs) {
        var id = requestId++;
        var v = new RequestHolder(id, System.currentTimeMillis() + timeoutMs, consumer);
        requests.put(id, v);
        requestsQueue.offer(v);
        return v;
    }

    public static class RequestHolder implements Comparable<RequestHolder> {
        private final int id;
        private final long timeoutAt;
        private final ResponseFuture<?> callback;

        public RequestHolder(int id, long timeoutAt, ResponseFuture<?> callback) {
            this.id = id;
            this.timeoutAt = timeoutAt;
            this.callback = callback;
        }

        @Override
        public int compareTo(@NotNull RequestHolder o) {
            return Long.compare(timeoutAt, o.timeoutAt);
        }
    }
}
