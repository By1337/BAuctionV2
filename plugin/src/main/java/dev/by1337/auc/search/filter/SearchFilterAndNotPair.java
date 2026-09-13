package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.search.LotsResult;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public record SearchFilterAndNotPair(int @Nullable [] and, int @Nullable [] not,
                                     String[] from) implements SearchFilter {

/*    @Override
    public BitSetPool.PooledBitSet search(SearchEngine indexer) {
        return indexer.findWithTags(and, not);
    }*/

    public LotsResult searchLots(LotsIndexer indexer, Sorting sorting) {
        var set = indexer.lotsSet(sorting);
        return LotsResult.of(set.size(), indexer.findWithTags(and, not), set.iterator());
    }

    public LotsResult apply(LotsIndexer indexer, LotsResult upper) {
        return LotsResult.of(upper.size(), indexer.findWithTags(and, not), upper);
    }

    @Override
    public boolean matches(ClientItemStack itemStack) {
        return itemStack.allOfTags(and) && itemStack.noneOfTags(not);
    }

    @Override
    public void forEachAnds(Consumer<int[]> consumer) {
        if (and == null) return;
        consumer.accept(and);
    }

    @Override
    public String toString() {
        return "SearchFilterAndNotPair{" +
                "and=" + Arrays.toString(and) +
                ", not=" + Arrays.toString(not) +
                ", from=" + Arrays.toString(from) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SearchFilterAndNotPair that = (SearchFilterAndNotPair) o;
        return Objects.deepEquals(from, that.from) && Objects.deepEquals(and, that.and) && Objects.deepEquals(not, that.not);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(and), Arrays.hashCode(not), Arrays.hashCode(from));
    }
}
