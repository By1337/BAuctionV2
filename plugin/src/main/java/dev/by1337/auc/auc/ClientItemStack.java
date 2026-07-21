package dev.by1337.auc.auc;

import dev.by1337.auc.handler.index.Tag2IdService;
import dev.by1337.auc.tag.TagsExtractor;
import dev.by1337.core.BCore;
import dev.by1337.item.ItemModel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ClientItemStack {
    private final int id;
    private final byte[] bytes;
    private final ItemStack itemStack;
    private final ItemModel itemModel;
    private final int[] tags;
    private final Material material;
    private final String itemName;
    private final String itemNameNoColors;

    public ClientItemStack(int id, byte[] bytes, ItemStack itemStack, ItemModel itemModel, int[] tags) {
        this.id = id;
        this.bytes = bytes;
        this.itemStack = itemStack;
        material = itemStack.getType();
        this.itemModel = itemModel;
        this.tags = tags;
        itemName = itemName(itemStack);
        itemNameNoColors = itemNameNoColors(itemStack);
    }

    public static ClientItemStack make(int id, byte[] bytes, TagsExtractor extractor, Tag2IdService tag2id) {
        ItemStack itemStack = BCore.getItemStackSerializer().deserialize(bytes, null);
        var s_tags = extractor.extractTags(itemStack);
        int[] tags = new int[s_tags.size()];
        int x = 0;
        for (String sTag : s_tags) {
            tags[x++] = tag2id.getId(sTag);
        }
        return new ClientItemStack(id, bytes, itemStack, ItemModel.fromItemStack(itemStack), tags);
    }

    @Contract(pure = true)
    public ItemStack itemStack() {
        return itemStack.clone();
    }

    @Contract(pure = true)
    public ItemStack asQuantity(int count) {
        return itemStack.asQuantity(count);
    }

    public int id() {
        return id;
    }

    public byte[] bytes() {
        return bytes;
    }

    public ItemModel itemModel() {
        return itemModel;
    }

    public ItemModel itemModel(int count) {
        return itemModel.withAmount(count);
    }

    public int[] tags() {
        return tags;
    }

    public Material material() {
        return material;
    }

    public String itemName() {
        return itemName;
    }

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
        return "ClientItemStack{" +
                "id=" + id +
                ", bytes=" + Arrays.toString(bytes) +
                ", itemStack=" + itemStack +
                ", itemModel=" + itemModel +
                ", tags=" + Arrays.toString(tags) +
                ", material=" + material +
                ", itemName='" + itemName + '\'' +
                ", itemNameNoColors='" + itemNameNoColors + '\'' +
                '}';
    }
}
