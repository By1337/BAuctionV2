package dev.by1337.auc.auc;

import dev.by1337.auc.auc.util.BatchedLotSubtractor;
import dev.by1337.auc.handler.Auction;
import dev.by1337.item.ItemModel;
import org.bukkit.inventory.ItemStack;

public interface BuyableLot extends LotData {

    int addToSubtractor(BatchedLotSubtractor subtractor, int count, Auction auction);

    default ItemStack bukkitItem(int count) {
        return itemStack().asQuantity(count);
    }

    default ItemModel itemModel() {
        return itemStack().itemModel();
    }

    ItemStackData itemStack();


}
