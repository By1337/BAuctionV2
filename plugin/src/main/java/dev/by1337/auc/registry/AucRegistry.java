package dev.by1337.auc.registry;

import com.google.common.collect.Iterators;
import dev.by1337.auc.util.CyclicListIterator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class AucRegistry<T> implements Iterable<T> {
    private final Map<String, T> id2value = new LinkedHashMap<>();
    private final Map<T, String> value2id = new IdentityHashMap<>();
    private boolean writeLock;
    private T first;

    public void register(String key, T value) {
        if (writeLock) throw new IllegalStateException("write locked!");
        id2value.put(key, value);
        value2id.put(value, key);
    }

    public @Nullable String getId(T value) {
        return value2id.get(value);
    }

    public @Nullable T getByName(String name) {
        return id2value.get(name);
    }

    public void writeLock() {
        writeLock = true;
    }

    public T first() {
        if (first == null) return first = iterator().next();
        return first;
    }

    private List<T> list;

    public CyclicListIterator<T> cycle() {
        if (!writeLock) return new CyclicListIterator<>(List.copyOf(id2value.values()));
        if (list == null) list = List.copyOf(id2value.values());
        return new CyclicListIterator<>(list);
    }

    public Collection<T> values() {
        return Collections.unmodifiableCollection(id2value.values());
    }

    @Override
    public @NonNull Iterator<T> iterator() {
        return Iterators.unmodifiableIterator(id2value.values().iterator());
    }

    public int size() {
        return id2value.size();
    }
}
