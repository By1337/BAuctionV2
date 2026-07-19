package dev.by1337.auc.handler;

import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.ClientItemStack;
import dev.by1337.auc.auc.ClientVaultLot;
import dev.by1337.auc.auc.GhostLot;
import dev.by1337.auc.auc.sort.Sorting;
import dev.by1337.auc.common.auc.log.AuctionLog;
import dev.by1337.auc.common.auc.log.LogQuery;
import dev.by1337.auc.common.auc.log.LogRecord;
import dev.by1337.auc.handler.event.ActionResult;
import dev.by1337.auc.handler.index.BitSetPool;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.registry.AucRegistries;
import dev.by1337.auc.search.PlayerVaultResult;
import dev.by1337.auc.search.SearchResult;
import dev.by1337.auc.handler.item.ItemStackRepository;
import dev.by1337.auc.handler.log.LogRepository;
import dev.by1337.auc.handler.name.PlayerName;
import dev.by1337.auc.handler.name.PlayerNameService;
import dev.by1337.auc.pipeline.LocalChannelContext;
import dev.by1337.auc.pipeline.LocalChannelHandler;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.pipeline.Remote;
import dev.by1337.auc.search.filter.SearchFilter;
import dev.by1337.auc.transaction.Transaction;
import dev.by1337.auc.user.AucUser;
import dev.by1337.sync.PlayerDataRepository;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class Auction implements LocalChannelHandler {
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

    public SearchResult search(@Nullable UUID owner, @Nullable SearchFilter filter, Sorting sorting) {
        return index.search(owner, filter, sorting);
    }

    public ResponseFuture<@Nullable Long> publishLog(AuctionLog log) {
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

    @Nullable
    public UUID name2uuid(String normalName) {
        return index.name2uuid(normalName);
    }

    public Iterator<String> normalPlayerNamesIterator() {
        return index.normalPlayerNamesIterator();
    }

    public PlayerDataRepository<AucUser> users() {
        return auction.users();
    }

    public ResponseFuture<@Nullable List<LogRecord>> loadLogs(LogQuery query) {
        return log.loadLogs(query);
    }

    public void registerLogListener(Consumer<LogRecord> c) {
        log.registerLogListener(c);
    }

    public ResponseFuture<@Nullable LogRecord> getLog(long uid) {
        return log.getLog(uid);
    }

    public AucRegistries registries() {
        return auction.registries();
    }

    public ResponseFuture<ActionResult> subtractOrRemoveLot(ClientAucLot lot0, int count) {
        return repo.subtractOrRemoveLot(lot0, count);
    }

    @Nullable
    public BitSetPool.PooledBitSet findLotsWithTags(int @Nullable [] and, int @Nullable [] not) {
        return index.findLotsWithTags(and, not);
    }

    @Nullable
    public BitSetPool.PooledBitSet findLotsWithTags(int @Nullable [] @Nullable [] ands, int @Nullable [] @Nullable [] nots) {
        return index.findLotsWithTags(ands, nots);
    }

    public int getPlayerOwnedLotsCount(UUID key) {
        return index.getPlayerOwnedLotsCount(key);
    }

    @Override
    public void handle(LocalChannelContext ctx, ChannelMessage msg) throws Exception {
        ctx.fire(msg);
    }

    @Override
    public void close() {

    }


}
