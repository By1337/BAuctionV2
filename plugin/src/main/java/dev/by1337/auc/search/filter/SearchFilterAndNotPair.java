package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;

public record SearchFilterAndNotPair(int @Nullable [] and, int @Nullable [] not, String[] from) implements SearchFilter {
    @Override
    public @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer) {
        return indexer.findLotsWithTags(and, not);
    }

    @Override
    public boolean matches(ClientItemStack itemStack) {
        return itemStack.allOfTags(and) && itemStack.noneOfTags(not);
    }

    @Override
    public void forEachAnds(Consumer<int[]> consumer) {
        if (and == null) return;
        consumer.accept(and);
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
