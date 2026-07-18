package dev.by1337.auc.common.auc.log;

import dev.by1337.auc.common.registry.NetworkRegistry;
import dev.by1337.sync.common.packet.ByteBufCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface AuctionLog {
    NetworkRegistry<AuctionLog> REGISTRY = new NetworkRegistry<>();

    long timestamp();

    UUID actor();

    @Nullable
    UUID subject();

    String type();

    void writePayload(ByteBuf buf);

    default void write(ByteBuf buf, int protocolVersion) {
        if (AuctionLog.REGISTRY.creator(type()) == null) {
            throw new EncoderException("Unknown AuctionLog type " + type());
        }
        ByteBufCodecs.writeUtf8(buf, type());
        writePayload(buf);
    }

    static AuctionLog read(ByteBuf buf, int protocolVersion) {
        String type = ByteBufCodecs.readUtf8(buf);
        var f = AuctionLog.REGISTRY.creator(type);
        if (f == null) throw new DecoderException("Unknown AuctionLog type " + type);
        return f.apply(buf);
    }
}