package com.by1337.auc.handler.index;

import com.by1337.auc.auc.ClientAucLot;
import com.by1337.auc.auc.ClientVaultLot;
import com.by1337.auc.auc.sort.Sorting;
import com.by1337.auc.auc.sort.SortingRegistry;
import com.by1337.auc.handler.index.search.PlayerVaultResult;
import com.by1337.auc.handler.index.search.SearchResult;
import com.by1337.auc.pipeline.LocalChannelContext;
import com.by1337.auc.pipeline.LocalChannelHandler;
import com.by1337.auc.pipeline.LocalPipeline;
import com.by1337.auc.pipeline.Remote;
import com.by1337.auc.search.filter.SearchFilter;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.work.EventLoopWorker;
import it.unimi.dsi.fastutil.longs.LongArrayPriorityQueue;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

public class LotsIndexer implements LocalChannelHandler {
    private static final Logger log = LoggerFactory.getLogger(LotsIndexer.class);
    private static final long REUSE_GRANULARITY_NS = TimeUnit.SECONDS.toNanos(5);
    private static final int REUSE_DELAY_QUANTA = (int) (TimeUnit.SECONDS.toNanos(30) / REUSE_GRANULARITY_NS);

    private final LongArrayPriorityQueue freeIds = new LongArrayPriorityQueue();
    private int shortIdCounter;
    private BitSet[] index = new BitSet[2048];
    private final BitSet used = new BitSet();
    private EventLoopWorker eventLoop;
    private final ConcurrentSkipListMap<ClientAucLot, Boolean>[] sorted;
    private final Object2ObjectOpenHashMap<UUID, ConcurrentSkipListMap<ClientVaultLot, Boolean>> owner2vaultLots = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<UUID, BitSet> owner2ownedLots = new Object2ObjectOpenHashMap<>();

    public LotsIndexer() {
        var list = SortingRegistry.sortings();
        //noinspection unchecked
        sorted = new ConcurrentSkipListMap[list.size()];
        for (Sorting sorting : list) {
            sorted[sorting.id()] = new ConcurrentSkipListMap<>(sorting.comparator());
        }
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        ctx.fire(msg);
    }

    @Override
    public void init(LocalPipeline pipeline, Remote remote) {
        eventLoop = pipeline.eventLoop();
    }


    public SearchResult search(UUID owner, @Nullable SearchFilter filter, Sorting sorting) {
        var ownerMask = owner2ownedLots.get(owner);
        if (ownerMask == null) return SearchResult.EMPTY;
        @Nullable BitSetPool.PooledBitSet mask = filter != null ? filter.search(this) : null;
        if (mask == null || mask.isEmpty()) {
            mask = BitSetPool.get(ownerMask);
        } else {
            mask.and(ownerMask);
        }
        var set = sorted[sorting.id()];
        return new SearchResult(mask.cardinality(), mask, set.navigableKeySet().iterator());
    }

    public SearchResult search(@Nullable SearchFilter filter, Sorting sorting) {
        @Nullable BitSetPool.PooledBitSet mask = filter != null ? filter.search(this) : null;
        var set = sorted[sorting.id()];
        return new SearchResult(mask != null ? mask.cardinality() : set.size(), mask, set.navigableKeySet().iterator());
    }

    public @Nullable BitSetPool.PooledBitSet findLotsWithTags(int @Nullable [] and, int @Nullable [] not) {
        if (and == null && not == null) return null;
        BitSetPool.PooledBitSet base = null;
        var index = this.index;
        int maxIndex = index.length - 1;
        if (and == null) {
            base = BitSetPool.get(used);
        } else {
            for (int i : and) {
                if (maxIndex < i) continue;
                var set = index[i];
                if (set == null) continue;
                if (base == null) {
                    base = BitSetPool.get(set);
                } else {
                    base.and(set);
                }
            }
        }
        if (base == null) return BitSetPool.empty();
        if (not != null) {
            for (int i : not) {
                if (maxIndex < i) continue;
                var set = index[i];
                if (set == null) continue;
                base.andNot(set);
            }
        }
        return base;
    }

    private static <T> @Nullable T safeGet(int i, @Nullable T[] arr) {
        if (arr == null || i < 0 || i >= arr.length) {
            return null;
        }
        return arr[i];
    }

