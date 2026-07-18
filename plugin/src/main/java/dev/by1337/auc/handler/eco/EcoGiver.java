package dev.by1337.auc.handler.eco;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.handler.event.UserMailEvent;
import dev.by1337.auc.pipeline.LocalChannelContext;
import dev.by1337.auc.pipeline.LocalChannelHandler;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.pipeline.Remote;
import dev.by1337.auc.user.AucUser;
import dev.by1337.auc.user.UserMails;
import dev.by1337.sync.common.channel.ChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EcoGiver implements LocalChannelHandler {
    private static final Logger log = LoggerFactory.getLogger(EcoGiver.class);

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof UserMailEvent(AucUser user, String mail)) {
            if (UserMails.isDepositCents(mail)) {
                long cents = UserMails.getLong(mail);
                BAuction.economy().depositCents(user.uuid, cents);
                if (!BAuction.playerList().isOnline(user.uuid)){
                    log.warn("Give to offline player! {} {}", user.uuid, mail);
                }
            } else {
                ctx.fire(msg);
            }
        } else {
            ctx.fire(msg);
        }
    }


    @Override
    public void close() {
    }
}
