package dev.by1337.auc.common.backend.item;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;
import java.util.OptionalInt;

public class ItemRepository {
    private final DataSource dataSource;
    private final String tableName;

    public ItemRepository(DataSource dataSource, String tableName) {
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
                    id INT NOT NULL AUTO_INCREMENT,
                    hash BINARY(32) NOT NULL,
                    data MEDIUMBLOB NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                    PRIMARY KEY (id),
                    UNIQUE KEY uk_hash (hash)
                ) ENGINE=InnoDB ROW_FORMAT=DYNAMIC
                """.formatted(tableName);

        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    public Optional<byte[]> get(int id) throws SQLException {
        String sql = """
                SELECT data
                FROM `%s`
                WHERE id = ?
                """.formatted(tableName);

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(rs.getBytes(1));
            }
        }
    }

    public OptionalInt findByHash(byte[] sha256) throws SQLException {
        String sql = """
                SELECT id
                FROM `%s`
                WHERE hash = ?
                """.formatted(tableName);

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setBytes(1, sha256);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return OptionalInt.empty();
                }
                return OptionalInt.of(rs.getInt(1));
            }
        }
    }

    public int putIfAbsent(byte[] hash, byte[] data) throws SQLException {
        String sql = """
            INSERT INTO `%s` (hash, data)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE
                id = LAST_INSERT_ID(id)
            """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setBytes(1, hash);
            statement.setBytes(2, data);
            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("INSERT returned no generated key");
                }
                return rs.getInt(1);
            }
        }
    }
}