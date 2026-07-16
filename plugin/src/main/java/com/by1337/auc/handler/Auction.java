package com.by1337.auc.handler;

import com.by1337.auc.auc.ClientAucLot;
import com.by1337.auc.auc.ClientItemStack;
import com.by1337.auc.auc.ClientVaultLot;
import com.by1337.auc.auc.GhostLot;
import com.by1337.auc.auc.sort.Sorting;
import com.by1337.auc.common.auc.log.AuctionLog;
import com.by1337.auc.common.auc.log.LogRecord;
import com.by1337.auc.handler.event.ActionResult;
import com.by1337.auc.handler.index.LotsIndexer;
import com.by1337.auc.registry.AucRegistries;
import com.by1337.auc.search.PlayerVaultResult;
import com.by1337.auc.search.SearchResult;
import com.by1337.auc.handler.item.ItemStackRepository;
import com.by1337.auc.handler.log.LogRepository;
import com.by1337.auc.handler.name.PlayerName;
import com.by1337.auc.handler.name.PlayerNameService;
import com.by1337.auc.pipeline.LocalChannelContext;
import com.by1337.auc.pipeline.LocalChannelHandler;
import com.by1337.auc.pipeline.LocalPipeline;
import com.by1337.auc.pipeline.Remote;
import com.by1337.auc.search.filter.SearchFilter;
import com.by1337.auc.transaction.Transaction;
import com.by1337.auc.user.AucUser;
import dev.by1337.sync.PlayerDataRepository;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class Auction implements LocalChannelHandler {
    private static final ResponseFuture<ActionResult> DENY = new ResponseFuture<>(ActionResult.deny());
    private static final ResponseFuture<ActionResult> SUCCESS = new ResponseFuture<>(ActionResult.success());
    private LocalPipeline pipeline;
    private Remote remote;
    private LotsRepository repo;
    private LotsIndexer index;
    private PlayerNameService players;
    private LogRepository log;
    private ItemStackRepository itemService;
    private SimpleAuction auction;

    @Override
    public void init(LocalPipeline pipeline, Remote remote, SimpleAuction auction) {
        this.pipeline = pipeline;
        this.remote = remote;
        this.auction = auction;
        itemService = pipeline.get(ItemStackRepository.class);
        repo = pipeline.get(LotsRepository.class);
        players = pipeline.get(PlayerNameService.class);
        index = pipeline.get(LotsIndexer.class);
        log = pipeline.get(LogRepository.class);
    }

    public <T> ResponseFuture<T> apply(Transaction<T> t) {
        return t.apply(this);
    }

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

    public ResponseFuture<@Nullable Integer> publishLog(AuctionLog log) {
        return this.log.publishLog(log);
    }

    public ResponseFuture<@Nullable ClientItemStack> loadItem(int id) {
        return itemService.loadItem(id);
    }

    public ResponseFuture<@Nullable Integer> pushItem(ItemStack itemStack) {
        return itemService.pushItem(itemStack);
    }

    public PlayerVaultResult playerVaultLots(UUID player) {
        return index.playerVaultLots(player);
    }

    public PlayerDataRepository<AucUser> users() {
        return auction.users();
    }

    public ResponseFuture<@Nullable List<LogRecord>> loadLogs(long after, @Nullable UUID actor, @Nullable UUID subject, @Nullable String type, int limit) {
        return log.loadLogs(after, actor, subject, type, limit);
    }

    public void registerLogListener(Consumer<LogRecord> c) {
        log.registerLogListener(c);
    }

    public ResponseFuture<@Nullable LogRecord> getLog(int uid) {
        return log.getLog(uid);
    }

    public AucRegistries registries() {
        return auction.registries();
    }

    public ResponseFuture<ActionResult> subtractOrRemoveLot(ClientAucLot lot0, int count) {
        return repo.subtractOrRemoveLot(lot0, count);
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        ctx.fire(msg);
    }

    @Override
    public void close() {

    }


}
