package com.by1337.auc.common.auc.log;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record LogRecord(int uid, AuctionLog log) {

    public void write(ByteBuf buf, int protocolVersion){
        buf.writeInt(uid);
        log.write(buf, protocolVersion);
    }
    public static LogRecord read(ByteBuf buf, int protocolVersion){
        int uid = buf.readInt();
        AuctionLog log = AuctionLog.read(buf, protocolVersion);
        return new LogRecord(uid, log);
    }

    public long timestamp() {
        return log.timestamp();
    }

    public UUID actor() {
        return log.actor();
    }

    public @Nullable UUID subject() {
        return log.subject();
    }

    public String type() {
        return log.type();
    }

    public void writePayload(ByteBuf buf) {
        log.writePayload(buf);
    }
}
