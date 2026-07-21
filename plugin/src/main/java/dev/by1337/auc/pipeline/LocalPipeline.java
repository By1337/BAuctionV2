package dev.by1337.auc.pipeline;

import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.channel.pipeline.Connection;
import dev.by1337.sync.common.channel.pipeline.SocketConnection;
import dev.by1337.sync.common.work.EventLoopWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class LocalPipeline {

    private static final Logger log = LoggerFactory.getLogger(LocalPipeline.class);
    private Entry[] handlers = new Entry[0];

    private final EventLoopWorker eventLoop;
    private InitData initData;

    private final Connection connection = new Connection() {
        final SocketConnection socketConnection = this::write;

        @Override
        public void write(ChannelMessage msg) {
            execute(msg);
        }

        @Override
        public SocketConnection transport() {
            return socketConnection;
        }
    };

    public LocalPipeline(EventLoopWorker eventLoop) {
        this.eventLoop = eventLoop;
    }

    public Connection asConnection() {
        return connection;
    }

    public <T> ResponseFuture<T> submit(Supplier<ResponseFuture<T>> s) {
        if (eventLoop.isWorkerThread()) {
            return s.get();
        }
        ResponseFuture<T> future = new ResponseFuture<>();
        eventLoop.execute(() -> s.get().then(future::complete));
        eventLoop.schedule(() -> {
            //todo?
            if (!future.hasResult()) {
                log.error("Timeout request {}", s);
                future.complete(null);
            }
        }, 5_000);
        return future;
    }

    public void schedule(ChannelMessage msg) {
        eventLoop.schedule(() -> {
            execute0(msg, 0);
        });
    }

    public void schedule(ChannelMessage msg, long ms) {
        eventLoop.schedule(() -> {
            execute0(msg, 0);
        }, ms);
    }

    public void execute(ChannelMessage msg) {
        eventLoop.execute(() -> {
            execute0(msg, 0);
        });
    }


    private void execute0(ChannelMessage msg, int idx) {
        if (idx >= handlers.length) {
            if (!(msg instanceof ChannelMessage.UnhandledIgnored))
                log.warn("unprocessed message {}", msg);
            return;
        }
        try (var ctx = new ChannelContextImpl(idx)) {
            try {
                handlers[idx].handler.handle(ctx, msg);
            } catch (Exception e) {
                log.error("Failed to handle message in handler {}", handlers[idx].name, e);
            }
        }
    }

    private record InitData(Remote remote, SimpleAuction auction) {

    }

    public void initAll(Remote remote, SimpleAuction auction) {
        eventLoop.execute(() -> {
            initData = new InitData(remote, auction);
            for (Entry handler : handlers) {
                handler.handler.init(this, remote, auction);
            }
        });
    }

    public EventLoopWorker eventLoop() {
        return eventLoop;
    }

    public LocalChannelHandler getHandler(String name) {
        for (Entry handler : handlers) {
            if (handler.name.equals(name)) return handler.handler;
        }
        throw new IllegalArgumentException("Unknown handler " + name);
    }

    public <T extends LocalChannelHandler> T get(Class<T> t) {
        for (Entry handler : handlers) {
            if (t.isAssignableFrom(handler.handler.getClass())) return t.cast(handler.handler);
        }
        throw new IllegalArgumentException("Unknown handler " + t);
    }

    public CompletableFuture<Void> closeAll() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        boolean fromEventLoop = eventLoop.isWorkerThread();
        eventLoop.execute(() -> {
            try {
                for (Entry handler : handlers) {
                    try {
                        handler.handler.close();
                    } catch (Exception e) {
                        log.error("Failed to close {}", handler.name, e);
                    }
                }
            } finally {
                if (fromEventLoop) {
                    future.complete(null);
                } else {
                    //сами handler'ы могут ложить новые таски в eventLoop, отпустим future после тех тасков
                    eventLoop.schedule(() -> future.complete(null));
                }
            }
        });
        return future;
    }

    private class ChannelContextImpl implements LocalChannelContext, AutoCloseable {
        private final int idx;
        private boolean closed;

        private ChannelContextImpl(int idx) {
            this.idx = idx;
        }

        @Override
        public void fire(ChannelMessage msg) {
            if (closed) throw new IllegalStateException("closed");
            execute0(msg, idx + 1);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    public LocalPipeline addLast(String name, LocalChannelHandler handler) {
        handlers = Arrays.copyOf(handlers, handlers.length + 1);
        handlers[handlers.length - 1] = new Entry(name, handler);
        if (initData != null) {
            try {
                handler.init(this, initData.remote, initData.auction);
            } catch (Exception e) {
                log.error("Failed to init handler ", e);
            }
        }
        return this;
    }

    public LocalPipeline addFirst(String name, LocalChannelHandler handler) {
        var arr = Arrays.copyOf(handlers, handlers.length + 1);
        System.arraycopy(handlers, 0, arr, 1, handlers.length);
        arr[0] = new Entry(name, handler);
        handlers = arr;
        if (initData != null) {
            try {
                handler.init(this, initData.remote, initData.auction);
            } catch (Exception e) {
                log.error("Failed to init handler ", e);
            }
        }
        return this;
    }

    public Entry[] getHandlers() {
        return Arrays.copyOf(handlers, handlers.length);
    }

    public record Entry(String name, LocalChannelHandler handler) {
    }
}
