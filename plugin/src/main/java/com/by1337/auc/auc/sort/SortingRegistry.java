package com.by1337.auc.auc.sort;

import com.by1337.auc.auc.ClientAucLot;
import com.by1337.auc.util.CyclicListIterator;

import java.util.*;

public class SortingRegistry {
    private static final List<Sorting> list = new ArrayList<>();
    public static final Sorting NEWEST = register("newest", Comparator.<ClientAucLot>comparingLong(v -> v.lot.createdDate()).reversed());
    //сначала старые не понятно нахуя нужны
    public static final Sorting CHEAPER = register("cheaper", Comparator.<ClientAucLot>comparingLong(v -> v.lprice_for_one));
    //public static final Sorting EXPENSIVE = register("expensive", CHEAPER.comparator().reversed()); //сомнительный


    public static List<Sorting> sortings() {
        return Collections.unmodifiableList(list);
    }

    public static CyclicListIterator<Sorting> cycle() {
        return new CyclicListIterator<>(list);
    }

    private static Sorting register(String key, Comparator<ClientAucLot> comparator) {
        int id = list.size();
        Sorting s = new Sorting(id, key, (v, v1) -> {
            if (v.lot.uid() == v1.lot.uid()) return 0;
            var x = comparator.compare(v, v1);
            if (x == 0) return Integer.compare(v.lot.uid(), v1.lot.uid());
            return x;
        });
        list.add(s);
        return s;
    }
}
