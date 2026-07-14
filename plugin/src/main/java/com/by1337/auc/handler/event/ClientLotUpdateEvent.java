package com.by1337.auc.handler.event;

import com.by1337.auc.auc.ClientAucLot;
import dev.by1337.sync.common.channel.ChannelMessage;

public record ClientLotUpdateEvent(ClientAucLot lot) implements ChannelMessage.UnhandledIgnored {
}
