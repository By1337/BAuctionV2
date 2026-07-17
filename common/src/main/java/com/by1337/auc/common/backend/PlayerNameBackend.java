package com.by1337.auc.common.backend;

import com.by1337.auc.common.handler.BAucRuntime;
import com.by1337.auc.common.handler.GetPostChannelHandler;
import com.by1337.auc.common.network.a2a.A2ASetPlayerNamePacket;
import com.by1337.auc.common.network.c2s.C2SPlayerNameRequest;
import com.by1337.auc.common.network.s2c.S2CPlayerNameResponse;
import dev.by1337.sync.bd.repo.UUID2VarChar16Repository;
import dev.by1337.sync.bd.table.K2VCache;
import dev.by1337.sync.common.callback.ResponseFuture;
import dev.by1337.sync.common.channel.pipeline.ChannelRuntime;
import dev.by1337.sync.common.util.BSUtils;
import dev.by1337.sync.common.util.SneakyThrow;

import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;

public class PlayerNameBackend extends GetPostChannelHandler {
    private BAucRuntime channel;

    private K2VCache<UUID, String> uuid2name;

    public PlayerNameBackend() {
        registerGet(C2SPlayerNameRequest.class, this::getName);
        registerPost(A2ASetPlayerNamePacket.class, this::setName);
    }

    @Override
    public void init(ChannelRuntime runtime) {
        if (!(runtime instanceof BAucRuntime server)) throw new IllegalArgumentException("Invalid runtime type");
        channel = server;
        uuid2name = new K2VCache<>(
                new UUID2VarChar16Repository(server.database().dataSource(), server.name() + "_uuid2name_repository"),
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