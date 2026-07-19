package dev.by1337.auc.search;

import dev.by1337.auc.auc.LotData;
import org.jetbrains.annotations.Nullable;

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

    default LotsResult and(LotsResult o) {
        return and(this, o);
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
}
