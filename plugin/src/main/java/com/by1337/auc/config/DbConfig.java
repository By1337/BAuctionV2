package com.by1337.auc.config;

import dev.by1337.sync.bd.DatabaseSource;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;

public class DbConfig {
    public static final YamlDecoder<DbConfig> DECODER = RecordYamlDecoder.mapOf(
            DbConfig::new,
            DatabaseSource.DatabaseConfig.DECODER.fieldOf("database")
    );
    public final DatabaseSource.DatabaseConfig database;

    public DbConfig(DatabaseSource.DatabaseConfig database) {
        this.database = database;
    }
}
