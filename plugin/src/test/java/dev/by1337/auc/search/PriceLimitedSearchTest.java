package dev.by1337.auc.search;

import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.LotData;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.search.filter.PriceLimiterSearchFilter;
import dev.by1337.auc.search.filter.SearchFilter;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Consumer;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;

public class PriceLimitedSearchTest {
    @Test
    public void filterSkipsRejectedLotsThroughoutTheResult() {
        LotData expensive = lot(201);
        LotData cheap = lot(100);
        LotData atLimit = lot(200);
        LotsResult result = LotsResult.memoized(List.of(expensive, cheap, expensive, atLimit, expensive))
                .filter(l -> l.centsPrice() <= 200);

        assertSame(result.next(), cheap);
        assertSame(result.next(), atLimit);
        assertNull(result.next());
    }

    @Test
    public void searchUsesItemFilterBeforeApplyingPriceLimit() {
        LotData expensive = lot(201);
        LotData cheap = lot(100);
        LotData atLimit = lot(200);
        SearchFilter filter = new PriceLimiterSearchFilter(itemFilter(List.of(expensive, cheap, atLimit)), 200);

        LotsResult result = filter.searchLots(null, null);

        assertSame(result.next(), cheap);
        assertSame(result.next(), atLimit);
        assertNull(result.next());
    }

    @Test
    public void applyPreservesItemFilterAndSkipsUnrelatedLots() {
        LotData unrelated = lot(50);
        LotData expensive = lot(201);
        LotData cheap = lot(100);
        SearchFilter filter = new PriceLimiterSearchFilter(itemFilter(List.of(expensive, cheap)), 200);

        LotsResult result = filter.apply(null, LotsResult.memoized(List.of(unrelated, expensive, cheap, unrelated)));

        assertSame(result.next(), cheap);
        assertNull(result.next());
    }

    @Test
    public void noAffordableLotsProducesEmptyResult() {
        SearchFilter filter = new PriceLimiterSearchFilter(itemFilter(List.of(lot(201), lot(300))), 200);

        assertNull(filter.searchLots(null, null).next());
    }

    private static SearchFilter itemFilter(List<LotData> matchingLots) {
        return new SearchFilter() {
            @Override
            public boolean matches(ClientItemStack itemStack) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forEachAnds(Consumer<int[]> consumer) {
                throw new UnsupportedOperationException();
            }

            @Override
            public LotsResult searchLots(LotsIndexer indexer, Sorting sorting) {
                return LotsResult.memoized(matchingLots);
            }

            @Override
            public LotsResult apply(LotsIndexer indexer, LotsResult upper) {
                return upper.filter(l -> matchingLots.stream().anyMatch(matching -> matching == l));
            }
        };
    }

    private static LotData lot(long centsPrice) {
        return (LotData) Proxy.newProxyInstance(LotData.class.getClassLoader(), new Class<?>[]{LotData.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("centsPrice")) return centsPrice;
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
