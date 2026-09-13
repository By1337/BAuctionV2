package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.handler.index.Tag2IdService;
import dev.by1337.auc.handler.index.search.SearchEngine;
import dev.by1337.auc.search.LotsResult;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class MaterialSearchFilter implements SearchFilter {
    public final Material material;
    public final int ordinal;
    public final int tagId;

    public MaterialSearchFilter(Material material) {
        this.material = material;
        ordinal = material.ordinal();
        tagId = Tag2IdService.INSTANCE.getId(material.getKey().value());
    }

    public LotsResult searchLots(LotsIndexer indexer, Sorting sorting) {
        return LotsResult.of(indexer.lotsSetByMaterial(ordinal, sorting));
    }

    @Override
    public LotsResult apply(LotsIndexer indexer, LotsResult upper) {
        return upper.filter(v -> v.itemStack().material().ordinal() == ordinal);
    }


    @Override
    public boolean matches(ClientItemStack itemStack) {
        return itemStack.materialOrdinal == ordinal;
    }

    @Override
    public void forEachAnds(Consumer<int[]> consumer) {
        consumer.accept(new int[]{tagId});
    }
}
