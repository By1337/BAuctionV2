package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.handler.index.Tag2IdService;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface SearchFilter {
    @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer);

    boolean matches(ClientItemStack itemStack);

    void forEachAnds(Consumer<int[]> consumer);

    static SearchFilter ofTag(String tag){
        int id = Tag2IdService.INSTANCE.getId(tag);
        return new SearchFilterAndNotPair(new int[]{id}, null, new String[]{tag});
    }
}
