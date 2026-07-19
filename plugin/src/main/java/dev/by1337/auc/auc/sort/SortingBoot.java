package dev.by1337.auc.auc.sort;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.registry.AucRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SortingBoot {
    private static final Map<String, Sorting> map = new HashMap<>();
    private static final Sorting NEWEST = make("newest", Comparator.<ClientAucLot>comparingLong(v -> v.lot.createdDate()).reversed());
    private static final Sorting OLDEST = make("oldest", NEWEST.comparator().reversed());
    private static final Sorting CHEAPER = make("cheaper", Comparator.<ClientAucLot>comparingLong(v -> v.lprice_for_one));
    private static final Sorting EXPENSIVE = make("expensive", CHEAPER.comparator().reversed());
    private static final Logger log = LoggerFactory.getLogger(SortingBoot.class);

    public static void boot(AucRegistry<Sorting> registry, List<String> usedSort) {
        for (String s : usedSort) {
            var sorting = map.get(s);
            if (sorting == null){
                log.error("Unknown sorting {}", s);
                continue;
            }
            registry.register(sorting.id(), sorting);
        }
        if (registry.size() == 0) {
            registry.register(NEWEST.id(), NEWEST);
        }
    }

    private static Sorting make(String key, Comparator<ClientAucLot> comparator) {
        var s = new Sorting(key, (v, v1) -> {
            var x = comparator.compare(v, v1);
            if (x == 0) return Integer.compare(v.lot.uid(), v1.lot.uid());
            return x;
        });
        map.put(key, s);
        return s;
    }
}
