package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.search.LotsResult;

import java.util.function.Consumer;

public record EmptySearchFilter() implements SearchFilter {
    public static final EmptySearchFilter INSTANCE = new EmptySearchFilter();



    public LotsResult searchLots(LotsIndexer indexer, Sorting sorting) {
        return LotsResult.of(indexer.lotsSet(sorting));
    }

    public LotsResult apply(LotsIndexer indexer, LotsResult upper){
        return upper;
    }

    @Override
    public boolean matches(ClientItemStack itemStack) {
        return false;
    }

    @Override
    public void forEachAnds(Consumer<int[]> consumer) {

    }
}
