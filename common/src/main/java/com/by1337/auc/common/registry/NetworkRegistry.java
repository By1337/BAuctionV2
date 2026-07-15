package com.by1337.auc.common.registry;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class NetworkRegistry<T> {
    private final Map<String, Function<ByteBuf, T>> map = new HashMap<>();

    public void register(String id, Function<ByteBuf, T> maker){
        map.put(id, maker);
    }

    public @Nullable Function<ByteBuf, T> creator(String id){
        return map.get(id);
    }
}
