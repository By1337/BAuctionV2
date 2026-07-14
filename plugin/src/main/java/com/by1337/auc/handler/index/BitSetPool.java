package com.by1337.auc.handler.index;

import io.netty.util.internal.shaded.org.jctools.queues.MpmcArrayQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;

public class BitSetPool {
    private static final MpmcArrayQueue<BitSet> sets = new MpmcArrayQueue<>(4096);

    public static PooledBitSet get(@Nullable BitSet base) {
        BitSet set = sets.poll();
        var v = new PooledBitSet(set == null ? new BitSet() : set);
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
        private BitSet src;

        public PooledBitSet(BitSet src) {
            this.src = src;
        }

        public void release() {
            var v = src;
            src = null;
            if (v == null) return;
            sets.offer(v);
        }
        public BitSet lotMask(){
            //LotMask set;
            return src;
        }

        public int cardinality() {
            if (src == null) return 0;
            return src.cardinality();
        }

        public void andNot(@NotNull BitSet other) {
            if (src == null) return;
            src.andNot(other);
        }

        public void and(@NotNull BitSet other) {
            if (src == null) return;
            src.and(other);
        }

        public void or(@NotNull BitSet other) {
            if (src == null) return;
            src.or(other);
        }

        public void copy(@NotNull BitSet src) {
            if (this.src == null) throw new IllegalStateException("Empty");
            this.src.clear();
            this.src.or(src);
           // this.src.copy(src);
        }

        public void set(int bit, boolean f) {
            if (src == null) throw new IllegalStateException("Empty");
            src.set(bit, f);
        }

        public void set(int bit) {
            if (src == null) throw new IllegalStateException("Empty");
            src.set(bit);
        }

        public void clear(int bit) {
            if (src == null) throw new IllegalStateException("Empty");
            src.clear(bit);
        }

        public void clear() {
            if (src == null) throw new IllegalStateException("Empty");
            src.clear();
        }

        public boolean get(int bit) {
            if (src == null) return false;
            return src.get(bit);
        }

        public boolean isEmpty() {
            return src == null;
        }
    }
}
