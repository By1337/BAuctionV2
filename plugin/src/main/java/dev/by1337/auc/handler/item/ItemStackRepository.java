package dev.by1337.auc.handler.item;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.common.network.c2s.C2SLoadItemRequest;
import dev.by1337.auc.common.network.c2s.C2SPushItemRequest;
import dev.by1337.auc.config.Config;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.handler.index.Tag2IdService;
import dev.by1337.auc.pipeline.LocalChannelContext;
import dev.by1337.auc.pipeline.LocalChannelHandler;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.pipeline.Remote;
import dev.by1337.core.BCore;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Base64;

public class ItemStackRepository implements LocalChannelHandler {
    private static final HashFunction SHA_256 = Hashing.sha256();
    private static final Logger log = LoggerFactory.getLogger(ItemStackRepository.class);
    private LocalPipeline pipeline;
    private Remote remote;

    private Tag2IdService tag2id;

    private final Int2ObjectOpenHashMap<ClientItemStack> items = new Int2ObjectOpenHashMap<>();
    private final Object2IntOpenHashMap<HashCode> sha2item = new Object2IntOpenHashMap<>();

    private final Config config;

    public ItemStackRepository(Config config) {
        this.config = config;
        sha2item.defaultReturnValue(-1);
    }

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.pipeline = pipeline;
        this.remote = remote;
        tag2id = Tag2IdService.INSTANCE;
    }

    public ResponseFuture<@Nullable ClientItemStack> loadItem(int id) {
        return pipeline.submit(() -> loadItem0(id));
    }

    private final Int2ObjectOpenHashMap<ResponseFuture<@Nullable ClientItemStack>> inflightLoads = new Int2ObjectOpenHashMap<>();

    private ResponseFuture<@Nullable ClientItemStack> loadItem0(int id) {
        pipeline.eventLoop().assertThread();

        var v = items.get(id);
        if (v != null) {
            return new ResponseFuture<>(v);
        } else {
            var inWork = inflightLoads.get(id);
            if (inWork != null) return inWork;

            ResponseFuture<@Nullable ClientItemStack> future = new ResponseFuture<>();
            inflightLoads.put(id, future);

            remote.request(new C2SLoadItemRequest(id))
                    .map(result -> {
                        pipeline.eventLoop().assertThread();
                        byte[] itemStack;
                        if ((itemStack = result.itemStack()) != null) {
                            ClientItemStack item;
                            var old = items.put(id, item = ClientItemStack.make(
                                    id,
                                    itemStack,
                                    config.tagsExtractor,
                                    tag2id
                            ));
                            sha2item.put(SHA_256.hashBytes(itemStack), id);
                            if (old != null && !Arrays.equals(old.bytes(), item.bytes())) {
                                log.error("Rewrite ItemStack old={} new={}", Base64.getEncoder().encodeToString(old.bytes()), Base64.getEncoder().encodeToString(item.bytes()));
                            }
                            return item;
                        }
                        return null;
                    }).then(res -> {
                        inflightLoads.remove(id);
                        future.complete(res);
                    });
            return future;
        }
    }

    public ResponseFuture<@Nullable Integer> pushItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            log.error("Попытка AIR запушить! {}", itemStack, new Throwable());
            return new ResponseFuture<>(null);
        }
        if (itemStack.getAmount() != 1) itemStack = itemStack.asOne();
        return pushItem(BCore.getItemStackSerializer().serialize(itemStack, null));
    }

    public ResponseFuture<@Nullable Integer> pushItem(byte[] itemStack) {
        return pipeline.submit(() -> pushItem0(itemStack));
    }

    private ResponseFuture<@Nullable Integer> pushItem0(byte[] itemStack) {
        pipeline.eventLoop().assertThread();
        var sha256 = SHA_256.hashBytes(itemStack);
        int bySha = sha2item.getInt(sha256);
        if (bySha != -1) {
            var v = items.get(bySha);
            if (v != null && Arrays.equals(itemStack, v.bytes())) {
                return new ResponseFuture<>(bySha);
            } else {
                log.error("Duplicate SHA! old={}, new={}", v == null ? "null" : Base64.getEncoder().encodeToString(v.bytes()), Base64.getEncoder().encodeToString(itemStack));
            }
        }
        return remote.request(new C2SPushItemRequest(itemStack))
                .map(result -> {
                    pipeline.eventLoop().assertThread();
                    int id = result.id();
                    items.put(id, ClientItemStack.make(
                            id,
                            itemStack,
                            config.tagsExtractor,
                            tag2id
                    ));
                    sha2item.put(sha256, id);
                    return id;
                });
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        ctx.fire(msg);
    }


    @Override
    public void close() {
    }
}
