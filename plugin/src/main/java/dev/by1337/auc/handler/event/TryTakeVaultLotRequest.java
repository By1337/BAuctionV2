package dev.by1337.auc.handler.event;

import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.packet.ExpectsResponse;

import java.util.UUID;

public record TryTakeVaultLotRequest(UUID who, int uidItem)  implements ChannelMessage, ExpectsResponse<ActionResult> {

}
