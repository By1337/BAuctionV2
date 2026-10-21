package dev.by1337.auc.search.filter;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.handler.index.Tag2IdService;
import org.bukkit.Material;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;

import static org.testng.Assert.*;

public class MaterialSearchTest {
    @Test
    public void plainMaterialUsesOnlyMaterialIndex() {
        SearchFilter filter = SearchFilterParser.parse("stone");
        assertTrue(filter instanceof MaterialSearchFilter);
        List<Integer> visited = new ArrayList<>();
        var index = new LotsIndexer() {
            @Override
            public NavigableSet<ClientAucLot> lotsSet(Sorting sorting) {
                throw new AssertionError("Global index must not be used");
            }

            @Override
            public NavigableSet<ClientAucLot> lotsSetByMaterial(int ordinal, Sorting sorting) {
                visited.add(ordinal);
                return Collections.emptyNavigableSet();
            }
        };
        assertNull(filter.searchLots(index, null).next());
        assertEquals(visited, List.of(Material.STONE.ordinal()));
    }

    @Test
    public void materialAlternativesUseMaterialFilter() {
        assertTrue(SearchFilterParser.parse("stone|dirt") instanceof MaterialsSearchFilter);
    }

    @Test
    public void alternativeWithoutMaterialKeepsGlobalSearch() {
        assertTrue(SearchFilterParser.parse("stone|custom_tag") instanceof ComplexSearchFilter);
        assertTrue(SearchFilterParser.parse("stone|!custom_tag") instanceof ComplexSearchFilter);
    }

    @Test
    public void exclusionsStayOnTheirOrBranches() {
        SearchFilter result = SearchFilterParser.parse("custom_a&!excluded_a|custom_b&!excluded_b");
        assertTrue(result instanceof ComplexSearchFilter);
        ComplexSearchFilter filter = (ComplexSearchFilter) result;
        assertEquals(filter.nots()[0], new int[]{Tag2IdService.INSTANCE.getId("excluded_a")});
        assertEquals(filter.nots()[1], new int[]{Tag2IdService.INSTANCE.getId("excluded_b")});
    }

    @Test
    public void finalNegativeOnlyBranchPreservesAlignment() {
        ComplexSearchFilter filter = (ComplexSearchFilter) SearchFilterParser.parse("custom_a|!excluded_b");
        assertNull(filter.ands()[1]);
        assertNull(filter.nots()[0]);
        assertEquals(filter.nots()[1], new int[]{Tag2IdService.INSTANCE.getId("excluded_b")});
    }
}
