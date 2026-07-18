
package dev.by1337.auc.tag.tester;

import dev.by1337.auc.util.WildcardMatcher;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.YamlDecoder;

import java.util.Collection;
import java.util.Locale;

public class WildcardTagTester {
    public static final YamlDecoder<Tester> DECODER = YamlDecoder.STRINGS.flatMap(l -> {
        try {
            return DataResult.success(parse(String.join("|", l)));
        } catch (Exception e) {
            return DataResult.error(e.getMessage());
        }
    });

    public static Tester parse(String input) {
        ExpReader reader = new ExpReader(input);
        Tester d = parseOr(reader);
        if (reader.next() != '\0') {
            throw reader.expected("EOF");
        }
        return d;
    }

    public static Tester parseOr(ExpReader reader) {
        Tester tester = parseAnd(reader);
        while (true) {
            switch (reader.next()) {
                case ' ' -> {
                }
                case '|' -> tester = tester.or(parseAnd(reader));
                case '\0' -> {
                    return tester;
                }
                default -> {
                    reader.back();
                    return tester;
                }
            }
        }
    }

    public static Tester parseAnd(ExpReader reader) {
        Tester tester = parseTester(reader);
        while (true) {
            switch (reader.next()) {
                case ' ' -> {
                }
                case '&' -> tester = tester.and(parseTester(reader));
                case '\0' -> {
                    return tester;
                }
                default -> {
                    reader.back();
                    return tester;
                }
            }
        }
    }


    private static Tester parseTester(ExpReader reader) {
        char c = reader.next();
        while (c == ' ') {
            c = reader.next();
        }
        if (c == '\0') {
            throw reader.expected("tag");
        }
        if (c == '!') {
            return parseTester(reader).not();
        }
        if (c == '(') {
            var value = parseOr(reader);
            if (reader.next() != ')') {
                throw reader.expected(')');
            }
            return value;
        }
        StringBuilder sb = new StringBuilder();
        while (c != '!' && c != '&' && c != '|' && c != '\0' && c != ')' && c != '(') {
            if (c != ' ')
                sb.append(c);
            c = reader.next();
        }
        reader.back();
        String word = sb.toString().toLowerCase(Locale.ROOT);
        if (word.contains("*") || word.contains("?")) return Tester.ofWildcard(word);
        return Tester.of(word);
    }

    public interface Tester {
        boolean test(String m);

        default boolean test(Collection<String> set) {
            for (String s : set) {
                if (test(s)) return true;
            }
            return false;
        }

        default Tester and(Tester o) {
            return m -> test(m) && o.test(m);
        }

        default Tester or(Tester o) {
            return m -> test(m) || o.test(m);
        }

        default Tester not() {
            return m -> !test(m);
        }

        static Tester of(String w) {
            return m -> m.equals(w);
        }

        static Tester ofWildcard(String pattern) {
            return m -> WildcardMatcher.match(pattern, m);
        }
    }

}