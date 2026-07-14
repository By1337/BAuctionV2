package com.by1337.auc.util;

import java.util.Arrays;
import java.util.BitSet;

public final class LotMask {
    private static final long WORD_MASK = 0xffffffffffffffffL;
    private static final int ADDRESS_BITS_PER_WORD = 6;
    private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

    private long[] words;
    private int wordsInUse = 0;

    public LotMask(int words) {
        this.words = new long[words];
    }


    public LotMask() {
        words = new long[16];
    }

    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);


        int u = fromIndex >> 6;
        if (u >= wordsInUse)
            return -1;

        long word = words[u] & (WORD_MASK << (fromIndex & 63));

        while (true) {
            if (word != 0)
                return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            if (++u == wordsInUse)
                return -1;
            word = words[u];
        }
    }

    public int cardinality() {
        int x = 0;
        for (int i = 0; i < wordsInUse; i++) {
            x += Long.bitCount(words[i]);
        }
        return x;
    }

    public void andNot(LotMask other) {
        int min = Math.min(wordsInUse, other.wordsInUse);
        for (int i = 0; i < min; i++) {
            words[i] &= ~other.words[i];
        }
        for (int i = min; i < wordsInUse; i++) {
            words[i] = 0;
        }
        recalculateWordsInUse();
    }

    public void and(LotMask other) {
        int min = Math.min(wordsInUse, other.wordsInUse);
        for (int i = 0; i < min; i++) {
            words[i] &= other.words[i];
        }
        for (int i = min; i < wordsInUse; i++) {
            words[i] = 0;
        }
        recalculateWordsInUse();
    }

    public void orFiltered(LotMask include, LotMask exclude){
        ensureCapacity(include.wordsInUse - 1);
        int max = Math.min(include.wordsInUse, words.length);

        for (int i = 0; i < max; i++) {
            long inc = include.words[i];
            long exc = i < exclude.wordsInUse ? exclude.words[i] : 0L;

            words[i] |= inc & ~exc;
        }
        wordsInUse = Math.max(wordsInUse, include.wordsInUse);
    }

    public void or(LotMask set) {
        if (this == set)
            return;

        int wordsInCommon = Math.min(wordsInUse, set.wordsInUse);

        if (wordsInUse < set.wordsInUse) {
            ensureCapacity(set.wordsInUse);
            wordsInUse = set.wordsInUse;
        }

        for (int i = 0; i < wordsInCommon; i++)
            words[i] |= set.words[i];

        if (wordsInCommon < set.wordsInUse)
            System.arraycopy(set.words, wordsInCommon, words, wordsInCommon, wordsInUse - wordsInCommon);
    }

    public void copy(LotMask src) {
        ensureCapacity(src.wordsInUse - 1);

        System.arraycopy(src.words, 0, words, 0, src.wordsInUse);
        Arrays.fill(words, src.wordsInUse, wordsInUse, 0);

        wordsInUse = src.wordsInUse;
    }

    public void set(int bit, boolean f) {
        if (f) set(bit);
        else clear(bit);
    }
    public void set(int bit) {
        int wordIndex = wordIndex(bit);
        ensureCapacity(wordIndex);

        words[wordIndex] |= 1L << (bit & 63);

        wordsInUse = Math.max(wordsInUse, wordIndex + 1);
    }

    private static int wordIndex(int bitIndex) {
        return bitIndex >> ADDRESS_BITS_PER_WORD;
    }

    public void clear(int bit) {
        int wordIndex = wordIndex(bit);
        if (wordIndex >= wordsInUse)
            return;
        words[wordIndex] &= ~(1L << (bit & 63));
        recalculateWordsInUse();
    }

    public boolean get(int bit) {
        int word = wordIndex(bit);

        return word < wordsInUse &&
                (words[word] & (1L << (bit & 63))) != 0;
    }

    public void clear() {
        if (wordsInUse == 0) return;
        Arrays.fill(words, 0, wordsInUse, 0L);
        wordsInUse = 0;
    }

    private void recalculateWordsInUse() {
        int i;
        for (i = wordsInUse - 1; i >= 0; i--)
            if (words[i] != 0)
                break;

        wordsInUse = i + 1;
    }

    private void ensureCapacity(int wordsRequired) {
        if (words.length <= wordsRequired) {
            int request = Math.max(2 * words.length, wordsRequired + 1);
            words = Arrays.copyOf(words, request);
        }
    }
}
