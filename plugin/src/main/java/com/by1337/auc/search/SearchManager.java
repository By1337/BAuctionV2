package com.by1337.auc.search;

import com.by1337.auc.assets.McLang;
import com.by1337.auc.handler.index.Tag2IdService;
import com.by1337.auc.search.filter.SearchFilter;
import com.by1337.auc.search.filter.SearchFilterAndNotPair;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import net.kyori.adventure.translation.Translatable;
import net.kyori.adventure.translation.Translator;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.Map;

public class SearchManager {
    public static final YamlDecoder<SearchManager> DECODER = Config.DECODER.map(SearchManager::new);
    private final Config config;
    private Trie<SearchFilter> trie;
    private Locale locale;

    public SearchManager(Config config) {
        this.config = config;

    }

    public void boot(Tag2IdService tag2id, String locale) {
        trie = new Trie<>();
        this.locale = Translator.parseLocale(locale);
        McLang lang = new McLang(locale);
        for (Map.Entry<String, String[]> e : config.search.entrySet()) {
            addLookup(tag2id, e.getKey(), e.getValue());
        }
        addRegistry(Registry.MATERIAL, tag2id, lang);
        addRegistry(Registry.POTION_EFFECT_TYPE, tag2id, lang);
        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            addLookup(tag2id, lang.getTranslation(enchantment.description()), enchantment.getKey().value());
        }
    }

    private <T extends Translatable & Keyed> void addRegistry(Registry<@NonNull T> r, Tag2IdService tag2id, McLang lang){
        for (T t : r) {
            addLookup(tag2id, lang.getTranslation(t.translationKey()), t.getKey().value());
        }
    }

    public Trie<SearchFilter> trie() {
        return trie;
    }

    public Locale locale() {
        return locale;
    }

    public void addLookup(Tag2IdService tag2id, String key, String... tags) {
        key = key.toLowerCase(Locale.ROOT);
        IntArraySet andSet = new IntArraySet();
        IntArraySet notSet = new IntArraySet();
        for (String tag : tags) {
            if (tag.startsWith("!")) {
                notSet.add(tag2id.getId(tag.substring(1)));
            } else {
                andSet.add(tag2id.getId(tag));
            }
        }
        int[] and = andSet.isEmpty() ? null : andSet.toIntArray();
        int[] not = notSet.isEmpty() ? null : notSet.toIntArray();

        SearchFilter filter = new SearchFilterAndNotPair(and, not, tags);
        trie.insert(key, filter);
        for (Map.Entry<String, String> e : config.aliases.entrySet()) {
            var s = key.replace(e.getKey(), e.getValue());
            if (!s.endsWith(key)) {
                trie.insert(s, filter);
            }
        }
    }


    public static class Config {
        public static final YamlDecoder<Config> DECODER = RecordYamlDecoder.mapOf(
                Config::new,
                YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.STRING)
                        .fieldOf("aliases"),
                YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.STRING.map(s -> s.split(",")))
                        .fieldOf("search")
        );
        private final Map<String, String> aliases;
        private final Map<String, String[]> search;

        public Config(Map<String, String> aliases, Map<String, String[]> search) {
            this.aliases = aliases;
            this.search = search;
        }

        public Map<String, String> aliases() {
            return aliases;
        }

        public Map<String, String[]> search() {
            return search;
        }
    }
}
