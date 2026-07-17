package com.by1337.auc.handler.index;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.BitSet;

public class BitSetPool {
    private static final MpmcArrayQueue<RoaringBitmap> sets = new MpmcArrayQueue<>(256);

    public static PooledBitSet get(@Nullable RoaringBitmap base) {
        RoaringBitmap set = sets.poll();
        var v = new PooledBitSet(set == null ? new RoaringBitmap() : set);
        if (base == null) {
            v.clear();
        } else {
            v.copy(base);
        }
        return v;
    }

    public static PooledBitSet empty() {
        return new PooledBitSet(null);
    }

    public static final class PooledBitSet {
        private RoaringBitmap src;

        public PooledBitSet(RoaringBitmap src) {
            this.src = src;
        }

        public void release() {
            var v = src;
            src = null;
            if (v == null) return;
            sets.offer(v);
        }
        public RoaringBitmap lotMask(){
            //LotMask set;
            return src;
        }

        public int cardinality() {
            if (src == null) return 0;
            return src.getCardinality();
          //  return src.cardinality();
        }

        public void andNot(@NotNull RoaringBitmap other) {
            if (src == null) return;
            src.andNot(other);
        }

        public void and(@NotNull RoaringBitmap other) {
            if (src == null) return;
            src.and(other);
        }

        public void or(@NotNull RoaringBitmap other) {
            if (src == null) return;
            src.or(other);
        }

        public void copy(@NotNull RoaringBitmap src) {
            if (this.src == null) throw new IllegalStateException("Empty");
            this.src.clear();
            this.src.or(src);
           // this.src.copy(src);
        }

        public void set(int bit, boolean f) {
            if (src == null) throw new IllegalStateException("Empty");
            if (f) src.add(bit);
            else src.remove(bit);
            //src.set(bit, f);
        }

        public void set(int bit) {
            if (src == null) throw new IllegalStateException("Empty");
            src.add(bit);
            //src.set(bit);
        }

        public void clear(int bit) {
            if (src == null) throw new IllegalStateException("Empty");
            src.remove(bit);
            //src.clear(bit);
        }

        public void clear() {
            if (src == null) throw new IllegalStateException("Empty");
            src.clear();
        }

        public boolean get(int bit) {
            if (src == null) return false;
           return src.contains(bit);
            //return src.get(bit);
        }

        public boolean isEmpty() {
            return src == null;
        }
    }
}
