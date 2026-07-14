package com.by1337.auc.search.filter;

import com.by1337.auc.handler.index.BitSetPool;
import com.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SearchFilterList implements SearchFilter {
    private final List<SearchFilter> filters;

    public SearchFilterList(List<SearchFilter> filters) {
        this.filters = filters;
    }

    @Override
    public @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer) {
        BitSetPool.PooledBitSet result = null;
        for (SearchFilter filter : filters) {
            var v = filter.search(indexer);
            if (v != null && !v.isEmpty()) {
                if (result == null) {
                    result = v;
                } else {
                    result.or(v.lotMask());
                    v.release();
                }
            }
        }
        return result;
    }
}
