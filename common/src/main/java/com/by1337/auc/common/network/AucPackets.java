package com.by1337.auc.common.network;

import com.by1337.auc.common.network.a2a.A2AFlagResponse;
import com.by1337.auc.common.network.a2a.A2ALongResponse;
import com.by1337.auc.common.network.a2a.A2ASetPlayerNamePacket;
import com.by1337.auc.common.network.c2s.*;
import com.by1337.auc.common.network.s2c.*;
import dev.by1337.sync.common.packet.PacketRegistry;
import dev.by1337.sync.common.packet.Packets;

public class AucPackets {

    public static void boot(){
    }
    static {
        Packets.REGISTRIES.add("bauc:main", new PacketRegistry()
                .add(C2SAddNewLotRequest.class, C2SAddNewLotRequest::new)
                .add(S2CChangeNameEventPacket.class, S2CChangeNameEventPacket::new)
                .add(S2CLotUpdate.class, S2CLotUpdate::new)
                .add(A2AFlagResponse.class, A2AFlagResponse::new)
                .add(C2SPushItemRequest.class, C2SPushItemRequest::new)
                .add(S2CItemIdResponsePacket.class, S2CItemIdResponsePacket::new)
                .add(C2SLoadItemRequest.class, C2SLoadItemRequest::new)
                .add(S2CItemResponsePacket.class, S2CItemResponsePacket::new)
                .add(SendMessagePacket.class, SendMessagePacket::new)
                .add(S2CRemoveLotPacket.class, S2CRemoveLotPacket::new)
                .add(C2SRemoveLotRequest.class, C2SRemoveLotRequest::new)
                .add(C2SMove2VaultRequest.class, C2SMove2VaultRequest::new)
                .add(S2CVaultLotUpdate.class, S2CVaultLotUpdate::new)
                .add(C2SAddNewVaultRequest.class, C2SAddNewVaultRequest::new)
                .add(C2SRemoveVaultLotRequest.class, C2SRemoveVaultLotRequest::new)
                .add(S2CRemoveVaultLotPacket.class, S2CRemoveVaultLotPacket::new)
                .add(A2ASetPlayerNamePacket.class, A2ASetPlayerNamePacket::new)
                .add(S2CPlayerNameResponse.class, S2CPlayerNameResponse::new)
                .add(C2SPlayerNameRequest.class, C2SPlayerNameRequest::new)
                .add(C2SPublishLog.class, C2SPublishLog::new)
                .add(S2CLogAdded.class, S2CLogAdded::new)
                .add(S2CLogsLoadResponse.class, S2CLogsLoadResponse::read)
                .add(C2SLoadLogsRequest.class, C2SLoadLogsRequest::read)
                .add(A2ALongResponse.class, A2ALongResponse::new)
                .add(S2COptionalLogRecord.class, S2COptionalLogRecord::new)
                .add(C2SGetLogRecordRequest.class, C2SGetLogRecordRequest::new)
                .add(C2SSubtractLotRequest.class, C2SSubtractLotRequest::new)
                .add(S2CLotCountChange.class, S2CLotCountChange::new)
                .add(S2COptionalLot.class, S2COptionalLot::new)
                .add(C2SGetLotRequest.class, C2SGetLotRequest::new)
                .add(C2SGetAllLotsRequest.class, C2SGetAllLotsRequest::new)
                .add(S2CActualLotsUids.class, S2CActualLotsUids::new)
                .add(S2COptionalVaultLot.class, S2COptionalVaultLot::new)
                .add(C2SGetAllVaultLotsRequest.class, C2SGetAllVaultLotsRequest::new)
                .add(S2CActualVaultLotsUids.class, S2CActualVaultLotsUids::new)
                .add(C2SGetVaultLotRequest.class, C2SGetVaultLotRequest::new)
        );
    }
}