    public @Nullable BitSetPool.PooledBitSet findLotsWithTags(
            int @Nullable [] @Nullable [] ands,
            int @Nullable [] @Nullable [] nots
    ) {
        int filtersCount = Math.max(ands != null ? ands.length : 0, nots != null ? nots.length : 0);
        if (filtersCount == 0) return null;
        BitSetPool.PooledBitSet base = null;
        var index = this.index;
        var buffer = BitSetPool.get(null);
        try {
            for (int i = 0; i < filtersCount; i++) {
                buffer.clear();
                int[] and = safeGet(i, ands);
                int[] not = safeGet(i, nots);
                if (and == null) {
                    buffer.copy(used);
                } else {
                    boolean init = false;
                    for (int idx : and) {
                        var set = safeGet(idx, index);
                        if (set == null) continue;
                        if (!init) {
                            buffer.copy(set);
                            init = true;
                        } else {
                            buffer.and(set);
                        }
                    }
                    if (!init) {
                        //has no lots
                        continue;
                    }
                }
                if (buffer.isEmpty()) continue;
                if (not != null) {
                    for (int idx : not) {
                        var set = safeGet(idx, index);
                        if (set != null)
                            buffer.andNot(set);
                    }
                }
                if (buffer.isEmpty()) continue;
                if (base == null) {
                    base = BitSetPool.get(buffer.lotMask());
                } else {
                    base.or(buffer.lotMask());
                }
            }
        } finally {
            buffer.release();
        }
        return base;
    }

    public void removeLot(ClientAucLot lot) {
        eventLoop.assertThread();
        int id = lot.shortId;
        for (var map : sorted) {
            map.remove(lot, Boolean.TRUE);
        }
        removeIndex(lot, id);
        releaseShortId(id);
    }

    public void insertOrUpdateLot(ClientAucLot lot) {
        eventLoop.assertThread();
        int localId = lot.shortId;
        boolean reindex = false;
        for (var map : sorted) {
            var old = map.put(lot, Boolean.TRUE);
            if (!reindex) reindex = old == null;
        }
        if (reindex)
            reindex(lot, localId);
    }

    public void addVaultLot(ClientVaultLot lot) {
        eventLoop.assertThread();
        var map = owner2vaultLots.computeIfAbsent(lot.owner(), k -> new ConcurrentSkipListMap<>((o1, o2) -> {
            if (o1.uid() == o2.uid()) return 0;
            int x = Long.compare(o1.lot.removalDate(), o2.lot.removalDate());
            if (x != 0) return x;
            return Integer.compare(o1.uid(), o2.uid());
        }));
        map.put(lot, Boolean.TRUE);
    }

    public PlayerVaultResult playerVaultLots(UUID player) {
        var map = owner2vaultLots.get(player);
        if (map == null) return new PlayerVaultResult(Collections.emptyIterator(), 0);
        return new PlayerVaultResult(map.navigableKeySet().iterator(), map.size());
    }

    public void removeVaultLot(ClientVaultLot lot) {
        eventLoop.assertThread();
        var set = owner2vaultLots.get(lot.owner());
        if (set == null) return;
        set.remove(lot, Boolean.TRUE);
        if (set.isEmpty()) owner2vaultLots.remove(lot.owner());
    }

    private void removeIndex(ClientAucLot lot, int localId) {
        used.clear(localId);
        for (int tag : lot.itemStack.tags()) {
            index(tag, localId, false);
        }
        var set = owner2ownedLots.get(lot.owner());
        if (set != null) set.clear(localId);
    }

    private void reindex(ClientAucLot lot, int localId) {
        used.set(localId);
        for (int tag : lot.itemStack.tags()) {
            index(tag, localId, true);
        }
        var set = owner2ownedLots.computeIfAbsent(lot.owner(), k -> new BitSet());
        set.set(localId, true);
    }

    private void releaseShortId(int id) {
        long now = System.nanoTime() / REUSE_GRANULARITY_NS;
        freeIds.enqueue((now << 32) | (id & 0xffffffffL));
    }

    public int nextShortId() {
        if (!freeIds.isEmpty()) {
            long now = System.nanoTime() / REUSE_GRANULARITY_NS;

            long v = freeIds.firstLong();

            long released = v >>> 32;
            if (now - released >= REUSE_DELAY_QUANTA) {
                freeIds.dequeueLong();
                var id = (int) (v & 0xffffffffL);
                if (id <= 0 || id > shortIdCounter) {
                    log.error("freeIds дал неправильный id {}", id);
                    return shortIdCounter++;
                }
                return id;
            } else {
                return shortIdCounter++;
            }
        } else {
            return shortIdCounter++;
        }
    }

    private void index(int tag, int lot, boolean state) {
        if (tag < 0) throw new IllegalArgumentException("pos < 0");
        if (tag >= index.length) index = ensureCapacity(index, tag);
        var bitset = index[tag];
        if (bitset == null) {
            bitset = index[tag] = new BitSet();
        }
        bitset.set(lot, state);
    }

    private <T> T[] ensureCapacity(T[] arr, int minCapacity) {
        if (minCapacity <= arr.length) return arr;
        int newCap = Math.max(Math.max(arr.length * 2, 1024), minCapacity);
        return Arrays.copyOf(arr, newCap);
    }


    @Override
    public void close() {

    }
}
