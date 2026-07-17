package com.by1337.auc.common.backend.lot;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public void createTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS `%s` (
                    id INT NOT NULL AUTO_INCREMENT,
                    data BLOB NOT NULL,

                    PRIMARY KEY (id)
                ) ENGINE=InnoDB ROW_FORMAT=DYNAMIC
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public int insert(byte[] data) throws SQLException {
        String sql = """
                INSERT INTO `%s` (data)
                VALUES (?)
                """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setBytes(1, data);
            statement.executeUpdate();

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to obtain generated id.");
                }
                return rs.getInt(1);
            }
        }
    }
    public boolean update(int id, byte[] data) throws SQLException {
        String sql = """
            UPDATE `%s`
            SET data = ?
            WHERE id = ?
            """.formatted(tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBytes(1, data);
            statement.setInt(2, id);

            return statement.executeUpdate() != 0;
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