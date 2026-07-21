package dev.by1337.auc.search;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.search.filter.PriceLimiterSearchFilter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;

public class SearchResult implements LotsResult {
    public static final SearchResult EMPTY = new SearchResult(0, null, Collections.emptyIterator(), -1);
    private final int size;
    private @Nullable BitSetPool.PooledBitSet filter;
    private final Iterator<ClientAucLot> iterator;
    private final long maxPrice;

    public SearchResult(int size, @Nullable BitSetPool.PooledBitSet filter, Iterator<ClientAucLot> iterator, long maxPrice) {
        this.size = size;
        this.filter = filter;
        this.iterator = iterator;
        this.maxPrice = maxPrice;
    }

    public @Nullable ClientAucLot next() {
        while (iterator.hasNext()) {
            var lot = iterator.next();
            var v = filter;
            if (v != null && !v.get(lot.shortId)) continue;
            if (maxPrice != -1 && lot.lprice() > maxPrice) continue;
            return lot;
        }
        return null;
    }

    @Override
    public void release() {
        var v = filter;
        filter = null;
        if (v != null) v.release();
    }

    public int size() {
        return size;
    }
}
