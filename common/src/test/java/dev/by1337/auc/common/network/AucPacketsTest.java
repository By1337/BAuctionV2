package dev.by1337.auc.common.network;

import dev.by1337.auc.common.auc.AucLot;
import dev.by1337.auc.common.auc.VaultLot;
import dev.by1337.auc.common.auc.log.*;
import dev.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.auc.common.network.a2a.A2ALongResponse;
import dev.by1337.auc.common.network.a2a.A2ASetPlayerNamePacket;
import dev.by1337.auc.common.network.c2s.*;
import dev.by1337.auc.common.network.s2c.*;
import dev.by1337.sync.common.packet.Packet;
import dev.by1337.sync.common.packet.Packets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

public class AucPacketsTest {

    @Test
    public void run() {
        AucPackets.boot();
        AuctionLogBoot.boot();
        var lot = new AucLot(888, 123, UUID.randomUUID(), 123, 123, 13, 37);
        var vault = new VaultLot(123, 123, UUID.randomUUID(), 321, 13, 37);
        var uuid = UUID.randomUUID();
        var array = new byte[]{13, 37};
        assertReadWrite(new C2SAddNewLotRequest(10, uuid, System.currentTimeMillis(), 55, 1337));
        assertReadWrite(new S2CChangeNameEventPacket(uuid, "name"));
        assertReadWrite(new S2CLotUpdate(new AucLot(888, 123, uuid, 123, 123, 13, 37)));
        assertReadWrite(new A2AFlagResponse(false));
        assertReadWrite(new C2SPushItemRequest(array, array));
        assertReadWrite(new S2CItemIdResponsePacket(999));
        assertReadWrite(new C2SLoadItemRequest(999));
        assertReadWrite(new S2CItemResponsePacket(array));
        assertReadWrite(new SendMessagePacket(uuid, "123"));
        assertReadWrite(new S2CRemoveLotPacket(123));
        assertReadWrite(new C2SRemoveLotRequest(123));
        assertReadWrite(new C2SMove2VaultRequest(123, uuid, 123));
        assertReadWrite(new S2CVaultLotUpdate(vault));
        assertReadWrite(new C2SAddNewVaultRequest(123, uuid, 321, 13, 1337));
        assertReadWrite(new C2SRemoveVaultLotRequest(123));
        assertReadWrite(new S2CRemoveVaultLotPacket(123));
        assertReadWrite(new A2ASetPlayerNamePacket(uuid, "321"));
        assertReadWrite(new S2CPlayerNameResponse("123"));
        assertReadWrite(new C2SPlayerNameRequest(uuid));
        assertReadWrite(new C2SPublishLog(new BuyAuctionLog(321, uuid, uuid, 13, 23, 123)));
        assertReadWrite(new S2CLogAdded(new LogRecord(123, new BuyAuctionLog(321, uuid, uuid, 13, 23, 123))));
        assertReadWrite(new S2CLogsLoadResponse(List.of(new LogRecord(123, new BuyAuctionLog(321, uuid, uuid, 13, 23, 123)))));
        assertReadWrite(new C2SLoadLogsRequest(new LogQuery(123L, 123L, 123L, 123L, uuid, uuid, "123", 100)));
        assertReadWrite(new A2ALongResponse(123));
        assertReadWrite(new S2COptionalLogRecord(new LogRecord(123, new BuyAuctionLog(321, uuid, uuid, 13, 23, 123))));
        assertReadWrite(new C2SGetLogRecordRequest(123));
        assertReadWrite(new C2SSubtractLotRequest(123, 321));
        assertReadWrite(new S2CLotCountChange(123, 321));
        assertReadWrite(new S2COptionalLot(lot));
        assertReadWrite(new C2SGetLotRequest(123));
        assertReadWrite(new C2SGetAllLotsRequest());
        assertReadWrite(new S2CActualLotsUids(new int[]{123, 321, 122}));
        assertReadWrite(new S2COptionalVaultLot(vault));
        assertReadWrite(new C2SGetAllVaultLotsRequest());
        assertReadWrite(new S2CActualVaultLotsUids(new int[]{123, 321, 122}));
        assertReadWrite(new C2SGetVaultLotRequest(123));


    }

    private <T extends Packet> void assertReadWrite(T packet) {
        ByteBuf buf = Unpooled.buffer();
        Packets.write(buf, Packets.PROTOCOL_VERSION, packet);
        Assert.assertEquals(Packets.read(buf, Packets.PROTOCOL_VERSION), packet);
        Assert.assertEquals(buf.readableBytes(), 0);
        buf.release();
    }
}