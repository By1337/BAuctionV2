package dev.by1337.auc.search;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.handler.index.BitSetPool;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;

public class SearchResult implements LotsResult {
    public static final SearchResult EMPTY = new SearchResult(0, null, Collections.emptyIterator());
    private final int size;
    private @Nullable BitSetPool.PooledBitSet filter;
    private final Iterator<ClientAucLot> iterator;

    public SearchResult(int size, @Nullable BitSetPool.PooledBitSet filter, Iterator<ClientAucLot> iterator) {
        this.size = size;
        this.filter = filter;
        this.iterator = iterator;
    }

    public @Nullable ClientAucLot next() {
        while (iterator.hasNext()) {
            var lot = iterator.next();
            var v = filter;
            if (v != null && !v.get(lot.shortId)) continue;
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
