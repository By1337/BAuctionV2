package dev.by1337.auc.handler.index;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.ClientVaultLot;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.handler.event.*;
import dev.by1337.auc.handler.index.search.SearchEngine;
import dev.by1337.auc.handler.name.PlayerNameService;
import dev.by1337.auc.pipeline.LocalChannelContext;
import dev.by1337.auc.pipeline.LocalChannelHandler;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.pipeline.Remote;
import dev.by1337.auc.registry.AucRegistry;
import dev.by1337.auc.search.LotsResult;
import dev.by1337.auc.search.PlayerVaultResult;
import dev.by1337.auc.search.SearchResult;
import dev.by1337.auc.search.filter.SearchFilter;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.work.EventLoopWorker;
import it.unimi.dsi.fastutil.longs.LongArrayPriorityQueue;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

public class LotsIndexer implements LocalChannelHandler, SearchEngine {
    private static final Logger log = LoggerFactory.getLogger(LotsIndexer.class);
    private static final long REUSE_GRANULARITY_NS = TimeUnit.SECONDS.toNanos(5);
    private static final int REUSE_DELAY_QUANTA = (int) (TimeUnit.SECONDS.toNanos(30) / REUSE_GRANULARITY_NS);

    private final LongArrayPriorityQueue freeIds = new LongArrayPriorityQueue();
    private int shortIdCounter;
    private RoaringBitmap[] index = new RoaringBitmap[2048];
    private final RoaringBitmap used = new RoaringBitmap();
    private EventLoopWorker eventLoop;
    private final Object2IntOpenHashMap<String> sorting2id = new Object2IntOpenHashMap<>();
    private ConcurrentSkipListMap<ClientAucLot, Boolean>[] sorted;
    private final ConcurrentSkipListMap<String, UUID> normalName2UUID = new ConcurrentSkipListMap<>();
    private final Object2ObjectOpenHashMap<UUID, ConcurrentSkipListMap<ClientVaultLot, Boolean>> owner2vaultLots = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<UUID, RoaringBitmap> owner2ownedLots = new Object2ObjectOpenHashMap<>();
    private PlayerNameService playerNames;
    private Auction auction;
    private final ConcurrentSkipListMap<ClientAucLot, Boolean>[][] material2lots;
    private AucRegistry<Sorting> sortingRegistry;

    public LotsIndexer() {
        sorting2id.defaultReturnValue(0);
        material2lots = new ConcurrentSkipListMap[Material.values().length][];
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof PlayerChangeNameEvent(UUID uuid, String newName, String oldName)) {
            if (normalName2UUID.remove(oldName.toLowerCase(), uuid)) {
                normalName2UUID.put(newName.toLowerCase(Locale.ROOT), uuid);
            }
        } else if (msg instanceof LotUpdateEvent(ClientAucLot lot)) {
            insertOrUpdateLot(lot);
        } else if (msg instanceof RemoveLotEvent(ClientAucLot lot)) {
            removeLot(lot);
        } else if (msg instanceof VaultLotUpdateEvent(ClientVaultLot lot)) {
            addVaultLot(lot);
        } else if (msg instanceof RemoveVaultLotEvent(ClientVaultLot lot)) {
            removeVaultLot(lot);
        }

