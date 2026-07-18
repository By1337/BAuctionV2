package dev.by1337.auc.util;

import dev.by1337.yaml.decoder.YamlDecoder;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public record WildcardPattern(List<Pattern> patterns) {
    public static final YamlDecoder<WildcardPattern> DECODER = YamlDecoder.STRING
            .map(s -> Arrays.asList(s.split(",")))
            .and(Pattern.DECODER.listOf())
            .map(WildcardPattern::new);

    public boolean test(Collection<String> text) {
        loop:
        for (Pattern pattern : patterns) {
            var inverted = pattern.invert;
            if (inverted) {
                if (pattern.allOf(text)) continue loop;
            } else {
                for (String s : text) {
                    if (pattern.test(s)) continue loop;
                }
            }
            return false;
        }
        return true;
    }

    public boolean testOr(String text) {
        for (Pattern pattern : patterns) {
            if (pattern.test(text)) return true;
        }
        return false;
    }

    public boolean testAnd(String text) {
        for (Pattern pattern : patterns) {
            if (!pattern.test(text)) return false;
        }
        return true;
    }

    public record Pattern(String pattern, boolean invert) {
        public static final YamlDecoder<Pattern> DECODER = YamlDecoder.STRING.map(s -> {
            if (s.startsWith("!")) return new Pattern(s.substring(1), true);
            return new Pattern(s, false);
        });

        public boolean test(String text) {
            return invert != WildcardMatcher.match(pattern, text);
        }

        public boolean allOf(Collection<String> l) {
            for (String s : l) {
                if (!test(s)) return false;
            }
            return true;
        }
    }
}
