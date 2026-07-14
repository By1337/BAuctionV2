package com.by1337.auc.handler;

import com.by1337.auc.auc.ClientAucLot;
import com.by1337.auc.auc.ClientVaultLot;
import com.by1337.auc.auc.GhostLot;
import com.by1337.auc.auc.sort.Sorting;
import com.by1337.auc.handler.event.ActionResult;
import com.by1337.auc.handler.index.LotsIndexer;
import com.by1337.auc.handler.index.search.PlayerVaultResult;
import com.by1337.auc.handler.index.search.SearchResult;
import com.by1337.auc.handler.name.PlayerName;
import com.by1337.auc.handler.name.PlayerNameService;
import com.by1337.auc.pipeline.LocalChannelContext;
import com.by1337.auc.pipeline.LocalChannelHandler;
import com.by1337.auc.pipeline.LocalPipeline;
import com.by1337.auc.pipeline.Remote;
import com.by1337.auc.search.filter.SearchFilter;
import com.by1337.auc.transaction.Transaction;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Auction implements LocalChannelHandler {
    private static final ResponseFuture<ActionResult> DENY = new ResponseFuture<>(ActionResult.deny());
    private static final ResponseFuture<ActionResult> SUCCESS = new ResponseFuture<>(ActionResult.success());
    private LocalPipeline pipeline;
    private Remote remote;
    private LotsRepository repo;
    private LotsIndexer index;
    private PlayerNameService players;

    @Override
    public void init(LocalPipeline pipeline, Remote remote) {
        this.pipeline = pipeline;
        this.remote = remote;
        repo = pipeline.get(LotsRepository.class);
        players = pipeline.get(PlayerNameService.class);
        index = pipeline.get(LotsIndexer.class);
    }

    public <T> ResponseFuture<T> apply(Transaction<T> t) {
        return t.apply(this);
    }

/*    public ResponseFuture<ActionResult> tryBuyItem(UUID who, int uid) {
        ClientAucLot lot = getLot(uid);
        if (lot == null) {
            BAuction.sendMessage("outdated_lot", who);
            return DENY;
        }
        var eco = BAuction.economy();
        double balance = eco.getBalance(who);
        if (balance < lot.price) {
            BAuction.sendMessage("insufficient_balance", who);
            return DENY;
        }
        eco.withdrawPlayer(who, lot.price);
        return null;
    }*/

    public @Nullable ClientAucLot getLot(int uid) {
        return repo.getLot(uid);
    }

    public @Nullable ClientVaultLot getVaultLot(int uid) {
        return repo.getVaultLot(uid);
    }

    public ResponseFuture<ActionResult> removeLot(ClientAucLot lot0) {
        return repo.removeLot(lot0);
    }

    public ResponseFuture<ActionResult> removeVaultLot(ClientVaultLot lot0) {
        return repo.removeVaultLot(lot0);
    }

    public ResponseFuture<Boolean> moveToVault(ClientAucLot lot, UUID owner) {
        return repo.moveToVault(lot, owner);
    }

    public ResponseFuture<ActionResult> readdVaultLot(ClientVaultLot vault) {
        return repo.readdVaultLot(vault);
    }

    public ResponseFuture<ActionResult> addToVault(ItemStack itemStack, int count, UUID owner, long price) {
        return repo.addToVault(itemStack, count, owner, price);
    }

    public void setName(UUID uuid, String name) {
        players.setName(uuid, name);
    }

    public ResponseFuture<@NotNull PlayerName> loadName(UUID uuid) {
        return players.loadName(uuid);
    }

    public ResponseFuture<@Nullable GhostLot> addLot(ItemStack itemStack, UUID owner, long sellingDuration, int count, long price) {
        return repo.addLot(itemStack, owner, sellingDuration, count, price);
    }

    public SearchResult search(@Nullable SearchFilter filter, Sorting sorting) {
        return index.search(filter, sorting);
    }

    public SearchResult search(UUID owner, @Nullable SearchFilter filter, Sorting sorting) {
        return index.search(owner, filter, sorting);
    }


    public PlayerVaultResult playerVaultLots(UUID player) {
        return index.playerVaultLots(player);
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        ctx.fire(msg);
    }

    @Override
    public void close() {

    }


}
