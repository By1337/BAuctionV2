package com.by1337.auc.common.backend.item;

import com.by1337.auc.common.db.BatchedK2VCache;
import com.by1337.auc.common.handler.BAucRuntime;
import com.by1337.auc.common.handler.GetPostChannelHandler;
import com.by1337.auc.common.network.c2s.C2SLoadItemRequest;
import com.by1337.auc.common.network.c2s.C2SPushItemRequest;
import com.by1337.auc.common.network.s2c.S2CItemIdResponsePacket;
import com.by1337.auc.common.network.s2c.S2CItemResponsePacket;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import dev.by1337.sync.bd.repo.Int2MediumBLOBRepository;
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
    //private ItemRepository repository;
    private final Object2IntOpenHashMap<HashCode> sha2id = new Object2IntOpenHashMap<>();
    private BatchedK2VCache<Integer, byte[]> repo;
    private int lastId;

    public ItemServiceBackend() {
        sha2id.defaultReturnValue(-1);
        registerGet(C2SPushItemRequest.class, this::pushItem);
        registerGet(C2SLoadItemRequest.class, this::loadItem);

    }

    @Override
    public void init(ChannelRuntime runtime) {
        if (!(runtime instanceof BAucRuntime server)) throw new IllegalArgumentException("Invalid runtime type");
        channel = server;
        var int2blob = new Int2MediumBLOBRepository(server.database().dataSource(), server.name() + "_item_repository");
        try {
            lastId = int2blob.getMaxId();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        repo = new BatchedK2VCache<>(
                int2blob,
                server.ioWorker(),
                b -> b
                        .maximumWeight(1024 * 1024 * 1024)
                        .weigher((Integer id, byte[] item) -> item.length + 32 + 4)
                        .expireAfterAccess(Duration.ofHours(2))
                        .removalListener((key, value, cause) -> {
                            if (key == null || value == null) return;
                            sha2id.remove(Hashing.sha256().hashBytes(value), key.intValue());
                        })
        );
    }

    private ResponseFuture<S2CItemResponsePacket> loadItem(C2SLoadItemRequest load) {
        var res = repo.get(load.id()).orElse(null);
        if (res == null) return new ResponseFuture<>(new S2CItemResponsePacket(null));
        sha2id.put(Hashing.sha256().hashBytes(res), load.id());
        return new ResponseFuture<>(new S2CItemResponsePacket(res));
    }

    private ResponseFuture<S2CItemIdResponsePacket> pushItem(C2SPushItemRequest push) {
        HashCode hashCode = HashCode.fromBytes(push.sha256());
        int id = sha2id.getInt(hashCode);
        if (id != -1) return new ResponseFuture<>(new S2CItemIdResponsePacket(id));
        id = lastId++;
        repo.put(id, push.itemStack());
        sha2id.put(hashCode, id);
        return new ResponseFuture<>(new S2CItemIdResponsePacket(id));
    }

    @Override
    public void close() {
    }
}