package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.handler.index.search.SearchEngine;
import dev.by1337.auc.search.LotsResult;

import java.util.Objects;
import java.util.function.Consumer;


public class PriceLimiterSearchFilter implements SearchFilter{
    private final SearchFilter delegated;
    public final long maxPrice;

    public PriceLimiterSearchFilter(SearchFilter delegated, long maxPrice) {
        this.delegated = delegated;
        this.maxPrice = maxPrice;
    }

    public LotsResult searchLots(LotsIndexer indexer, Sorting sorting) {
        return LotsResult.of(indexer.lotsSet(sorting)).filter(l -> l.centsPrice() <= maxPrice);
    }

    public LotsResult apply(LotsIndexer indexer, LotsResult upper){
        return upper.filter(l -> l.centsPrice() <= maxPrice);
    }


    @Override
    public boolean matches(ClientItemStack itemStack) {
        return delegated.matches(itemStack);
    }

    @Override
    public void forEachAnds(Consumer<int[]> consumer) {
        delegated.forEachAnds(consumer);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PriceLimiterSearchFilter that = (PriceLimiterSearchFilter) o;
        return maxPrice == that.maxPrice && Objects.equals(delegated, that.delegated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegated, maxPrice);
    }
}
