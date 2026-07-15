package com.by1337.auc.handler.event;

import com.by1337.auc.user.AucUser;
import dev.by1337.sync.common.channel.ChannelMessage;

public record UserMailEvent(AucUser user, String mail) implements ChannelMessage {
}
