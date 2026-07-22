package dev.by1337.auc.handler.event;

import dev.by1337.sync.common.channel.ChannelMessage;

import java.util.UUID;

public record PlayerChangeNameEvent(UUID uuid, String newName, String oldName) implements ChannelMessage.UnhandledIgnored {
}
