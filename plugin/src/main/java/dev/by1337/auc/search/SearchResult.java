package dev.by1337.auc.search;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.LotData;
import org.jetbrains.annotations.Nullable;

public class SearchResult implements LotsResult {
    public static final SearchResult EMPTY = new SearchResult(null);
    private LotsResult result;

    public SearchResult(LotsResult result) {
        this.result = result;
    }

    @Override
    public int size() {
        return result == null ? 0 : result.size();
    }

    public @Nullable ClientAucLot next() {
        var v = result;
        if (v == null) return null;
        LotData data;
        while ((data = v.next()) != null) {
            if (data instanceof ClientAucLot l) return l;
        }
        return null;
    }

    @Override
    public void release() {
        var v = result;
        result = null;
        if (v != null) v.release();
    }

}
