package dev.by1337.auc.auc;

import dev.by1337.item.ItemModel;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;

public interface ItemStackData {
    boolean matches(ItemStackDataImpl o);

    int maxStack();

    @Contract(pure = true)
    ItemStack itemStack();

    @Contract(pure = true)
    ItemStack asQuantity(int count);

    ItemModel itemModel();

    ItemModel itemModel(int count);

    Material material();

    String itemName();

    String itemNameNoColors();
}
