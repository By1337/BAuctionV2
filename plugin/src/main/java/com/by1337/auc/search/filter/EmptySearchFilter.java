package com.by1337.auc.search.filter;

import com.by1337.auc.handler.index.BitSetPool;
import com.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

public record EmptySearchFilter() implements SearchFilter {
    public static final EmptySearchFilter INSTANCE = new EmptySearchFilter();

    @Override
    public @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer) {
        return null;
    }
}
