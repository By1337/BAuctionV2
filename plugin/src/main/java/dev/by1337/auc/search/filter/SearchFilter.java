package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.handler.index.Tag2IdService;
import dev.by1337.auc.handler.index.search.SearchEngine;
import dev.by1337.auc.search.LotsResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface SearchFilter {
    //@Nullable BitSetPool.PooledBitSet search(SearchEngine indexer);

    boolean matches(ClientItemStack itemStack);

    void forEachAnds(Consumer<int[]> consumer);

    LotsResult searchLots(LotsIndexer indexer, Sorting sorting);

    LotsResult apply(LotsIndexer indexer, LotsResult upper);

    static SearchFilter ofTag(String tag){
        int id = Tag2IdService.INSTANCE.getId(tag);
        return new SearchFilterAndNotPair(new int[]{id}, null, new String[]{tag});
    }

    default SearchFilter and(SearchFilter o){
        var self = this;
        return new SearchFilter() {
            @Override
            public boolean matches(ClientItemStack itemStack) {
                return self.matches(itemStack) && o.matches(itemStack);
            }

            @Override
            public void forEachAnds(Consumer<int[]> consumer) {
                self.forEachAnds(consumer);
                o.forEachAnds(consumer);
            }

            @Override
            public LotsResult searchLots(LotsIndexer indexer, Sorting sorting) {
                return o.apply(indexer, self.searchLots(indexer, sorting));
            }

            @Override
            public LotsResult apply(LotsIndexer indexer, LotsResult upper) {
                return o.apply(indexer, self.apply(indexer, upper));
            }
        };
    }
}
