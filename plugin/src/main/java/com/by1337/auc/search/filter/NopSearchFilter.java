package com.by1337.auc.search.filter;

import com.by1337.auc.handler.index.BitSetPool;
import com.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

public record NopSearchFilter() implements SearchFilter {
    public static final NopSearchFilter INSTANCE = new NopSearchFilter();

    @Override
    public @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer) {
        return null;
    }
}
