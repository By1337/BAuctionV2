package dev.by1337.auc.auc.sort;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.LotData;

import java.util.Comparator;

public record Sorting(String id, Comparator<ClientAucLot> comparator, Comparator<LotData> baseComparator) {
    @Deprecated
    public Sorting(String id, Comparator<ClientAucLot> comparator) {
        this(id, comparator, Comparator.comparingInt(LotData::count));
    }
}
