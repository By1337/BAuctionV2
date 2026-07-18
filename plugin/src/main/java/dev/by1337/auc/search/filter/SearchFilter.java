package dev.by1337.auc.search.filter;

import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.handler.index.Tag2IdService;
import org.jetbrains.annotations.Nullable;

public interface SearchFilter {
    @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer);

    static SearchFilter ofTag(String tag){
        int id = Tag2IdService.INSTANCE.getId(tag);
        return new SearchFilterAndNotPair(new int[]{id}, null, new String[]{tag});
    }
}
