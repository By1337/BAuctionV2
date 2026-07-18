package dev.by1337.auc.common.handler;

import dev.by1337.sync.bd.DatabaseSource;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;

public interface DataSourceRuntime extends ChannelRuntime {
    DatabaseSource database();
}
