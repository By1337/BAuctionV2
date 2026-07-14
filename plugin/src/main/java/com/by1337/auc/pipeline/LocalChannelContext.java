package com.by1337.auc.pipeline;

import dev.by1337.sync.common.channel.ChannelMessage;


public interface LocalChannelContext {
    void fire(ChannelMessage msg);
}
