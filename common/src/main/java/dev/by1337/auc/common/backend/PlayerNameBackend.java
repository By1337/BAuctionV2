package dev.by1337.auc.common.backend;

import dev.by1337.auc.common.db.BatchedK2VCache;
import dev.by1337.auc.common.handler.BAucRuntime;
import dev.by1337.auc.common.handler.GetPostChannelHandler;
import dev.by1337.auc.common.network.a2a.A2ASetPlayerNamePacket;
import dev.by1337.auc.common.network.c2s.C2SPlayerNameRequest;
import dev.by1337.auc.common.network.c2s.C2SPlayerUUIDRequest;
import dev.by1337.auc.common.network.s2c.S2CPlayerNameResponse;
import dev.by1337.auc.common.network.s2c.S2CPlayerNameUUIDResponse;
import dev.by1337.sync.bd.repo.UUID2PlayerNameRepository;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.util.BSUtils;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;

public class PlayerNameBackend extends GetPostChannelHandler {
    private BAucRuntime channel;

    private BatchedK2VCache<UUID, String> uuid2name;
    private UUID2PlayerNameRepository table;

    public PlayerNameBackend() {
        registerGet(C2SPlayerNameRequest.class, this::getName);
        registerGet(C2SPlayerUUIDRequest.class, this::getUUID);
        registerPost(A2ASetPlayerNamePacket.class, this::setName);
    }

    private ResponseFuture<S2CPlayerNameUUIDResponse> getUUID(C2SPlayerUUIDRequest r) {
        var pair = BSUtils.safe(() -> table.findByName(r.name()).orElse(null));
        if (pair == null) return new ResponseFuture<>(new S2CPlayerNameUUIDResponse(null, null));
        return new ResponseFuture<>(new S2CPlayerNameUUIDResponse(pair.value, pair.key));
    }

    @Override
    public void init(ChannelRuntime runtime) {
        if (!(runtime instanceof BAucRuntime server)) throw new IllegalArgumentException("Invalid runtime type");
        channel = server;
        uuid2name = new BatchedK2VCache<>(
                table = new UUID2PlayerNameRepository(server.database().dataSource(), server.name() + "_uuid2name_repository"),
                server.ioWorker(),
                b -> b
                        .maximumSize(65536)
                        .expireAfterAccess(Duration.ofHours(2))
        );
    }

    private void setName(A2ASetPlayerNamePacket packet) {
        BSUtils.safe(() -> uuid2name.put(packet.uuid(), packet.name()));
        channel.broadcast(packet);
    }

    private ResponseFuture<S2CPlayerNameResponse> getName(C2SPlayerNameRequest packet) {
        return new ResponseFuture<>(new S2CPlayerNameResponse(BSUtils.safe(() -> uuid2name.get(packet.uuid()).orElse("NoName"))));
    }

    @Override
    public void close() {
        try {
            uuid2name.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}