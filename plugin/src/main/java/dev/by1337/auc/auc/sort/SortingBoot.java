package dev.by1337.auc.auc.sort;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.LotData;
import dev.by1337.auc.registry.AucRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SortingBoot {
    private static final Map<String, Sorting> map = new HashMap<>();
    private static final Sorting NEWEST = make("newest", Comparator.<ClientAucLot>comparingLong(v -> v.lot.createdDate()).reversed(), (v, v1) -> 0);
    private static final Sorting OLDEST = make("oldest", NEWEST.comparator().reversed(), (v, v1) -> 0);
    private static final Sorting CHEAPER = make("cheaper", Comparator.<ClientAucLot>comparingLong(v -> v.pricer().centsPriceForOne), Comparator.comparingLong(l -> l.pricer().centsPriceForOne));
    private static final Sorting EXPENSIVE = make("expensive", CHEAPER.comparator().reversed(), CHEAPER.baseComparator().reversed());
    private static final Logger log = LoggerFactory.getLogger(SortingBoot.class);

    public static void boot(AucRegistry<Sorting> registry, List<String> usedSort) {
        for (String s : usedSort) {
            var sorting = map.get(s);
            if (sorting == null) {
                log.error("Unknown sorting {}", s);
                continue;
            }
            registry.register(sorting.id(), sorting);
        }
        if (registry.size() == 0) {
            registry.register(NEWEST.id(), NEWEST);
        }
    }

    private static Sorting make(String key, Comparator<ClientAucLot> comparator, Comparator<LotData> baseComparator) {
        var s = new Sorting(key, (v, v1) -> {
            int byUid = Integer.compare(v.lot.uid(), v1.lot.uid());
            if (byUid == 0) return 0;
            var x = comparator.compare(v, v1);
            if (x == 0) return byUid;
            return x;
        }, baseComparator);
        map.put(key, s);
        return s;
    }
}
