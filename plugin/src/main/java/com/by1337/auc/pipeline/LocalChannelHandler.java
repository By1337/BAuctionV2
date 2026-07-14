package com.by1337.auc.pipeline;

import dev.by1337.sync.common.channel.ChannelMessage;

public interface LocalChannelHandler {
    void init(LocalPipeline pipeline, Remote remote);

    void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception;

    void close();
}
