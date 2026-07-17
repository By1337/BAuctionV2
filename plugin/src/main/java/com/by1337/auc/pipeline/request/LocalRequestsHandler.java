package com.by1337.auc.pipeline.request;

import com.by1337.auc.handler.SimpleAuction;
import com.by1337.auc.pipeline.LocalChannelContext;
import com.by1337.auc.pipeline.LocalChannelHandler;
import com.by1337.auc.pipeline.LocalPipeline;
import com.by1337.auc.pipeline.Remote;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.channel.handler.request.RequestsHandler;
import dev.by1337.sync.common.channel.pipeline.ChannelContext;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.channel.pipeline.Connection;
import dev.by1337.sync.common.channel.pipeline.Pipeline;
import dev.by1337.sync.common.work.EventLoopWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalRequestsHandler implements LocalChannelHandler {
    private static final Logger log = LoggerFactory.getLogger(LocalRequestsHandler.class);
    private RequestsHandler requests;
    private LocalPipeline pipeline;

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.pipeline = pipeline;
        requests = new RequestsHandler();
        requests.init(new ChannelRuntime() {
            @Override
            public Pipeline pipeline() {
                throw new UnsupportedOperationException();
            }

            @Override
            public EventLoopWorker eventLoop() {
                return pipeline.eventLoop();
            }

            @Override
            public Logger logger() {
                return log;
            }
        });
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        requests.handle(new ChannelContext() {
            @Override
            public void fire(ChannelMessage msg) {
                ctx.fire(msg);
            }

            @Override
            public Pipeline pipeline() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Connection connection() {
                return pipeline.asConnection();
            }

            @Override
            public void execute(ChannelMessage msg, Connection out) {
                pipeline.execute(msg);
            }
        }, msg);
    }

    @Override
    public void close() {

    }
}
