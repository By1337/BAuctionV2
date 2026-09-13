package dev.by1337.auc.search.filter;

import dev.by1337.auc.handler.index.Tag2IdService;
import dev.by1337.auc.tag.tester.ExpReader;
import dev.by1337.auc.util.WildcardMatcher;
import dev.by1337.yaml.codec.DataResult;
import dev.by1337.yaml.decoder.YamlDecoder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.bukkit.Material;
import org.bukkit.Registry;

import java.util.*;

public class SearchFilterParser {

    public static final YamlDecoder<SearchFilter> DECODER = YamlDecoder.STRINGS.flatMap((ctx, l) -> {
        try {
            return DataResult.success(parse(String.join("|", l)));
        } catch (Exception e) {
            return DataResult.error(e.getMessage());
        }
    });


    public static SearchFilter parse(String input) {
        if (input.isBlank()) return EmptySearchFilter.INSTANCE;
        ExpReader reader = new ExpReader(input);

        Set<Material> materials = new HashSet<>();

        List<IntArrayList> ands = new ArrayList<>();
        List<IntArrayList> nots = new ArrayList<>();
        IntArrayList and = new IntArrayList();
        IntArrayList not = new IntArrayList();
        loop:
        while (reader.hasNext()) {
            for (String token : parseToken(reader)) {
                switch (token) {
                    case "!" -> {
                        for (String word : parseToken(reader)) {
                            not.add(Tag2IdService.INSTANCE.getId(word));
                        }
                    }
                    case "|" -> {
                        ands.add(and);
                        nots.add(not);
                        and = new IntArrayList();
                        not = new IntArrayList();
                    }
                    case "&" -> {
                        //skip
                    }
                    case "\0" -> {
                        break loop;
                    }
                    default -> {
                        int id = Tag2IdService.INSTANCE.getId(token);
                        and.add(id);
                        var mat = Tag2IdService.INSTANCE.getMaterial(id);
                        if (mat != null) {
                            materials.add(mat);
                        }
                    }
                }
            }
        }
        if (!and.isEmpty())
            ands.add(and);
        if (!not.isEmpty())
            nots.add(not);
        SearchFilter result;
        int size = Math.max(ands.size(), nots.size());
        if (size == 0) return EmptySearchFilter.INSTANCE;
        if (size == 1) {
            result = new SearchFilterAndNotPair(
                    !and.isEmpty() ? and.toIntArray() : null,
                    !not.isEmpty() ? not.toIntArray() : null,
                    new String[]{input}
            );
        } else {
            int[][] resAnds = new int[size][];
            int[][] resNots = new int[size][];
            for (int i = 0; i < size; i++) {
                IntArrayList a = ands.size() > i ? ands.get(i) : null;
                IntArrayList n = nots.size() > i ? nots.get(i) : null;
                if (a != null && !a.isEmpty()) resAnds[i] = a.toIntArray();
                if (n != null && n.isEmpty()) resNots[i] = n.toIntArray();
            }
            result = new ComplexSearchFilter(resAnds, resNots);
        }
        if (materials.isEmpty()) return result;
        if (materials.size() > 10) return result;
        Material[] arr = materials.toArray(new Material[0]);
        if (arr.length == 1) {
            return new MaterialSearchFilter(arr[0]).and(result);
        }
        return new MaterialsSearchFilter(arr).and(result);
    }

    private static List<String> parseToken(ExpReader reader) {
        char c = reader.next();
        while (c == ' ') {
            c = reader.next();
        }
        if (c == '\0') {
            return List.of("\0");
            //throw reader.expected("tag");
        }
        if (c == '!') {
            return List.of("!");
        }
        if (c == '&') {
            return List.of("&");
        }
        if (c == '|') {
            return List.of("|");
        }
        StringBuilder sb = new StringBuilder();
        while (c != '!' && c != '&' && c != '|' && c != '\0' && c != ')' && c != '(') {
            if (c != ' ')
                sb.append(c);
            c = reader.next();
        }
        reader.back();
        String word = sb.toString().toLowerCase(Locale.ROOT);
        if (word.contains("*") || word.contains("?")) {
            List<String> result = new ArrayList<>();
            for (Material material : Registry.MATERIAL) {
                var key = material.getKey().value();
                if (WildcardMatcher.match(word, key)) {
                    result.add(key);
                    result.add("|");
                }
            }
            if (!result.isEmpty())
                return result.subList(0, result.size() - 1);
        }
        return List.of(word);
    }
}