        ctx.fire(msg);
    }

    @Override
    public RoaringBitmap[] index() {
        return index;
    }

    @Override
    public RoaringBitmap used() {
        return used;
    }

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.auction = auction.auction();
        playerNames = pipeline.get(PlayerNameService.class);
        eventLoop = pipeline.eventLoop();
        int x = 0;
        sortingRegistry = auction.registries().sorting;
        sorted = new ConcurrentSkipListMap[sortingRegistry.size()];
        for (Sorting sorting : auction.registries().sorting) {
            sorting2id.put(sorting.id(), x);
            sorted[x++] = new ConcurrentSkipListMap<>(sorting.comparator());
        }
    }

    public NavigableSet<ClientAucLot> lotsSet() {
        return lotsSet(null);
    }

    public NavigableSet<ClientAucLot> lotsSet(@Nullable Sorting sorting) {
        var set = sorted[sorting == null ? 0 : sorting2id.getInt(sorting.id())];
        return set.navigableKeySet();
    }

    public NavigableSet<ClientAucLot> lotsSetByMaterial(int ordinal, @Nullable Sorting sorting) {
        var sorted = material2lots[ordinal];
        if (sorted == null) return Collections.emptyNavigableSet();
        var set = sorted[sorting == null ? 0 : sorting2id.getInt(sorting.id())];
        if (set == null) return Collections.emptyNavigableSet();
        return set.navigableKeySet();
    }

    public @Nullable BitSetPool.PooledBitSet ownerMask(UUID owner) {
        return new BitSetPool.PooledBitSet(owner2ownedLots.get(owner), false);
    }


    @Deprecated
    public SearchResult search(@Nullable UUID owner, @Nullable SearchFilter filter, Sorting sorting) {
        if (filter != null) {
            if (owner != null) {
                var mask = ownerMask(owner);
                if (mask == null) return SearchResult.EMPTY;
                LotsResult result = filter.searchLots(this, sorting);
                return new SearchResult(LotsResult.of(mask.lotMask().getCardinality(), mask, result));
            }
            return new SearchResult(filter.searchLots(this, sorting));
        } else if (owner != null) {
            var mask = ownerMask(owner);
            if (mask == null) return SearchResult.EMPTY;
            var v = LotsResult.of(mask.lotMask().getCardinality(), mask, lotsSet(sorting).iterator());
            return new SearchResult(v);
        } else {
            return new SearchResult(LotsResult.of(lotsSet(sorting)));
        }
    }

    @Deprecated
    public SearchResult search(@Nullable SearchFilter filter, Sorting sorting) {
        if (filter == null) return new SearchResult(LotsResult.of(lotsSet(sorting)));
        return new SearchResult(filter.searchLots(this, sorting));
    }

    @Deprecated
    public SearchResult search(@Nullable BitSetPool.PooledBitSet mask, Sorting sorting) {
        if (mask == null) return new SearchResult(LotsResult.of(lotsSet(sorting)));
        var v = LotsResult.of(mask.lotMask().getCardinality(), mask, lotsSet(sorting).iterator());
        return new SearchResult(v);
    }


    private void removeLot(ClientAucLot lot) {
        eventLoop.assertThread();
        int id = lot.shortId;
        for (var map : sorted) {
            map.remove(lot, Boolean.TRUE);
        }
        int material = lot.itemStack.materialOrdinal;
        var v = material2lots[material];
        if (v != null) {
            for (ConcurrentSkipListMap<ClientAucLot, Boolean> map : v) {
                if (map != null) {
                    map.remove(lot, Boolean.TRUE);
                    if (map.isEmpty()) {
                        material2lots[material] = null;
                        break;
                    }
                }
            }
        }
        removeIndex(lot, id);
        releaseShortId(id);
    }

    private void insertOrUpdateLot(ClientAucLot lot) {
        eventLoop.assertThread();
        int localId = lot.shortId;
        boolean reindex = false;
        for (var map : sorted) {
            //нужен map#remove так как map#put не заменит ключ, а нам надо
            reindex |= !map.remove(lot, Boolean.TRUE);
            map.put(lot, Boolean.TRUE);
        }
        int material = lot.itemStack.materialOrdinal;
        var v = material2lots[material];
        if (v == null) {
            v = material2lots[material] = new ConcurrentSkipListMap[sortingRegistry.size()];
        }
        for (Sorting sorting : sortingRegistry) {
            int sortId = sorting2id.getInt(sorting.id());
            var map = v[sortId];
            if (map == null) {
                map = v[sortId] = new ConcurrentSkipListMap<>(sorting.comparator());
            } else {
                map.remove(lot, Boolean.TRUE);
            }
            map.put(lot, Boolean.TRUE);
        }
        if (reindex)
            reindex(lot, localId);
    }

    private void addVaultLot(ClientVaultLot lot) {
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

    public int getPlayerOwnedLotsCount(UUID key) {
        var v = owner2ownedLots.get(key);
        if (v == null) return 0;
        return v.getCardinality();
    }

    public @Nullable UUID name2uuid(String normalName) {
        return normalName2UUID.get(normalName.toLowerCase(Locale.ROOT));
    }

    public Iterator<String> normalPlayerNamesIterator() {
        return normalName2UUID.navigableKeySet().iterator();
    }

    private void removeVaultLot(ClientVaultLot lot) {
        eventLoop.assertThread();
        var set = owner2vaultLots.get(lot.owner());
        if (set == null) return;
        set.remove(lot, Boolean.TRUE);
        if (set.isEmpty()) owner2vaultLots.remove(lot.owner());
    }

    private void removeIndex(ClientAucLot lot, int localId) {
        used.remove(localId);
        for (int tag : lot.itemStack.tags()) {
            index(tag, localId, false);
        }
        var set = owner2ownedLots.get(lot.owner());
        if (set != null) {
            set.remove(localId);
            if (set.isEmpty()) owner2ownedLots.remove(lot.owner());
            playerNames.loadName(lot.owner()).ifPresent(name -> {
                if (!owner2ownedLots.containsKey(lot.owner()))
                    normalName2UUID.remove(name.name().toLowerCase(Locale.ROOT), lot.owner());
            });
        }
    }

    private void reindex(ClientAucLot lot, int localId) {
        used.add(localId);
        for (int tag : lot.itemStack.tags()) {
            index(tag, localId, true);
        }
        var set = owner2ownedLots.computeIfAbsent(lot.owner(), k -> new RoaringBitmap());
        playerNames.loadName(lot.owner()).ifPresent(name -> {
            if (owner2ownedLots.containsKey(lot.owner()))
                normalName2UUID.put(name.name().toLowerCase(Locale.ROOT), lot.owner());
        });
        set.add(localId);
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
                if (id < 0 || id > shortIdCounter) {
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
        if (tag >= index.length) index = ensureCapacity(index, tag + 1);
        var bitset = index[tag];
        if (bitset == null) {
            bitset = index[tag] = new RoaringBitmap();
        }
        if (state) bitset.add(lot);
        else bitset.remove(lot);
        //bitset.set(lot, state);
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
