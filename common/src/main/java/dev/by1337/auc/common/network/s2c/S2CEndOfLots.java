package dev.by1337.auc.common.network.s2c;

import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record S2CEndOfLots() implements Packet, ChannelMessage.UnhandledIgnored {

    public S2CEndOfLots(ByteBuf buf, int protocolVersion) {
        this();
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
    }
}
