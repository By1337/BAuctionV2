package dev.by1337.auc.handler;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.ClientVaultLot;
import dev.by1337.auc.auc.GhostLot;
import dev.by1337.auc.common.auc.AucLot;
import dev.by1337.auc.common.auc.VaultLot;
import dev.by1337.auc.common.network.a2a.A2AFlagResponse;
import dev.by1337.auc.common.network.c2s.*;
import dev.by1337.auc.common.network.s2c.*;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.handler.item.ItemStackRepository;
import dev.by1337.auc.handler.name.PlayerNameService;
import dev.by1337.auc.pipeline.LocalChannelContext;
import dev.by1337.auc.pipeline.LocalChannelHandler;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.pipeline.Remote;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.channel.handler.request.IncomingRequest;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.work.EventLoopWorker;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public class LotsRepository implements LocalChannelHandler {
    private static final long VAULT_STORE_DURATION_MS = TimeUnit.DAYS.toMillis(1);
    private static final Logger log = LoggerFactory.getLogger(LotsRepository.class);
    private LocalPipeline pipeline;
    private Remote remote;
    private ItemStackRepository itemService;
    private LotsIndexer indexer;
    private PlayerNameService players;
    private boolean closing;
    private EventLoopWorker worker;

    private final Int2ObjectOpenHashMap<ClientAucLot> lots = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<ClientVaultLot> vault = new Int2ObjectOpenHashMap<>();

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.pipeline = pipeline;
        this.remote = remote;
        worker = pipeline.eventLoop();
        itemService = pipeline.get(ItemStackRepository.class);
        indexer = pipeline.get(LotsIndexer.class);
        players = pipeline.get(PlayerNameService.class);
        pipeline.eventLoop().schedule(() -> {
            remote.write(new C2SGetAllLotsRequest());
            remote.write(new C2SGetAllVaultLotsRequest());
        });
    }

    public @Nullable ClientAucLot getLot(int uid) {
        return lots.get(uid);
    }

    public @Nullable ClientVaultLot getVaultLot(int uid) {
        return vault.get(uid);
    }


    public ResponseFuture<ActionResult> subtractOrRemoveLot(ClientAucLot lot0, int count) {
        if (count == lot0.count()) return removeLot(lot0);
        return pipeline.submit(() -> remote.request(new C2SSubtractLotRequest(lot0.uid(), count))
                .map(ActionResult::of).orElse(ActionResult::deny));
    }

    public ResponseFuture<ActionResult> removeLot(ClientAucLot lot0) {
        return pipeline.submit(() -> remote.request(new C2SRemoveLotRequest(lot0.uid()))
                .map(ActionResult::of).orElse(ActionResult::deny));
    }

    public ResponseFuture<ActionResult> removeVaultLot(ClientVaultLot lot0) {
        return pipeline.submit(() -> remote.request(new C2SRemoveVaultLotRequest(lot0.uid()))
                .map(ActionResult::of).orElse(ActionResult::deny));
    }

    public ResponseFuture<Boolean> moveToVault(ClientAucLot lot, UUID owner) {
        return pipeline.submit(() -> remote.request(new C2SMove2VaultRequest(lot.uid(), owner, VAULT_STORE_DURATION_MS)).map(A2AFlagResponse::flag).orElse(() -> false));
    }

    public ResponseFuture<ActionResult> readdVaultLot(ClientVaultLot vault) {
        return pipeline.submit(() -> remote.request(new C2SAddNewVaultRequest(
                vault.lot.uid(),
                vault.owner(),
                VAULT_STORE_DURATION_MS,
                vault.count(),
                vault.lot.lprice()
        ))).map(ActionResult::of).orElse(ActionResult::deny);
    }

    public ResponseFuture<ActionResult> addToVault(ItemStack itemStack, int count, UUID owner, long price) {
        return itemService.pushItem(itemStack)
                .flatMap(id -> remote.request(new C2SAddNewVaultRequest(
                        id,
                        owner,
                        VAULT_STORE_DURATION_MS,
                        count,
                        price
                ))).map(ActionResult::of).orElse(ActionResult::deny);
    }

    public ResponseFuture<@Nullable GhostLot> addLot(ItemStack itemStack, UUID owner, long sellingDuration, int count, long price) {
        return pipeline.submit(() -> addLot0(itemStack, owner, sellingDuration, count, price));
    }

    private ResponseFuture<@Nullable GhostLot> addLot0(ItemStack itemStack, UUID owner, long sellingDuration, int count, long price) {
        pipeline.eventLoop().assertThread();
        if (itemStack.getAmount() != 1) {
            log.error("Bad item size {}", itemStack);
            return new ResponseFuture<>(null);
        }
        return itemService.pushItem(itemStack)
                .flatMap(id -> remote.request(new C2SAddNewLotRequest(
                                id,
                                owner,
                                sellingDuration,
                                count,
                                price
                        ))
                        .ifEmpty(() -> log.error("Server ignored C2SAddNewLotRequest {}", itemStack))
                        .flatMap(flag -> {
                            if (!flag.flag()) return null;
                            return zip(
                                    itemService.loadItem(id),
                                    players.loadName(owner),
                                    (item, name) -> new GhostLot(
                                            item,
                                            owner,
                                            name,
                                            price,
                                            count
                                    )
                            );
                        }))
                ;
    }

    public ResponseFuture<@Nullable GhostLot> addLot(ClientItemStack itemStack, UUID owner, long sellingDuration, int count, long price) {
        return pipeline.submit(() -> addLot0(itemStack, owner, sellingDuration, count, price));
    }

    private ResponseFuture<@Nullable GhostLot> addLot0(ClientItemStack itemStack, UUID owner, long sellingDuration, int count, long price) {
        return zip(
                remote.request(new C2SAddNewLotRequest(
                        itemStack.id(),
                        owner,
                        sellingDuration,
                        count,
                        price
                )),
                players.loadName(owner),
                (flag, name) -> {
                    if (flag == null || !flag.flag()) return null;
                    return new GhostLot(
                            itemStack,
                            owner,
                            name,
                            price,
                            count
                    );
                }
        );
    }

    public ResponseFuture<@Nullable GhostLot> makeGhostLot(ItemStack itemStack, UUID owner, int count, long lprice) {
        return itemService.pushItem(itemStack).flatMap(id -> zip(
                itemService.loadItem(id),
                players.loadName(owner),
                (item, name) -> new GhostLot(
                        item,
                        owner,
                        name,
                        lprice,
                        count
                )
        ));
    }

    private static <T1, T2, R> ResponseFuture<R> zip(ResponseFuture<T1> t, ResponseFuture<T2> t2, BiFunction<T1, T2, R> m) {
        return t.flatMap(v -> t2.map(v2 -> m.apply(v, v2)));
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof IncomingRequest r) {
            if (r.payload() instanceof S2CActualLotsUids uids) {
                loadAllLots(
                        IntIterators.wrap(uids.uids()),
                        () -> r.response(uids, new A2AFlagResponse(!closing))
                );
            } else if (r.payload() instanceof S2CActualVaultLotsUids uids) {
                loadAllVaultLots(
                        IntIterators.wrap(uids.uids()),
                        () -> r.response(uids, new A2AFlagResponse(!closing))
                );
            } else {
                ctx.fire(msg);
            }
        } else if (msg instanceof S2CLotCountChange(int uid, int newCount)) {
            ClientAucLot lot = lots.get(uid);
            if (lot == null) return;
            var baseLot = lot.lot;
            ClientAucLot v2 = new ClientAucLot(
                    new AucLot(
                            baseLot.uid(),
                            baseLot.item(),
                            baseLot.owner(),
                            baseLot.createdDate(),
                            baseLot.removalDate(),
                            newCount,
                            baseLot.lprice_for_one * newCount
                    ),
                    lot.itemStack,
                    lot.shortId,
                    lot.playerName
            );
            lots.put(v2.uid(), v2);
            indexer.insertOrUpdateLot(v2);
        } else if (msg instanceof S2CLotUpdate(AucLot newLot)) {
            placeLot(newLot);
        } else if (msg instanceof S2COnVaultLotRemovePacket(int uid)) {
            var lot = vault.remove(uid);
            if (lot != null) {
                indexer.removeVaultLot(lot);
            }
        } else if (msg instanceof S2COnLotRemovePacket(int uid)) {
            var lot = lots.remove(uid);
            if (lot != null) {
                indexer.removeLot(lot);
            }
        } else if (msg instanceof S2CVaultLotUpdate(VaultLot lot)) {
            placeVaultLot(lot);
        } else {
            ctx.fire(msg);
        }
    }

    private void loadAllLots(IntListIterator uids, Runnable r) {
        parallel(uids, r, C2SGetLotRequest::new, v -> {
            if (v != null) {
                var lot = v.lot();
                if (lot != null) placeLot(lot);
            }
        });
    }

    private void loadAllVaultLots(IntListIterator uids, Runnable r) {
        parallel(uids, r, C2SGetVaultLotRequest::new, v -> {
            if (v != null) {
                var lot = v.lot();
                if (lot != null) placeVaultLot(lot);
            }
        });
    }

    private <T extends ChannelMessage> void parallel(
            IntListIterator uids,
            Runnable done,
            IntFunction<ExpectsResponse<T>> maker,
            Consumer<@Nullable T> then
    ) {
        worker.assertThread();
        if (closing || !uids.hasNext()) {
            done.run();
            return;
        }
        AtomicInteger inflight = new AtomicInteger();
        AtomicReference<IntConsumer> request = new AtomicReference<>();
        request.set(i -> {
            worker.assertThread();
            remote.request(maker.apply(i)).then(v -> {
                var count = inflight.decrementAndGet();
                then.accept(v);
                worker.assertThread();
                if (uids.hasNext()) {
                    int next = uids.nextInt();
                    inflight.incrementAndGet();
                    pipeline.eventLoop().schedule(() -> request.get().accept(next));
                } else if (count == 0) {
                    done.run();
                }
            });
        });
        int limit = 256;
        while (limit-- > 0 && uids.hasNext()) {
            inflight.incrementAndGet();
            request.get().accept(uids.nextInt());
        }
    }

    private void placeLot(AucLot newLot) {
        itemService.loadItem(newLot.item())
                .ifEmpty(() -> log.error("Failed to load ItemStack for {}", newLot))
                .map(item -> players.loadName(newLot.owner())
                        .then(name -> {
                            var old = lots.get(newLot.uid());
                            int shortId;
                            if (old != null) {
                                shortId = old.shortId;
                            } else {
                                shortId = indexer.nextShortId();
                            }
                            var newItem = new ClientAucLot(newLot, item, shortId, name);
                            lots.put(newLot.uid(), newItem);
                            indexer.insertOrUpdateLot(newItem);
                        }))
        ;
    }

    private void placeVaultLot(VaultLot lot) {
        itemService.loadItem(lot.item())
                .ifEmpty(() -> log.error("Failed to load itemstack for {}", lot))
                .map(item -> players.loadName(lot.owner())
                        .then(name -> {
                            ClientVaultLot cvl = new ClientVaultLot(
                                    lot,
                                    name,
                                    item
                            );
                            vault.put(cvl.uid(), cvl);
                            indexer.addVaultLot(cvl);
                        }))
        ;
    }

    @Override
    public void close() {
        closing = true;
    }
}
