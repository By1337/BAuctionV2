package com.by1337.auc.util;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CyclicListIterator<E> {
    private final List<E> list;
    private int current = 0;

    public CyclicListIterator(List<E> list) {
        this.list = Objects.requireNonNull(list);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be empty");
        }
    }

    public E next() {
        current = (current + 1) % list.size();
        return list.get(current);
    }

    public E previous() {
        current = (current - 1 + list.size()) % list.size();
        return list.get(current);
    }

    public E current() {
        return list.get(current);
    }

    public int pos() {
        return current;
    }

    public List<E> list() {
        return Collections.unmodifiableList(list);
    }
}
