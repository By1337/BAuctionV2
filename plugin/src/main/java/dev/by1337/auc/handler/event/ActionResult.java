package dev.by1337.auc.handler.event;

import dev.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.sync.common.channel.ChannelMessage;

public class ActionResult implements ChannelMessage {
    public final boolean success;

    public ActionResult(boolean success) {
        this.success = success;
    }

    public static ActionResult deny() {
        return new ActionResult(false);
    }

    public static ActionResult success() {
        return new ActionResult(true);
    }

    public static ActionResult of(A2AFlagResponse r){
        if (r == null) return deny();
        return r.flag() ? success() : deny();
    }
}
