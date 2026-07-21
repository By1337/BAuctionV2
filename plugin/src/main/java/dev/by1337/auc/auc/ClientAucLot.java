package dev.by1337.auc.auc;

import dev.by1337.auc.common.auc.AucLot;
import dev.by1337.auc.handler.name.PlayerName;

import java.util.UUID;

public class ClientAucLot implements LotData {
    /*    private static final DecimalFormat df = new DecimalFormat("#,###");
        private static final DecimalFormat simple = new DecimalFormat("#");
        public static final PlaceholderResolver<ClientAucLot> PLACEHOLDERS = Placeholders.<ClientAucLot>create()
                .withContext("price", v -> v.dprice < 1000 ? simple.format(v.dprice) : df.format(v.dprice))
                .withContext("seller_uuid", v -> v.lot.owner().toString())
                .withContext("seller_name", v -> v.playerName.name())
                .withContext("expires", v -> TimeUtil.getFormat(v.lot.removalDate()))
                .withContext("price_for_one", v -> v.dprice_for_one < 1000 ? simple.format(v.dprice_for_one) : df.format(v.dprice_for_one))
                .withContext("material", v -> v.itemStack.material().getKey().getKey())
                .withContext("count", v -> v.lot.count())
                .withContext("uid", v -> v.lot.uid())
                .withContext("item_name", v -> v.itemStack.itemName())
                .withContext("item_name_no_colors", v -> v.itemStack.itemNameNoColors())

                ;*/
    public final AucLot lot;
    public final ClientItemStack itemStack;
    public final int shortId;
    public final double dprice;
    public final double dprice_for_one;
    public final long lprice_for_one;
    public final PlayerName playerName;

    public ClientAucLot(AucLot lot, ClientItemStack itemStack, int shortId, PlayerName playerName) {
        this.lot = lot;
        this.itemStack = itemStack;
        this.shortId = shortId;
        dprice = lot.lprice() / 100D;
        dprice_for_one = ((double) lot.lprice() / lot.count()) / 100D;
        lprice_for_one = lot.lprice_for_one;
        this.playerName = playerName;
    }

    public int uid() {
        return lot.uid();
    }

    public UUID owner() {
        return lot.owner();
    }

    public long createdDate() {
        return lot.createdDate();
    }

    public long removalDate() {
        return lot.removalDate();
    }

    public int count() {
        return lot.count();
    }

    public long lprice() {
        return lot.lprice();
    }

    public double dprice() {
        return dprice;
    }

    public double dprice_for_one() {
        return dprice_for_one;
    }

    @Override
    public ClientItemStack itemStack() {
        return itemStack;
    }

    public AucLot lot() {
        return lot;
    }
    //        registerPlaceholder("{item_name}", this::getItem_name);
    //        registerPlaceholder("{item_name_no_colors}", this::getItem_name_no_colors);
    //        registerPlaceholder("{seller_is_online}", () -> Bukkit.getPlayer(sellerUuid) != null);
    //        registerPlaceholder("{seller_is_online_format}", () ->
    //                (BAuction.playerList().getPlayer(sellerUuid) != null) ?
    //                        Lang.getMessage("online-seller") : Lang.getMessage("offline-seller")
    //        );

    //    }


    @Override
    public String ownerName() {
        return playerName.name();
    }

    @Override
    public String toString() {
        return "ClientAucLot{" +
                "lot=" + lot +
                ", itemStack=" + itemStack +
                ", shortId=" + shortId +
                ", price=" + dprice +
                ", price_for_one=" + dprice_for_one +
                ", lprice_for_one=" + lprice_for_one +
                '}';
    }
}
