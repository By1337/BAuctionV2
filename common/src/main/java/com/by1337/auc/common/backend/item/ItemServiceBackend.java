package com.by1337.auc.common.backend.item;

import com.by1337.auc.common.handler.BAucRuntime;
import com.by1337.auc.common.handler.GetPostChannelHandler;
import com.by1337.auc.common.network.c2s.C2SLoadItemRequest;
import com.by1337.auc.common.network.c2s.C2SPushItemRequest;
import com.by1337.auc.common.network.s2c.S2CItemIdResponsePacket;
import com.by1337.auc.common.network.s2c.S2CItemResponsePacket;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Duration;

public class ItemServiceBackend extends GetPostChannelHandler {
    private static final Logger log = LoggerFactory.getLogger(ItemServiceBackend.class);
    private BAucRuntime channel;
    private ItemRepository repository;
    private final Object2IntOpenHashMap<HashCode> sha2id = new Object2IntOpenHashMap<>();
    private final Cache<Integer, Item> cache;

    public ItemServiceBackend() {
        sha2id.defaultReturnValue(-1);
        registerGet(C2SPushItemRequest.class, this::pushItem);
        registerGet(C2SLoadItemRequest.class, this::loadItem);
        cache = Caffeine.newBuilder()
                .maximumWeight(1024 * 1024 * 1024)
                .weigher((Integer id, Item item) -> item.item.length + 32 + 4)
                .expireAfterAccess(Duration.ofHours(2))
                .removalListener((key, value, cause) -> {
                    if (key == null || value == null) return;
                    sha2id.remove(value.sha256, key.intValue());
                })
                .build();

    }

    @Override
    public void init(ChannelRuntime runtime) {
        if (!(runtime instanceof BAucRuntime server)) throw new IllegalArgumentException("Invalid runtime type");
        channel = server;
        repository = new ItemRepository(server.database().dataSource(), server.name() + "_item_repository");
    }

    private ResponseFuture<S2CItemResponsePacket> loadItem(C2SLoadItemRequest load) {
        var item = cache.get(load.id(), k -> {
            try {
                var bytes = repository.get(k).orElse(null);
                return bytes == null ? null : new Item(bytes, Hashing.sha256().hashBytes(bytes));
            } catch (SQLException e) {
                log.error("Failed to load item", e);
                return null;
            }
        });
        return new ResponseFuture<>(new S2CItemResponsePacket(item == null ? null : item.item));
    }

    private ResponseFuture<S2CItemIdResponsePacket> pushItem(C2SPushItemRequest push) {
        HashCode hashCode = HashCode.fromBytes(push.sha256());
        int id = sha2id.getInt(hashCode);
        if (id != -1) return new ResponseFuture<>(new S2CItemIdResponsePacket(id));
        try {
            int result = repository.putIfAbsent(push.sha256(), push.itemStack());
            cache.put(result, new Item(push.itemStack(), hashCode));
            sha2id.put(hashCode, result);
            return new ResponseFuture<>(new S2CItemIdResponsePacket(result));
        } catch (SQLException e) {
            log.error("Failed to push item", e);
            return new ResponseFuture<>(null);
        }
    }

    @Override
    public void close() {
    }

    public record Item(byte[] item, HashCode sha256) {
    }
}