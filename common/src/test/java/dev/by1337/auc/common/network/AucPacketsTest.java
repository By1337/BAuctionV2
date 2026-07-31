package dev.by1337.auc.common.network;

import dev.by1337.auc.common.auc.AucLot;
import dev.by1337.auc.common.auc.VaultLot;
import dev.by1337.auc.common.auc.log.*;
import dev.by1337.auc.common.auc.log.impl.BuyAuctionLog;
import dev.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.auc.common.network.a2a.A2ALongResponse;
import dev.by1337.auc.common.network.a2a.A2ASetPlayerNamePacket;
import dev.by1337.auc.common.network.c2s.*;
import dev.by1337.auc.common.network.s2c.*;
import dev.by1337.sync.common.packet.Packet;
import dev.by1337.sync.common.packet.PacketRegistries;
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
        PacketRegistries registries = new PacketRegistries();
        registries.add(0, AucPackets.MAIN.id(), AucPackets.MAIN);
        var lot = new AucLot(888, 123, UUID.randomUUID(), 123, 123, 13, 37);
        var vault = new VaultLot(123, 123, UUID.randomUUID(), 321, 13, 37);
        var uuid = UUID.randomUUID();
        var array = new byte[]{13, 37};
        assertReadWrite(registries, new C2SAddNewLotRequest(10, uuid, System.currentTimeMillis(), 55, 1337));
        assertReadWrite(registries, new S2CLotUpdate(new AucLot(888, 123, uuid, 123, 123, 13, 37)));
        assertReadWrite(registries, new A2AFlagResponse(false));
        assertReadWrite(registries, new C2SPushItemRequest(array));
        assertReadWrite(registries, new S2CItemIdResponsePacket(999));
        assertReadWrite(registries, new C2SLoadItemRequest(999));
        assertReadWrite(registries, new S2CItemResponsePacket(array));
        assertReadWrite(registries, new S2COnLotRemovePacket(123));
        assertReadWrite(registries, new C2SRemoveLotRequest(123));
        assertReadWrite(registries, new C2SMove2VaultRequest(123, uuid, 123));
        assertReadWrite(registries, new S2CVaultLotUpdate(vault));
        assertReadWrite(registries, new C2SAddNewVaultRequest(123, uuid, 321, 13, 1337));
        assertReadWrite(registries, new C2SRemoveVaultLotRequest(123));
        assertReadWrite(registries, new S2COnVaultLotRemovePacket(123));
        assertReadWrite(registries, new A2ASetPlayerNamePacket(uuid, "321"));
        assertReadWrite(registries, new S2CPlayerNameResponse("123"));
        assertReadWrite(registries, new C2SPlayerNameRequest(uuid));
        assertReadWrite(registries, new C2SPublishLog(new BuyAuctionLog(321, uuid, uuid, 13, 23, 123)));
        assertReadWrite(registries, new S2CLogAdded(new LogRecord(123, new BuyAuctionLog(321, uuid, uuid, 13, 23, 123))));
        assertReadWrite(registries, new S2CLogsLoadResponse(List.of(new LogRecord(123, new BuyAuctionLog(321, uuid, uuid, 13, 23, 123)))));
        assertReadWrite(registries, new C2SLoadLogsRequest(new LogQuery(123L, 123L, 123L, 123L, uuid, uuid, "123", 100)));
        assertReadWrite(registries, new A2ALongResponse(123));
        assertReadWrite(registries, new S2COptionalLogRecord(new LogRecord(123, new BuyAuctionLog(321, uuid, uuid, 13, 23, 123))));
        assertReadWrite(registries, new C2SGetLogRecordRequest(123));
        assertReadWrite(registries, new C2SSubtractLotRequest(123, 321));
        assertReadWrite(registries, new S2CLotCountChange(123, 321));
        assertReadWrite(registries, new S2COptionalLot(lot));
        assertReadWrite(registries, new C2SGetLotRequest(123));
        assertReadWrite(registries, new C2SGetAllLotsRequest());
        assertReadWrite(registries, new S2CActualLotsUids(new int[]{123, 321, 122}));
        assertReadWrite(registries, new S2COptionalVaultLot(vault));
        assertReadWrite(registries, new C2SGetAllVaultLotsRequest());
        assertReadWrite(registries, new S2CActualVaultLotsUids(new int[]{123, 321, 122}));
        assertReadWrite(registries, new C2SGetVaultLotRequest(123));


    }

    private <T extends Packet> void assertReadWrite(PacketRegistries registries, T packet) {
        ByteBuf buf = Unpooled.buffer();
        Packets.write(buf, registries, packet);
        Assert.assertEquals(Packets.read(buf, registries), packet);
        Assert.assertEquals(buf.readableBytes(), 0);
        buf.release();
    }
}