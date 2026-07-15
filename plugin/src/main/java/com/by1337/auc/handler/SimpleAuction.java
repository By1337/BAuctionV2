package com.by1337.auc.handler;

import com.by1337.auc.config.Config;
import com.by1337.auc.handler.backend.ClientItemServiceBackend;
import com.by1337.auc.handler.backend.ClientLotsRepositoryBackend;
import com.by1337.auc.handler.backend.LogRepositoryBackend;
import com.by1337.auc.handler.backend.PlayerNameBackend;
import com.by1337.auc.handler.index.LotsIndexer;
import com.by1337.auc.handler.index.Tag2IdService;
import com.by1337.auc.handler.item.ItemStackRepository;
import com.by1337.auc.handler.log.LogRepository;
import com.by1337.auc.handler.name.PlayerNameService;
import com.by1337.auc.pipeline.LocalPipeline;
import com.by1337.auc.pipeline.Remote;
import com.by1337.auc.user.AucUser;
import dev.by1337.sync.DataManager;
import dev.by1337.sync.PlayerDataRepository;
import dev.by1337.sync.client.channel.ClientChannelRuntime;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.channel.pipeline.Connection;
import dev.by1337.sync.common.channel.pipeline.Pipeline;
import dev.by1337.sync.common.channel.pipeline.SocketConnection;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import dev.by1337.sync.common.work.EventLoopWorker;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SimpleAuction {

    private static final Logger log = LoggerFactory.getLogger(SimpleAuction.class);
    private final EventLoopWorker worker;
    private final LocalPipeline pipeline;
    private final Pipeline backend;
    private final Config config;
    private final LotsRepository lotsRepository;
    private final LotsIndexer indexer;
    private final Tag2IdService tag2id = Tag2IdService.INSTANCE;
    private final PlayerNameService nameService;
    private final Auction auction;
    private final PlayerDataRepository<AucUser> users;
    private final LogRepository logRepo;

    public SimpleAuction(Config config, Plugin plugin) {
        this.config = config;
        users = PlayerDataRepository.create(
                "main",
                plugin,
                new DataManager<>() {
                    @Override
                    public @NotNull AucUser read(byte @Nullable [] data) {
                        return AucUser.read(data);
                    }

                    @Override
                    public byte @NotNull [] write(@NotNull AucUser data) {
                        return data.write();
                    }

                    @Override
                    public void acceptMail(@NotNull AucUser data, @NotNull String mail) {
                        data.acceptMail(mail);
                    }

                    @Override
                    public void forceUnlock(UUID key) {
                    }
                }
        );
        worker = new EventLoopWorker("bauc");
        pipeline = new LocalPipeline(worker);
        pipeline
                .addLast("item_stack_repository", new ItemStackRepository(config))
                .addLast("client_lots_repository", lotsRepository = new LotsRepository())
                .addLast("indexer", indexer = new LotsIndexer())
                .addLast("name_service", nameService = new PlayerNameService(plugin))
                .addLast("log", logRepo = new LogRepository())
                .addLast("auction", auction = new Auction())
        ;
        backend = new Pipeline(worker);
        backend
                .addLast("item_stack_repository", new ClientItemServiceBackend())
                .addLast("lots_repository", new ClientLotsRepositoryBackend())
                .addLast("name_repository", new PlayerNameBackend())
                .addLast("log_repository", new LogRepositoryBackend())
        ;
        backend.registerAll(new ClientChannelRuntime() {
            @Override
            public Connection remote() {
                return new Connection() {
                    @Override
                    public void write(ChannelMessage msg) {
                        //    log.info("backend -> local {}", msg);
                        pipeline.execute(msg);
                    }

                    @Override
                    public SocketConnection transport() {
                        return this::write;
                    }
                };
            }

            @Override
            public Pipeline pipeline() {
                return backend;
            }

            @Override
            public EventLoopWorker eventLoop() {
                return worker;
            }

            @Override
            public Logger logger() {
                return log;
            }
        });
        pipeline.initAll(new Remote() {
            @Override
            public <T extends ChannelMessage> ResponseFuture<T> request(ExpectsResponse<T> msg) {
                //    log.info("local -> request backend {}", msg);
                return msg.request(backend, backend.local());
            }

            @Override
            public void write(Packet packet) {
                //   log.info("local -> write backend {}", packet);
                backend.local().write(packet);
            }
        }, this);

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
}