package dev.by1337.auc.search.filter;

import dev.by1337.auc.handler.index.search.SearchEngine;
import org.roaringbitmap.RoaringBitmap;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TagSearchTest {
    private static SearchEngine engine() {
        return new SearchEngine() {
            private final RoaringBitmap[] index = {null, RoaringBitmap.bitmapOf(7)};
            @Override public RoaringBitmap[] index() { return index; }
            @Override public RoaringBitmap used() { return RoaringBitmap.bitmapOf(7); }
        };
    }

    @Test
    public void missingRequiredTagAlwaysRejectsBranch() {
        SearchEngine engine = engine();
        for (int[] tags : new int[][]{{0, 1}, {1, 0}, {1, 50}, {50, 1}}) {
            var single = engine.findWithTags(tags, null);
            try { assertTrue(single.isEmpty()); } finally { single.release(); }
            var complex = engine.findWithTags(new int[][]{tags}, null);
            try { assertTrue(complex.isEmpty()); } finally { complex.release(); }
        }
    }
}
