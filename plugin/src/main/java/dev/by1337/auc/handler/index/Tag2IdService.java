package dev.by1337.auc.handler.index;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Tag2IdService {
    public static final Tag2IdService INSTANCE = new Tag2IdService();

    private final AtomicInteger counter = new AtomicInteger();
    private final Map<String, Integer> tag2id = new ConcurrentHashMap<>();
    private final Int2ObjectOpenHashMap<Material> tagId2material = new Int2ObjectOpenHashMap<>();

    public Tag2IdService() {
        for (Material material : Registry.MATERIAL) {
            tagId2material.put(getId(material.getKey().value()), material);
        }
    }

    public @Nullable Material getMaterial(int tag) {
        return tagId2material.get(tag);
    }

    public int[] getIds(Collection<String> tag) {
        int[] res = new int[tag.size()];
        int i = 0;
        for (String s : tag) {
            res[i++] = getId(s);
        }
        return res;
    }

    public int getId(String tag) {
        return tag2id.computeIfAbsent(tag.toLowerCase(Locale.ROOT), ignored -> counter.getAndIncrement());
    }
}
