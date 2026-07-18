package dev.by1337.auc.search.filter;

import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;


public record ComplexSearchFilter(
        int @Nullable [] @Nullable [] ands,
        int @Nullable [] @Nullable [] nots
) implements SearchFilter {
    @Override
    public @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer) {
        return indexer.findLotsWithTags(ands, nots);
    }
}
