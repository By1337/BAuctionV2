package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.search.LotsResult;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class SearchFilterAndList implements SearchFilter {
    private final List<SearchFilter> filters;

    public SearchFilterAndList(List<SearchFilter> filters) {
        this.filters = filters;
    }

    public LotsResult searchLots(LotsIndexer indexer, Sorting sorting) {
        return apply(indexer, LotsResult.of(indexer.lotsSet(sorting)));
    }

    public LotsResult apply(LotsIndexer indexer, LotsResult upper) {
        for (SearchFilter filter : filters) {
            upper = filter.apply(indexer, upper);
        }
        return upper;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SearchFilterAndList that = (SearchFilterAndList) o;
        return Objects.equals(filters, that.filters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(filters);
    }
}
