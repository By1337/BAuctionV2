package com.by1337.auc.common.handler;

import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.channel.handler.request.IncomingRequest;
import dev.by1337.sync.common.channel.pipeline.ChannelContext;
import dev.by1337.sync.common.channel.pipeline.ChannelHandler;
import dev.by1337.sync.common.channel.pipeline.Connection;
import dev.by1337.sync.common.packet.ExpectsResponse;

import java.util.HashMap;
import java.util.Map;

public abstract class GetPostChannelHandler implements ChannelHandler {
    private final Map<Class<?>, Responser<?, ?>> gets = new HashMap<>();
    private final Map<Class<?>, EConsumer<?>> posts = new HashMap<>();

    @Override
    public void handle(ChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof IncomingRequest r) {
            if (r.payload() instanceof ExpectsResponse<?> e) {
                Responser f = gets.get(e.getClass());
                if (f != null) {
                    ResponseFuture res = f.response(e, ctx.connection());
                    System.out.println(e + " -> " + res);
                    res.then(v -> {
                        System.out.println(e + " -> " + v);
                        r.response((ExpectsResponse) e, (ChannelMessage) v);
                    });
                } else {
                    ctx.fire(msg);
                }
            } else {
                ctx.fire(msg);
            }
        } else {
            EConsumer c = posts.get(msg.getClass());
            if (c != null) {
                c.accept(msg);
            } else {
                ctx.fire(msg);
            }
        }
    }

    public <T extends ChannelMessage, E extends ExpectsResponse<T>> void registerGet(Class<E> type, Responser<T, E> responser) {
        gets.put(type, responser);
    }

    public <T extends ChannelMessage> void registerPost(Class<T> t, EConsumer<T> c) {
        posts.put(t, c);
    }

    @FunctionalInterface
    public interface Responser<T extends ChannelMessage, E extends ExpectsResponse<T>> {
        default ResponseFuture<T> response(E msg, Connection from) throws Exception {
            return response(msg);
        }

        ResponseFuture<T> response(E msg) throws Exception;
    }

    @FunctionalInterface
    public interface EFunction<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface EConsumer<T> {
        void accept(T t);
    }
}
