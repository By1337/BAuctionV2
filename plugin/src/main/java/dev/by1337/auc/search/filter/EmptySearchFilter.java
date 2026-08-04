package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public record EmptySearchFilter() implements SearchFilter {
    public static final EmptySearchFilter INSTANCE = new EmptySearchFilter();

    @Override
    public @Nullable BitSetPool.PooledBitSet search(LotsIndexer indexer) {
        return null;
    }

    @Override
    public boolean matches(ClientItemStack itemStack) {
        return false;
    }

    @Override
    public void forEachAnds(Consumer<int[]> consumer) {

    }
}
