package com.by1337.auc.common.backend.log;

import com.by1337.auc.common.auc.log.AuctionLog;
import com.by1337.auc.common.auc.log.LogQuery;
import com.by1337.auc.common.auc.log.LogRecord;
import com.zaxxer.hikari.HikariDataSource;
import dev.by1337.sync.bd.repo.UUIDUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jspecify.annotations.Nullable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class AuctionLogRepository {
    private final HikariDataSource dataSource;
    private final String tableName;

    public AuctionLogRepository(HikariDataSource dataSource, String tableName) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        try {
            createTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create auction lot table", e);
        }
    }


    public void createTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS `%s` (
                    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                    timestamp BIGINT NOT NULL,
                    actor BINARY(16),
                    subject BINARY(16),
                    type VARCHAR(64) NOT NULL,
                    payload BLOB,
                
                    INDEX idx_timestamp (timestamp),
                    INDEX idx_actor_timestamp (actor, timestamp),
                    INDEX idx_subject_timestamp (subject, timestamp),
                    INDEX idx_type_timestamp (type, timestamp)
                ) ENGINE=InnoDB ROW_FORMAT=DYNAMIC""".formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public long insert(AuctionLog log) throws SQLException {
        String sql = """
                INSERT INTO `%s` (`timestamp`, `actor`, `subject`, `type`, `payload`)
                VALUES (?, ?, ?, ?, ?)
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, log.timestamp());
            statement.setBytes(2, uuidToBytesNullable(log.actor()));
            statement.setBytes(3, uuidToBytesNullable(log.subject()));
            statement.setString(4, log.type());
            statement.setBytes(5, payloadToBytes(log));

            int affectedRows = statement.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Insert failed, no rows affected");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Insert failed, no ID obtained");
                }
            }
        }
    }

    public Optional<LogRecord> getById(long id) throws SQLException {
        String sql = """
                SELECT `id`, `type`, `payload`
                FROM `%s`
                WHERE `id` = ?
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
                return Optional.empty();
            }
        }
    }

    private LogRecord mapResultSet(ResultSet rs) throws SQLException {
        long id = rs.getInt(1);
        var type = rs.getString(2);
        byte[] payload = rs.getBytes(3);
        var f = AuctionLog.REGISTRY.creator(type);
        if (f == null) throw new SQLException("Unknown lot type " + type);
        return new LogRecord(id, f.apply(Unpooled.wrappedBuffer(payload)));
    }

    private static boolean append(
            StringBuilder sql,
            List<Object> args,
            boolean first,
            Object value,
            String condition
    ) {
        if (value == null) return first;

        sql.append(first ? "WHERE " : " AND ");
        sql.append(condition);
        args.add(value);
        return false;
    }

    public List<LogRecord> findByFilter(LogQuery query) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT `id`, `type`, `payload`
                FROM `%s`
                
                """.formatted(tableName));
        List<Object> args = new ArrayList<>();
        boolean first = true;
        first = append(sql, args, first, query.afterId(), "id > ?");
        first = append(sql, args, first, query.beforeId(), "id < ?");
        first = append(sql, args, first, query.afterTimestamp(), "timestamp >= ?");
        first = append(sql, args, first, query.beforeTimestamp(), "timestamp <= ?");
        first = append(sql, args, first, query.actor(), "actor = ?");
        first = append(sql, args, first, query.subject(), "subject = ?");
        first = append(sql, args, first, query.type(), "type = ?");
        if (first) sql.append(" ");
        sql.append("ORDER BY id DESC LIMIT ?");
        args.add(query.limit());

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            for (int i = 0; i < args.size(); i++) {
                var o = args.get(i);
                if (o instanceof Long l) {
                    statement.setLong(i + 1, l);
                } else if (o instanceof UUID uuid) {
                    statement.setBytes(i + 1, UUIDUtil.uuidToBytes(uuid));
                } else if (o instanceof Integer l) {
                    statement.setInt(i + 1, l);
                } else if (o instanceof String s) {
                    statement.setString(i + 1, s);
                } else {
                    throw new SQLException("Unknown type " + o);
                }
            }

            try (ResultSet rs = statement.executeQuery()) {
                List<LogRecord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapResultSet(rs));
                }
                return result;
            }
        }
    }

    private byte[] payloadToBytes(AuctionLog log) {
        ByteBuf buf = Unpooled.buffer(100);
        try {
            log.writePayload(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes.length > 0 ? bytes : null;
        } finally {
            buf.release();
        }
    }

    private static byte[] uuidToBytesNullable(@Nullable UUID uuid) {
        if (uuid == null) return null;
        return UUIDUtil.uuidToBytes(uuid);
    }
}