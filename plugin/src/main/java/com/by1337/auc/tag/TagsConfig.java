package com.by1337.auc.tag;

import com.by1337.auc.search.SearchManager;
import com.by1337.auc.util.WildcardPattern;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TagsConfig {
    private static final YamlDecoder<Map<WildcardPattern, List<String>>> P2T =
            YamlDecoder.mapOf(WildcardPattern.DECODER, YamlDecoder.STRINGS);

    public static final YamlDecoder<TagsConfig> DECODER = RecordYamlDecoder.mapOf(
            TagsConfig::new,
            P2T.fieldOf("tag_rewriter", Map.of()),
            P2T.fieldOf("tag_appender", Map.of()),
            WildcardPattern.DECODER.listOf().fieldOf("cleanup", List.of()),
            YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.DOUBLE)
                    .fieldOf("damage_extract", Map.of()),
            YamlDecoder.mapOf(YamlDecoder.STRING, SearchManager.DECODER)
                    .fieldOf("search", Map.of())
    );

    private final Map<WildcardPattern, List<String>> tag_rewriter;
    private final Map<WildcardPattern, List<String>> tag_appender;
    private final List<WildcardPattern> cleanup;
    private final Map<String, Double> damage_extract;
    public final Map<String, SearchManager> search;

    public TagsConfig(Map<WildcardPattern, List<String>> tagRewriter, Map<WildcardPattern, List<String>> tagAppender, List<WildcardPattern> cleanup, Map<String, Double> damageExtract, Map<String, SearchManager> search) {
        tag_rewriter = tagRewriter;
        tag_appender = tagAppender;
        this.cleanup = cleanup;
        damage_extract = damageExtract;
        this.search = search;
    }



    public Set<String> apply(Collection<String> tags) {
        List<String> out = new ArrayList<>(tags);
        tag_rewriter.forEach((pattern, replaces) -> {
            if (out.removeIf(pattern::testOr)) {
                out.addAll(replaces);
            }
        });
        tag_appender.forEach((pattern, append) -> {
            if (pattern.test(out)) {
                out.addAll(append);
            }
        });
        for (WildcardPattern pattern : cleanup) {
            out.removeIf(pattern::testOr);
        }
        return new HashSet<>(out);
    }


    public @Nullable String damageTag(int damage, int maxDamage) {
        double precent = ((double) damage / maxDamage) * 100D;
        for (var e : damage_extract.entrySet()) {
            if (precent <= e.getValue()) {
                return e.getKey();
            }
        }
        return null;
    }
}
