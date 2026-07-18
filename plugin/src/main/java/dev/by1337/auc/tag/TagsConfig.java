package dev.by1337.auc.tag;

import dev.by1337.auc.search.SearchManager;
import dev.by1337.auc.tag.tester.WildcardTagTester;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TagsConfig {
    private static final YamlDecoder<Map<WildcardTagTester.Tester, List<String>>> P2TAGS =
            YamlDecoder.mapOf(WildcardTagTester.DECODER, YamlDecoder.STRINGS);

    private static final YamlDecoder<Map<String, WildcardTagTester.Tester>> TAGS2P =
            YamlDecoder.mapOf(YamlDecoder.STRING, WildcardTagTester.DECODER);

    public static final YamlDecoder<TagsConfig> DECODER = RecordYamlDecoder.mapOf(
            TagsConfig::new,
            P2TAGS.fieldOf("tag_rewriter", Map.of()),
            TAGS2P.fieldOf("tag_appender", Map.of()),
            WildcardTagTester.DECODER.listOf().fieldOf("cleanup", List.of()),
            YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.DOUBLE)
                    .fieldOf("damage_extract", Map.of()),
            YamlDecoder.mapOf(YamlDecoder.STRING, SearchManager.DECODER)
                    .fieldOf("search", Map.of())
    );

    private final Map<WildcardTagTester.Tester, List<String>> tag_rewriter;
    private final Map<String, WildcardTagTester.Tester> tag_appender;
    private final List<WildcardTagTester.Tester> cleanup;
    private final Map<String, Double> damage_extract;
    public final Map<String, SearchManager> search;

    public TagsConfig(Map<WildcardTagTester.Tester, List<String>> tagRewriter, Map<String, WildcardTagTester.Tester> tagAppender, List<WildcardTagTester.Tester> cleanup, Map<String, Double> damageExtract, Map<String, SearchManager> search) {
        tag_rewriter = tagRewriter;
        tag_appender = tagAppender;
        this.cleanup = cleanup;
        damage_extract = damageExtract;
        this.search = search;
    }



    public Set<String> apply(Collection<String> tags) {
        List<String> out = new ArrayList<>(tags);
        tag_rewriter.forEach((pattern, replaces) -> {
            if (out.removeIf(pattern::test)) {
                out.addAll(replaces);
            }
        });
        tag_appender.forEach((append, pattern) -> {
            if (pattern.test(out)) {
                out.add(append);
            }
        });
        for (WildcardTagTester.Tester pattern : cleanup) {
            out.removeIf(pattern::test);
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
