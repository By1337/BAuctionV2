package dev.by1337.auc.config;

import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.jetbrains.annotations.Nullable;

public class WebConfig {
    public static final YamlDecoder<WebConfig> DECODER = RecordYamlDecoder.mapOf(
            WebConfig::new,
            YamlDecoder.STRING.fieldOf("static", "baucv2"),
            YamlDecoder.STRING.fieldOf("url", "wss://btunnel.bdev.space/api/ws"),
            YamlDecoder.STRING.fieldOf("secret"),
            YamlDecoder.STRING.fieldOf("description")
    );
    public final String staticContent;
    public final String url;
    public final @Nullable String secret;
    public final @Nullable String description;

    public WebConfig(String staticContent, String url, @Nullable String secret, @Nullable String description) {
        this.staticContent = staticContent;
        this.url = url;
        this.secret = secret;
        this.description = description;
    }
}
