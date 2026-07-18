package dev.by1337.auc.pipeline;

import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;

public interface Remote {
    <T extends ChannelMessage> ResponseFuture<T> request(ExpectsResponse<T> msg);
    void write(Packet packet);
}
