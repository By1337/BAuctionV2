package com.by1337.auc.common.handler;

import dev.by1337.sync.common.channel.pipeline.BaseServerChannelRuntime;
import dev.by1337.sync.common.work.EventLoopWorker;

public interface BAucRuntime extends BaseServerChannelRuntime, DataSourceRuntime {
    EventLoopWorker ioWorker();
}
