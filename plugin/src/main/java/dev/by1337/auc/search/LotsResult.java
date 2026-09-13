package dev.by1337.auc.search;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.LotData;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public interface LotsResult {
    static LotsResult EMPTY = new LotsResult() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public @Nullable LotData next() {
            return null;
        }

        @Override
        public void release() {

        }
    };

    int size();

    @Nullable LotData next();

    void release();

    static MemoizedLotsResult memoized(List<LotData> list) {
        return new MemoizedLotsResult() {
            private int ptr = 0;

            @Override
            public void reset() {
                ptr = 0;
            }

            @Override
            public int size() {
                return list.size();
            }

            @Override
            public @Nullable LotData next() {
                if (ptr < list.size()) {
                    return list.get(ptr++);
                }
                return null;
            }

            @Override
            public void release() {

            }
        };
    }

    default MemoizedLotsResult memoized() {
        var self = this;
        return new MemoizedLotsResult() {
            private final List<LotData> list = new ArrayList<>();
            private int ptr = 0;
            private boolean finished = false;

            @Override
            public int size() {
                return self.size();
            }

            @Override
            public @Nullable LotData next() {
                if (ptr >= list.size()) {
                    if (finished) {
                        return null;
                    }
                    LotData next = self.next();
                    if (next == null) {
                        finished = true;
                        return null;
                    }
                    list.add(next);
                    ptr++;
                    return next;
                }
                return list.get(ptr++);
            }

            @Override
            public void release() {
                self.release();
            }

            @Override
            public void reset() {
                ptr = 0;
            }

            @Override
            public MemoizedLotsResult memoized() {
                return this;
            }
        };
    }

    default LotsResult and(LotsResult o) {
        return and(this, o);
    }

    default LotsResult whereOwner(LotsIndexer indexer, UUID owner) {
        var mask = indexer.ownerMask(owner);
        if (mask == null) {
            release();
            return EMPTY;
        }
        return LotsResult.of(mask.cardinality(), mask, this);
    }

    // в LotsResult будут только те что filter.test(N) == true
    default LotsResult filter(Predicate<LotData> filter) {
        var self = this;
        return new LotsResult() {
            @Override
            public int size() {
                return self.size();
            }

            @Override
            public @Nullable LotData next() {
                var v = self.next();
                if (v == null) return null;
                if (!filter.test(v)) return null;
                return v;
            }

            @Override
            public void release() {
                self.release();
            }
        };
    }

    static LotsResult of(NavigableSet<ClientAucLot> set) {
        return of(set.size(), set.iterator());
    }

    static LotsResult of(int size, Iterator<? extends LotData> it) {
        return new LotsResult() {
            @Override
            public int size() {
                return size;
            }

            @Override
            public @Nullable LotData next() {
                if (it.hasNext())
                    return it.next();
                return null;
            }

            @Override
            public void release() {
            }
        };
    }

    static LotsResult of(int size, @Nullable BitSetPool.PooledBitSet filter0, Iterator<? extends LotData> iterator) {
        return of(size, filter0, new LotsResult() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public @Nullable LotData next() {
                if (iterator.hasNext())
                    return iterator.next();
                return null;
            }

            @Override
            public void release() {
            }
        });
    }

    static LotsResult of(int size, @Nullable BitSetPool.PooledBitSet filter0, LotsResult res) {
        return new LotsResult() {
            private @Nullable BitSetPool.PooledBitSet filter = filter0;

            @Override
            public int size() {
                return size;
            }

            @Override
            public @Nullable LotData next() {
                LotData lot;
                while ((lot = res.next()) != null) {
                    var v = filter;
                    if (v != null) {
                        int shortId = lot.shortIdOr(-1);
                        if (shortId != -1 && !v.get(shortId)) continue;
                    }
                    return lot;
                }
                return null;
            }

            @Override
            public void release() {
                var v = filter;
                filter = null;
                if (v != null) v.release();
            }
        };
    }

    static LotsResult one(LotData result0) {
        return new LotsResult() {
            LotData result = result0;

            @Override
            public int size() {
                return 1;
            }

            @Override
            public @Nullable LotData next() {
                var v = result;
                result = null;
                return v;
            }

            @Override
            public void release() {
            }
        };
    }

    static LotsResult and(LotsResult o1, LotsResult o2) {
        int size = o1.size() + o2.size();
        return new LotsResult() {
            @Override
            public int size() {
                return size;
            }

            @Override
            public @Nullable LotData next() {
                var v = o1.next();
                return v != null ? v : o2.next();
            }

            @Override
            public void release() {
                o1.release();
                o2.release();
            }
        };
    }

    static LotsResult and(
            LotsResult o1,
            LotsResult o2,
            Comparator<LotData> comparator
    ) {
        return new LotsResult() {

            private LotData v1;
            private LotData v2;
            private boolean initialized;

            @Override
            public int size() {
                return o1.size() + o2.size();
            }

            @Override
            public @Nullable LotData next() {
                if (!initialized) {
                    initialized = true;
                    v1 = o1.next();
                    v2 = o2.next();
                }

                if (v1 == null) {
                    var result = v2;
                    v2 = o2.next();
                    return result;
                }

                if (v2 == null) {
                    var result = v1;
                    v1 = o1.next();
                    return result;
                }

                if (comparator.compare(v1, v2) <= 0) {
                    var result = v1;
                    v1 = o1.next();
                    return result;
                }

                var result = v2;
                v2 = o2.next();
                return result;
            }

            @Override
            public void release() {
                o1.release();
                o2.release();
            }
        };
    }

    interface MemoizedLotsResult extends LotsResult {
        void reset();
    }
}
