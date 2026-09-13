package dev.by1337.auc.auc;

import dev.by1337.auc.handler.index.Tag2IdService;
import dev.by1337.auc.tag.TagsExtractor;
import dev.by1337.core.BCore;
import dev.by1337.item.ItemModel;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ClientItemStack implements ItemStackData {
    private final int id;
    private final byte[] bytes;
    private final int[] tags;
    private final IntSet tagsSet;
    private final ItemStackData itemData;
    public final int materialOrdinal;

    public ClientItemStack(int id, byte[] bytes, ItemStack itemStack, ItemModel itemModel, int[] tags) {
        this.id = id;
        this.bytes = bytes;
        this.tags = tags;
        itemData = new ItemStackDataImpl(itemModel, itemStack);
        tagsSet = new IntOpenHashSet(tags.length);
        IntIterators.pour(IntIterators.wrap(tags), tagsSet, Integer.MAX_VALUE);
        materialOrdinal = itemStack.getType().ordinal();
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

    public boolean noneOfTags(int @Nullable [] arr) {
        if (arr == null) return true;
        for (int i : arr) {
            if (tagsSet.contains(i)) return false;
        }
        return true;
    }

    public boolean allOfTags(int @Nullable [] and) {
        if (and == null) return true;
        for (int i : and) {
            if (!tagsSet.contains(i)) return false;
        }
        return true;
    }

    @Contract(pure = true)
    public ItemStack itemStack() {
        return itemData.itemStack();
    }

    @Contract(pure = true)
    public ItemStack asQuantity(int count) {
        return itemData.asQuantity(count);
    }

    public int id() {
        return id;
    }

    public byte[] bytes() {
        return bytes;
    }

    @Override
    public ItemModel itemModel() {
        return itemData.itemModel();
    }

    @Override
    public ItemModel itemModel(int count) {
        return itemData.itemModel().withAmount(count);
    }

    public int[] tags() {
        return tags;
    }

    @Override
    public Material material() {
        return itemData.material();
    }

    @Override
    public String itemName() {
        return itemData.itemName();
    }

    @Override
    public String itemNameNoColors() {
        return itemData.itemNameNoColors();
    }

    public ItemStackData itemData() {
        return itemData;
    }

    @Override
    public boolean matches(ItemStackDataImpl o) {
        return itemData.matches(o);
    }

    @Override
    public int maxStack() {
        return itemData.maxStack();
    }

    @Override
    public String toString() {
        return "ClientItemStack{" +
                "id=" + id +
                ", bytes=" + Arrays.toString(bytes) +
                ", itemData=" + itemData +
                ", tags=" + Arrays.toString(tags) +
                '}';
    }
}
