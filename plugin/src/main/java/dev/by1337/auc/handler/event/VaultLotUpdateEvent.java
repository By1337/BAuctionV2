package dev.by1337.auc.handler.event;

import dev.by1337.auc.auc.ClientVaultLot;
import dev.by1337.sync.common.channel.ChannelMessage;

public record VaultLotUpdateEvent(ClientVaultLot lot) implements ChannelMessage.UnhandledIgnored {
}
