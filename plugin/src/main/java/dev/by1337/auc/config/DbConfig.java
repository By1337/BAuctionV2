package dev.by1337.auc.config;

import dev.by1337.sync.bd.DatabaseSource;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;

public class DbConfig {
    public static final YamlDecoder<DbConfig> DECODER = RecordYamlDecoder.mapOf(
            DbConfig::new,
            YamlDecoder.STRING.fieldOf("server_type"),
            YamlDecoder.STRING.fieldOf("bsync_connection"),
            YamlDecoder.STRING.fieldOf("bsync_channel"),
            YamlDecoder.STRING.fieldOf("users_repo", "local://bauc_main"),
            DatabaseSource.DatabaseConfig.DECODER.fieldOf("database")
    );
    public final String server_type;
    public final String bsync_connection;
    public final String bsync_channel;
    public final String users_repo;
    public final DatabaseSource.DatabaseConfig database;

    public DbConfig(String server_type, String bsync_connection, String bsync_channel, String users_repo, DatabaseSource.DatabaseConfig database) {
        this.server_type = server_type;
        this.bsync_connection = bsync_connection;
        this.bsync_channel = bsync_channel;
        this.users_repo = users_repo;
        this.database = database;
    }
}
