package dev.by1337.auc.auc;

import dev.by1337.item.ItemModel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Objects;

public class ItemStackDataImpl implements ItemStackData {
    private final ItemModel model;
    private final ItemStack itemStack;
    private final Material material;
    private final String itemName;
    private final String itemNameNoColors;
    private final int maxStack;

    public ItemStackDataImpl(ItemModel model, ItemStack itemStack) {
        this.model = model;
        this.itemStack = itemStack;
        maxStack = itemStack.getMaxStackSize();
        material = itemStack.getType();
        itemName = itemName(itemStack);
        itemNameNoColors = itemNameNoColors(itemStack);
    }

    @Override
    public boolean matches(ItemStackDataImpl o) {
        if (maxStack != o.maxStack) return false;
        return itemStack.isSimilar(o.itemStack);
    }

    @Override
    public int maxStack() {
        return maxStack;
    }

    @Contract(pure = true)
    @Override
    public ItemStack itemStack() {
        return itemStack.clone();
    }

    @Contract(pure = true)
    @Override
    public ItemStack asQuantity(int count) {
        return itemStack.asQuantity(count);
    }

    @Override
    public ItemModel itemModel() {
        return model;
    }

    @Override
    public ItemModel itemModel(int count) {
        return model.withAmount(count);
    }

    @Override
    public Material material() {
        return material;
    }

    @Override
    public String itemName() {
        return itemName;
    }

    @Override
    public String itemNameNoColors() {
        return itemNameNoColors;
    }

    private static String itemName(ItemStack itemStack) {
        var meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            String color;
            if (meta != null && meta.hasRarity()) {
                color = "<" + meta.getRarity().color().asHexString() + ">";
            } else {
                color = "";
            }
            return color + "<lang:" + itemStack.getType().translationKey() + ">";
        } else {
            //<lang_or:_key_:_fallback_:_value1_:_value2_...>
            //<lang:_key_:_value1_:_value2_...>
            var name = meta.displayName();
            if (name instanceof TranslatableComponent t) {
                var args = t.arguments();
                var fallback = t.fallback();
                if (fallback != null) {
                    return "<lang_or:" + t.key() + ":" + fallback + toArguments(args) + ">";
                }
                return "<lang:" + t.key() + toArguments(args) + ">";
            }
            return meta.getDisplayName();
        }
    }

    private static String toArguments(List<TranslationArgument> args) {
        if (args.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(":");
        for (TranslationArgument arg : args) {
            sb.append(arg.value()).append(":");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static String itemNameNoColors(ItemStack itemStack) {
        var meta = itemStack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return "<lang:" + itemStack.getType().translationKey() + ">";
        } else {
            var displayName = Objects.requireNonNullElse(meta.displayName(), Component.text(""));
            return PlainTextComponentSerializer.plainText().serialize(displayName);
        }
    }

    @Override
    public String toString() {
        return "ItemStackData{" +
                "model=" + model +
                ", itemStack=" + itemStack +
                ", material=" + material +
                ", itemName='" + itemName + '\'' +
                ", itemNameNoColors='" + itemNameNoColors + '\'' +
                '}';
    }
}
