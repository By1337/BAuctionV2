package com.by1337.auc.search.filter;

import com.by1337.auc.handler.index.BitSetPool;
import com.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

public interface SearchFilter {
    @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer);
}
