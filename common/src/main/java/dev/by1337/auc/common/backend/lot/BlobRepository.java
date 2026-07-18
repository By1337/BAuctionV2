package dev.by1337.auc.common.backend.lot;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class BlobRepository {
    private final DataSource dataSource;
    private final String tableName;

    public BlobRepository(DataSource dataSource, String tableName) {
        this.dataSource = dataSource;
        this.tableName = tableName;
        try {
            createTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create auction lot table", e);
        }
    }

    public int getMaxId() throws SQLException {
        String sql = "SELECT MAX(id) FROM %s;".formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public void createTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS `%s` (
                    id INT NOT NULL,
                    data BLOB NOT NULL,
                
                    PRIMARY KEY (id)
                ) ENGINE=InnoDB ROW_FORMAT=DYNAMIC
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public void put(int id, byte[] data) throws SQLException {
        String sql = """
                INSERT INTO `%s` (id, data)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE
                data = VALUES(data)
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.setBytes(2, data);
            statement.executeUpdate();
        }
    }

    public void putAll(Queue<Record> records, int limit) throws SQLException {
        if (records.isEmpty()) return;

        String sql = """
                INSERT INTO `%s` (id, data)
                VALUES (?, ?)
                ON DUPLICATE KEY UPDATE
                data = VALUES(data)
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);
            Record r;
            while (limit-- > 0 && (r = records.poll()) != null) {
                statement.setInt(1, r.id());
                statement.setBytes(2, r.data());
                statement.addBatch();
            }

            statement.executeBatch();
            connection.commit();
        }
    }

    public void removeAll(Queue<Integer> queue, int limit) throws SQLException {
        if (queue.isEmpty()) return;
        String sql = """
                DELETE FROM `%s`
                WHERE id = ?
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false);

            Integer i;
            while (limit-- > 0 && (i = queue.poll()) != null) {
                statement.setInt(1, i);
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        }
    }

    public boolean remove(int id) throws SQLException {
        String sql = """
                DELETE FROM `%s`
                WHERE id = ?
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() != 0;
        }
    }

    public List<Record> loadAll() throws SQLException {
        String sql = """
                SELECT id, data
                FROM `%s`
                ORDER BY id
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            List<Record> result = new ArrayList<>();

            while (rs.next()) {
                result.add(new Record(
                        rs.getInt(1),
                        rs.getBytes(2)
                ));
            }

            return result;
        }
    }

    public record Record(int id, byte[] data) {
    }
}