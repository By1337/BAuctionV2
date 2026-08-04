package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


public record ComplexSearchFilter(
        int @Nullable [] @Nullable [] ands,
        int @Nullable [] @Nullable [] nots
) implements SearchFilter {
    @Override
    public @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer) {
        return indexer.findLotsWithTags(ands, nots);
    }

    @Override
    public boolean matches(ClientItemStack itemStack) {
        int filtersCount = Math.max(ands != null ? ands.length : 0, nots != null ? nots.length : 0);
        if (filtersCount == 0) return false;
        for (int i = 0; i < filtersCount; i++) {
            int[] and = safeGet(i, ands);
            int[] not = safeGet(i, nots);
            if (itemStack.allOfTags(and) && itemStack.noneOfTags(not)) return true;
        }
        return false;
    }

    @Override
    public void forEachAnds(Consumer<int[]> consumer) {
        if (ands == null) return;
        for (int[] and : ands) {
            if (and == null) continue;
            consumer.accept(and);
        }
    }

    private static <T> @Nullable T safeGet(int i, @Nullable T[] arr) {
        if (arr == null || i < 0 || i >= arr.length) {
            return null;
        }
        return arr[i];
    }

}
