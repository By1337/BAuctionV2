package dev.by1337.auc.auc;

import dev.by1337.auc.util.DurationFormatter;
import dev.by1337.auc.util.number.NumberFormatter;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;

import java.util.UUID;

public interface LotData {
    PlaceholderResolver<LotData> PLACEHOLDERS = Placeholders.<LotData>create()
            .withContext("price", v -> NumberFormatter.format(v.dprice()))
            .withContext("seller_uuid", v -> v.owner().toString())
            .withContext("seller_name", LotData::ownerName)
            .withContext("expires", v -> DurationFormatter.getFormat(v.removalDate()))
            .withContext("price_for_one", v -> NumberFormatter.format(v.dprice_for_one()))
            .withContext("material", v -> v.itemStack().material().getKey().getKey())
            .withContext("count", LotData::count)
            .withContext("uid", LotData::uid)
            .withContext("item_name", v -> v.itemStack().itemName())
            .withContext("item_name_no_colors", v -> v.itemStack().itemNameNoColors())
            .withContext("lot_status", v -> v instanceof ClientVaultLot ? "<lang:bauctionv2.lot.status.vault>" : "<lang:bauctionv2.lot.status.lot>")
            ;

    ClientItemStack itemStack();

    int uid();

    UUID owner();

    int count();

    long removalDate();

    long lprice();

    double dprice();

    double dprice_for_one();

    String ownerName();

    default <T> PlaceholderResolver<T> placeholders() {
        return PLACEHOLDERS.bindCtx(this);
    }
}
