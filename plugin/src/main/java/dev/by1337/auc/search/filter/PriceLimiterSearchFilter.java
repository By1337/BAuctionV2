package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


public class PriceLimiterSearchFilter implements SearchFilter{
    private final SearchFilter delegated;
    public final long maxPrice;

    public PriceLimiterSearchFilter(SearchFilter delegated, long maxPrice) {
        this.delegated = delegated;
        this.maxPrice = maxPrice;
    }

    @Override
    public @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer) {
        return delegated.search(indexer);
    }

    @Override
    public boolean matches(ClientItemStack itemStack) {
        return delegated.matches(itemStack);
    }

    @Override
    public void forEachAnds(Consumer<int[]> consumer) {
        delegated.forEachAnds(consumer);
    }
}
