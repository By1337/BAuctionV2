package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class SearchFilterAndList implements SearchFilter {
    private final List<SearchFilter> filters;

    public SearchFilterAndList(List<SearchFilter> filters) {
        this.filters = filters;
    }

    @Override
    public @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer) {
        BitSetPool.PooledBitSet result = null;
        for (SearchFilter filter : filters) {
            var v = filter.search(indexer);
            if (v != null) {
                if (v.isEmpty()) {
                    if (result != null) result.release();
                    return v;
                }
                if (result == null) {
                    result = v;
                } else {
                    result.and(v.lotMask());
                    v.release();
                }
            }
        }
        return result;
    }

    @Override
    public boolean matches(ClientItemStack itemStack) {
        if (filters.isEmpty()) return true;
        for (SearchFilter filter : filters) {
            if (!filter.matches(itemStack)) return false;
        }
        return false;
    }

    @Override
    public void forEachAnds(Consumer<int[]> consumer) {
        if (filters.isEmpty()) return;
        for (SearchFilter filter : filters) {
            filter.forEachAnds(consumer);
        }
    }
}
