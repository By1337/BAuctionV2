package dev.by1337.auc.auc;

import dev.by1337.auc.auc.util.LotPricer;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.util.number.NumberFormatter;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface LotData {

    ItemStackData itemStack();

    @Nullable LotData update(Auction auction);

    default int normalCount() {
        return Math.min(count(), itemStack().maxStack());
    }

    default int shortIdOr(int def){
        return def;
    }

    int count();

    default int minimum() {
        return 1;
    }

    default boolean isOwner(UUID uuid) {
        return false;
    }

    LotPricer pricer();

    <T> PlaceholderResolver<T> placeholders();

    static <T extends LotData> Placeholders<T> createPlaceholders() {
        return Placeholders.<T>create()
                .withContext("price", v -> NumberFormatter.format(v.pricer().price))
                // .withContext("seller_uuid", v -> v.owner().toString())
                // .withContext("seller_name", LotData::ownerName)
                //.withContext("expires", v -> DurationFormatter.getFormat(v.removalDate()))
                .withContext("price_for_one", v -> NumberFormatter.format(v.pricer().priceForOne))
                .withContext("material", v -> v.itemStack().material().getKey().getKey())
                .withContext("count", LotData::normalCount)
                .withContext("max_count", LotData::count)
                //  .withContext("uid", LotData::uid)
                .withContext("item_name", v -> v.itemStack().itemName())
                .withContext("item_name_no_colors", v -> v.itemStack().itemNameNoColors())
                .withContext("lot_status", v -> v instanceof ClientVaultLot ? "<lang:bauctionv2.lot.status.vault>" : "<lang:bauctionv2.lot.status.lot>")
                ;
    }


    default long centsPrice() {
        return pricer().centsFor(count());
    }
    default long centsForOne() {
        return pricer().centsForOne();
    }

    default double price() {
        return pricer().priceFor(count());
    }

    default double priceForOne() {
        return pricer().priceForOne();
    }


    @Deprecated
    default long lprice() {
        return pricer().centsFor(count());
    }

    @Deprecated
    default long lprice_for_one() {
        return pricer().centsForOne();
    }

    @Deprecated
    default double dprice() {
        return pricer().priceFor(count());
    }

    @Deprecated
    default double dprice_for_one() {
        return pricer().priceForOne();
    }

}
