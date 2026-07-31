package dev.by1337.auc.handler.remote;

import dev.by1337.auc.common.backend.BackendPipelineFactory;
import dev.by1337.auc.common.handler.BAucRuntime;
import dev.by1337.auc.common.network.AucPackets;
import dev.by1337.auc.config.Config;
import dev.by1337.auc.handler.SimpleAuction;
import dev.by1337.auc.pipeline.Remote;
import dev.by1337.auc.util.libs.LibrariesUtil;
import dev.by1337.sync.bd.DatabaseSource;
import dev.by1337.sync.client.channel.status.ChannelActiveMessage;
import dev.by1337.sync.client.channel.status.ChannelInactiveMessage;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.ChannelMessage;
import dev.by1337.sync.common.channel.pipeline.*;
import dev.by1337.sync.common.packet.ExpectsResponse;
import dev.by1337.sync.common.packet.Packet;
import dev.by1337.sync.common.util.BSUtils;
import dev.by1337.sync.common.work.EventLoopWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AuctionBackendBooter {

    private static final Logger log = LoggerFactory.getLogger(AuctionBackendBooter.class);

    public static Backend bootRemote(String channelId, Connection connection, Runnable onReady, Runnable onDisabled) {
        var conn = dev.by1337.sync.bukkit.BSync.getConnection("bauc");
        var channel = conn.addChannel(channelId, "bauctionv2", c -> c
                .addRegistries(AucPackets.MAIN)
                .pipeline().addLast("bauc-ref", new ChannelHandler() {
                    @Override
                    public void init(ChannelRuntime runtime) {
                    }

                    @Override
                    public void handle(ChannelContext ctx, ChannelMessage msg) throws Exception {
                        if (msg instanceof ChannelActiveMessage) {
                            onReady.run();
                        } else if (msg instanceof ChannelInactiveMessage) {
                            onDisabled.run();
                        } else {
                            connection.write(msg);
                        }
                    }

                    @Override
                    public void close() {
                    }
                }), SimpleAuction.WORKER);
        return new Backend() {
            @Override
            public void close() {
                conn.removeChannel(channelId);
            }

            @Override
            public <T extends ChannelMessage> ResponseFuture<T> request(ExpectsResponse<T> msg) {
                return msg.request(channel.pipeline(), channel);
            }

            @Override
            public void write(Packet packet) {
                channel.write(packet);
            }
        };
    }

    public static Backend bootIntegrated(Connection connection, Config config) {
        var type = config.dbConfig.database.type;
        if (type.contains("h2")) {
            LibrariesUtil.bootH2Driver();
        } else if (type.contains("mariadb")) {
            LibrariesUtil.bootMariaDbDriver();
        }
        Pipeline backend = new Pipeline(SimpleAuction.WORKER);
        BackendPipelineFactory.make(backend);
        var databaseSource = new DatabaseSource(config.dbConfig.database, "./bsync");
        backend.registerAll(new BAucRuntime() {

            @Override
            public EventLoopWorker ioWorker() {
                return SimpleAuction.IO_WORKER;
            }

            @Override
            public DatabaseSource database() {
                return databaseSource;
            }

            @Override
            public void forEachConnections(Consumer<Connection> c) {
                c.accept(connection);
            }

            @Override
            public String name() {
                return "bauction";
            }

            @Override
            public Pipeline pipeline() {
                return backend;
            }

            @Override
            public EventLoopWorker eventLoop() {
                return SimpleAuction.WORKER;
            }

            @Override
            public Logger logger() {
                return log;
            }
        });
        return new Backend() {
            @Override
            public void close() {
                BSUtils.safe(() -> backend.closeAll().get(15, TimeUnit.SECONDS));
                BSUtils.safe(() -> databaseSource.close());
            }

            @Override
            public <T extends ChannelMessage> ResponseFuture<T> request(ExpectsResponse<T> msg) {
                return msg.request(backend, backend.local());
            }

            @Override
            public void write(Packet packet) {
                backend.execute(packet, connection);
            }
        };
    }

    public interface Backend extends Remote {
        void close();
    }
}
