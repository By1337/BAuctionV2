package com.by1337.auc.handler.index;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Tag2IdService {
    public static final Tag2IdService INSTANCE = new Tag2IdService();

    private final AtomicInteger counter = new AtomicInteger();
    private final Map<String, Integer> tag2id = new ConcurrentHashMap<>();


    public int getId(String tag) {
        return tag2id.computeIfAbsent(tag.toLowerCase(Locale.ROOT), ignored -> counter.getAndIncrement());
    }
}
