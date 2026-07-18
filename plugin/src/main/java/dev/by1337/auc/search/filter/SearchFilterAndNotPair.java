package dev.by1337.auc.search.filter;

import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public record SearchFilterAndNotPair(int @Nullable [] and, int @Nullable [] not, String[] from) implements SearchFilter {
    @Override
    public @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer) {
        return indexer.findLotsWithTags(and, not);
    }

    @Override
    public String toString() {
        return "SearchFilterAndNotPair{" +
                "and=" + Arrays.toString(and) +
                ", not=" + Arrays.toString(not) +
                ", from=" + Arrays.toString(from) +
                '}';
    }
}
