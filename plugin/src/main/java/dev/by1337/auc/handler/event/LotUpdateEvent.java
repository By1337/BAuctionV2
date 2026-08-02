package dev.by1337.auc.handler.event;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.sync.common.channel.ChannelMessage;

public record LotUpdateEvent(ClientAucLot lot) implements ChannelMessage.UnhandledIgnored {
}
