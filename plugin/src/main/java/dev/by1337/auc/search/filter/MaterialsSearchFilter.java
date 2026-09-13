package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.handler.index.Tag2IdService;
import dev.by1337.auc.handler.index.search.SearchEngine;
import dev.by1337.auc.search.LotsResult;
import dev.by1337.auc.search.SearchResult;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MaterialsSearchFilter implements SearchFilter {
    public final Material[] materials;
    public final int[] ordinal;
    public final int[] tagId;
    private final IntSet ordinalSet;

    public MaterialsSearchFilter(Material[] materials) {
        this.materials = materials;
        ordinal = new int[materials.length];
        tagId = new int[materials.length];
        int x = 0;
        for (Material material : materials) {
            ordinal[x] = material.ordinal();
            tagId[x] = Tag2IdService.INSTANCE.getId(material.getKey().value());
            x++;
        }
        ordinalSet = new IntOpenHashSet(x);
        IntIterators.pour(IntIterators.wrap(ordinal), ordinalSet, Integer.MAX_VALUE);
    }

    public LotsResult searchLots(LotsIndexer indexer, Sorting sorting) {
        LotsResult latest = null;
        for (int i : ordinal) {
            var set = indexer.lotsSetByMaterial(i, sorting);
            if (set.isEmpty()) {
                continue;
            }
            var v = LotsResult.of(set);
            if (latest == null) {
                latest = v;
            } else {
                latest = LotsResult.and(latest, v, sorting.baseComparator());
            }
        }
        if (latest == null) return LotsResult.EMPTY;
        return latest;
    }

    @Override
    public LotsResult apply(LotsIndexer indexer, LotsResult upper) {
        return upper.filter(v ->  ordinalSet.contains(v.itemStack().material().ordinal()));
    }

    @Override
    public boolean matches(ClientItemStack itemStack) {
        return ordinalSet.contains(itemStack.materialOrdinal);
    }

    @Override
    public void forEachAnds(Consumer<int[]> consumer) {
        consumer.accept(tagId);
    }
}
