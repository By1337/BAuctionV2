package dev.by1337.auc.handler;

import dev.by1337.auc.config.Config;
import dev.by1337.auc.handler.eco.EcoGiver;
import dev.by1337.auc.handler.event.UserMailEvent;
import dev.by1337.auc.handler.index.LotsIndexer;
import dev.by1337.auc.handler.index.Tag2IdService;
import dev.by1337.auc.handler.item.ItemStackRepository;
import dev.by1337.auc.handler.log.LogRepository;
import dev.by1337.auc.handler.name.PlayerNameService;
import dev.by1337.auc.handler.notify.LotSoldNotifier;
import dev.by1337.auc.handler.remote.AuctionBackendBooter;
import dev.by1337.auc.lifecycle.AucLifecycle;
import dev.by1337.auc.pipeline.LocalPipeline;
import dev.by1337.auc.pipeline.request.LocalRequestsHandler;
import dev.by1337.auc.registry.AucRegistries;
import dev.by1337.auc.user.AucUser;
import dev.by1337.sync.DataManager;
import dev.by1337.sync.PlayerDataRepository;
import dev.by1337.sync.common.util.BSUtils;
import dev.by1337.sync.common.work.EventLoopWorker;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleAuction {
    public static final EventLoopWorker WORKER = new EventLoopWorker("bauc");
    public static final EventLoopWorker IO_WORKER = new EventLoopWorker("bauc-io");
    private static final Logger log = LoggerFactory.getLogger(SimpleAuction.class);

    private final LocalPipeline pipeline;

    private final Config config;
    private final LotsRepository lotsRepository;
    private final LotsIndexer indexer;
    private final Tag2IdService tag2id = Tag2IdService.INSTANCE;
    private final PlayerNameService nameService;
    private final Auction auction;
    private final PlayerDataRepository<AucUser> users;
    private final LogRepository logRepo;
    private final AucRegistries registries;
    private final AuctionBackendBooter.Backend backend;


    public SimpleAuction(Config config, Plugin plugin, AucLifecycle lifecycle) {
        this.config = config;
        registries = new AucRegistries();
        this.config.boot(this);
        lifecycle.bootAucRegistries(registries);
        registries.writeLock();

        if (registries.sorting.size() == 0) throw new IllegalStateException("The sort list cannot be empty!");
        if (registries.category.size() == 0) throw new IllegalStateException("The category list cannot be empty!");


        users = PlayerDataRepository.create(
                "main",
                plugin,
                new DataManager<>() {
                    @Override
                    public @NotNull AucUser read(byte @Nullable [] data, @NotNull UUID key) {
                        return AucUser.read(data, key);
                    }

                    @Override
                    public byte @NotNull [] write(@NotNull AucUser data, @NotNull UUID key) {
                        return data.write();
                    }

                    @Override
                    public void acceptMail(@NotNull AucUser data, @NotNull String mail, @NotNull UUID key) {
                        pipeline.execute(new UserMailEvent(data, mail));
                    }

                    @Override
                    public void forceUnlock(UUID key) {
                    }
                }
        );
        pipeline = new LocalPipeline(WORKER);
        pipeline
                .addLast("$r", new LocalRequestsHandler())
                .addLast("item_stack_repository", new ItemStackRepository(config))
                .addLast("client_lots_repository", lotsRepository = new LotsRepository())
                .addLast("indexer", indexer = new LotsIndexer())
                .addLast("name_service", nameService = new PlayerNameService(plugin))
                .addLast("lot", logRepo = new LogRepository())
                .addLast("auction", auction = new Auction())
                .addLast("eco_giver", new EcoGiver())
                .addLast("lot_sold_notifier", new LotSoldNotifier())
        ;
        lifecycle.bootAucPipeline(pipeline);

        backend = AuctionBackendBooter.bootLocal(pipeline, config);
        onReady();
       // backend = AuctionBackendBooter.bootRemote(pipeline, config, this::onReady);
    }
    private void onReady(){
        pipeline.initAll(backend, this);
    }

    public void close() {
        BSUtils.safe(() -> pipeline.closeAll().get(15, TimeUnit.SECONDS));
        BSUtils.safe(backend::close);
        BSUtils.safe(users::close);
    }

    public LocalPipeline pipeline() {
        return pipeline;
    }

    public Auction auction() {
        return auction;
    }

    public LotsRepository lotsRepository() {
        return lotsRepository;
    }

    public Tag2IdService tag2id() {
        return tag2id;
    }

    public LotsIndexer indexer() {
        return indexer;
    }

    public PlayerDataRepository<AucUser> users() {
        return users;
    }

    public LogRepository logRepo() {
        return logRepo;
    }

    public AucRegistries registries() {
        return registries;
    }

    public EventLoopWorker worker() {
        return WORKER;
    }

    public EventLoopWorker ioWorker() {
        return IO_WORKER;
    }

    public Config config() {
        return config;
    }
}