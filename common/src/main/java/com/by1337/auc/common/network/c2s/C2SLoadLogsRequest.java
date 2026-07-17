package com.by1337.auc.common.network.c2s;

import com.by1337.auc.common.auc.log.LogQuery;
import com.by1337.auc.common.network.s2c.S2CLogsLoadResponse;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import io.netty.buffer.ByteBuf;

public record C2SLoadLogsRequest(LogQuery query) implements Packet, ExpectsResponse<S2CLogsLoadResponse> {

    public static C2SLoadLogsRequest read(ByteBuf buf, int protocolVersion) {
        return new C2SLoadLogsRequest(LogQuery.read(buf));
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        query.write(buf);
    }
}
