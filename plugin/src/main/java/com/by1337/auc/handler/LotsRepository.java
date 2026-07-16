package com.by1337.auc.handler;

import com.by1337.auc.auc.ClientAucLot;
import com.by1337.auc.auc.ClientVaultLot;
import com.by1337.auc.auc.GhostLot;
import com.by1337.auc.common.auc.AucLot;
import com.by1337.auc.common.auc.VaultLot;
import com.by1337.auc.common.network.a2a.A2AFlagResponse;
import com.by1337.auc.common.network.c2s.*;
import com.by1337.auc.common.network.s2c.*;
import com.by1337.auc.handler.event.ActionResult;
import com.by1337.auc.handler.index.LotsIndexer;
import com.by1337.auc.handler.item.ItemStackRepository;
import com.by1337.auc.handler.name.PlayerNameService;
import com.by1337.auc.pipeline.LocalChannelContext;
import com.by1337.auc.pipeline.LocalChannelHandler;
import com.by1337.auc.pipeline.LocalPipeline;
import com.by1337.auc.pipeline.Remote;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class LotsRepository implements LocalChannelHandler {
    private static final long VAULT_STORE_DURATION_MS = TimeUnit.DAYS.toMillis(1);
    private static final Logger log = LoggerFactory.getLogger(LotsRepository.class);
    private LocalPipeline pipeline;
    private Remote remote;
    private ItemStackRepository itemService;
    private LotsIndexer indexer;
    private PlayerNameService players;

    private final Int2ObjectOpenHashMap<ClientAucLot> lots = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<ClientVaultLot> vault = new Int2ObjectOpenHashMap<>();

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.pipeline = pipeline;
        this.remote = remote;
        itemService = pipeline.get(ItemStackRepository.class);
        indexer = pipeline.get(LotsIndexer.class);
        players = pipeline.get(PlayerNameService.class);
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

    private static <T1, T2, R> ResponseFuture<R> zip(ResponseFuture<T1> t, ResponseFuture<T2> t2, BiFunction<T1, T2, R> m) {
        return t.flatMap(v -> t2.map(v2 -> m.apply(v, v2)));
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        if (msg instanceof S2CLotCountChange(int uid, int newCount)) {
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
        } else if (msg instanceof S2CLotUpdate(com.by1337.auc.common.auc.AucLot newLot)) {
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
        } else {
            if (msg instanceof S2CRemoveVaultLotPacket(int uid)) {
                var lot = vault.remove(uid);
                if (lot != null) {
                    indexer.removeVaultLot(lot);
                }
            } else if (msg instanceof S2CRemoveLotPacket(int uid)) {
                var lot = lots.remove(uid);
                if (lot != null) {
                    indexer.removeLot(lot);
                }
            } else if (msg instanceof S2CVaultLotUpdate(VaultLot lot)) {
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
            ctx.fire(msg);
        }
    }

    @Override
    public void close() {

    }
}
