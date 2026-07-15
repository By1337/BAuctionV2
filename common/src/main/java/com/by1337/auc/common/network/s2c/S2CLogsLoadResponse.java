package com.by1337.auc.common.network.s2c;

import com.by1337.auc.common.auc.log.AuctionLog;
import com.by1337.auc.common.auc.log.LogRecord;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public record S2CLogsLoadResponse(List<LogRecord> logs) implements Packet {

    public static S2CLogsLoadResponse read(ByteBuf buf, int protocolVersion) {
        int count = buf.readInt();
        List<LogRecord> logs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            logs.add(LogRecord.read(buf, protocolVersion));
        }
        return new S2CLogsLoadResponse(logs);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(logs.size());
        for (LogRecord log : logs) {
            log.write(buf, protocolVersion);
        }
    }
}
