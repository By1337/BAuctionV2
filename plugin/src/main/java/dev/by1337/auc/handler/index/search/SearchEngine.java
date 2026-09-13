package dev.by1337.auc.handler.index.search;

import dev.by1337.auc.handler.index.BitSetPool;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

public interface SearchEngine {
    RoaringBitmap[] index();

    RoaringBitmap used();

    default @Nullable BitSetPool.PooledBitSet findWithTags(
            int @Nullable [] @Nullable [] ands,
            int @Nullable [] @Nullable [] nots
    ) {
        int filtersCount = Math.max(ands != null ? ands.length : 0, nots != null ? nots.length : 0);
        if (filtersCount == 0) return null;
        BitSetPool.PooledBitSet base = null;
        var index = this.index();
        var buffer = BitSetPool.get(null);
        try {
            for (int i = 0; i < filtersCount; i++) {
                buffer.clear();
                int[] and = safeGet(i, ands);
                int[] not = safeGet(i, nots);
                if (and == null) {
                    buffer.copy(used());
                } else {
                    boolean init = false;
                    for (int idx : and) {
                        var set = safeGet(idx, index);
                        if (set == null) {
                            buffer.clear();
                            continue;
                        }
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
        if (base == null) return BitSetPool.empty();
        return base;
    }

    default @Nullable BitSetPool.PooledBitSet findWithTags(int @Nullable [] and, int @Nullable [] not) {
        if (and == null && not == null) return null;
        BitSetPool.PooledBitSet base = null;
        var index = this.index();
        int maxIndex = index.length - 1;
        if (and == null) {
            base = BitSetPool.get(used());
        } else {
            for (int i : and) {
                if (maxIndex < i) continue;
                var set = index[i];
                if (set == null) {
                    if (base != null) base.clear();
                    continue;
                }
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
}
