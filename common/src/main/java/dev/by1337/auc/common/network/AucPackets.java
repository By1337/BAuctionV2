package dev.by1337.auc.common.network;

import dev.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.auc.common.network.a2a.A2ALongResponse;
import dev.by1337.auc.common.network.a2a.A2ASetPlayerNamePacket;
import dev.by1337.auc.common.network.c2s.*;
import dev.by1337.auc.common.network.s2c.*;
import dev.by1337.sync.common.packet.PacketRegistry;

public class AucPackets {
    public static final PacketRegistry MAIN = new PacketRegistry("bauc:main", 2)
            .add(0, C2SAddNewLotRequest.class, C2SAddNewLotRequest::new)
            .add(1, S2CLotUpdate.class, S2CLotUpdate::new)
            .add(2, A2AFlagResponse.class, A2AFlagResponse::new)
            .add(3, C2SPushItemRequest.class, C2SPushItemRequest::new)
            .add(4, S2CItemIdResponsePacket.class, S2CItemIdResponsePacket::new)
            .add(5, C2SLoadItemRequest.class, C2SLoadItemRequest::new)
            .add(6, S2CItemResponsePacket.class, S2CItemResponsePacket::new)
            .add(7, S2COnLotRemovePacket.class, S2COnLotRemovePacket::new)
            .add(8, C2SRemoveLotRequest.class, C2SRemoveLotRequest::new)
            .add(9, C2SMove2VaultRequest.class, C2SMove2VaultRequest::new)
            .add(10, S2CVaultLotUpdate.class, S2CVaultLotUpdate::new)
            .add(11, C2SAddNewVaultRequest.class, C2SAddNewVaultRequest::new)
            .add(12, C2SRemoveVaultLotRequest.class, C2SRemoveVaultLotRequest::new)
            .add(13, S2COnVaultLotRemovePacket.class, S2COnVaultLotRemovePacket::new)
            .add(14, A2ASetPlayerNamePacket.class, A2ASetPlayerNamePacket::new)
            .add(15, S2CPlayerNameResponse.class, S2CPlayerNameResponse::new)
            .add(16, C2SPlayerNameRequest.class, C2SPlayerNameRequest::new)
            .add(17, C2SPublishLog.class, C2SPublishLog::new)
            .add(18, S2CLogAdded.class, S2CLogAdded::new)
            .add(19, S2CLogsLoadResponse.class, S2CLogsLoadResponse::read)
            .add(20, C2SLoadLogsRequest.class, C2SLoadLogsRequest::read)
            .add(21, A2ALongResponse.class, A2ALongResponse::new)
            .add(22, S2COptionalLogRecord.class, S2COptionalLogRecord::new)
            .add(23, C2SGetLogRecordRequest.class, C2SGetLogRecordRequest::new)
            .add(24, C2SSubtractLotRequest.class, C2SSubtractLotRequest::new)
            .add(25, S2CLotCountChange.class, S2CLotCountChange::new)
            .add(26, S2COptionalLot.class, S2COptionalLot::new)
            .add(27, C2SGetLotRequest.class, C2SGetLotRequest::new)
            .add(28, C2SGetAllLotsRequest.class, C2SGetAllLotsRequest::new)
            .add(29, S2CActualLotsUids.class, S2CActualLotsUids::new)
            .add(30, S2COptionalVaultLot.class, S2COptionalVaultLot::new)
            .add(31, C2SGetAllVaultLotsRequest.class, C2SGetAllVaultLotsRequest::new)
            .add(32, S2CActualVaultLotsUids.class, S2CActualVaultLotsUids::new)
            .add(33, C2SGetVaultLotRequest.class, C2SGetVaultLotRequest::new)
            .add(34, C2SPlayerUUIDRequest.class, C2SPlayerUUIDRequest::new)
            .add(35, S2CPlayerNameUUIDResponse.class, S2CPlayerNameUUIDResponse::new)
            .add(36, S2CEndOfLots.class, S2CEndOfLots::new)
            .add(37, S2CEndOfVaultLots.class, S2CEndOfVaultLots::new)
            .lock();

    public static void boot() {
    }
}
