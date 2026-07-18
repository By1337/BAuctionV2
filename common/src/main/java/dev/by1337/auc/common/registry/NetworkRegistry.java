package dev.by1337.auc.common.registry;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NetworkRegistry<T> {
    private final Map<String, Function<ByteBuf, T>> map = new HashMap<>();

    public void register(String id, Function<ByteBuf, T> maker) {
        if (id.length() > 64) throw new IllegalArgumentException("name is too long! max 64 " + id);
        map.put(id, maker);
    }

    public @Nullable Function<ByteBuf, T> creator(String id) {
        return map.get(id);
    }
}
