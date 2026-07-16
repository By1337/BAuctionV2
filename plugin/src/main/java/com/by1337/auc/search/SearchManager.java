package com.by1337.auc.search;

import com.by1337.auc.assets.McLang;
import com.by1337.auc.search.filter.SearchFilter;
import com.by1337.auc.search.filter.SearchFilterParser;
import dev.by1337.yaml.decoder.RecordYamlDecoder;
import dev.by1337.yaml.decoder.YamlDecoder;
import net.kyori.adventure.translation.Translatable;
import net.kyori.adventure.translation.Translator;
import org.bukkit.Keyed;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SearchManager {
    public static final YamlDecoder<SearchManager> DECODER = SearchConfig.DECODER.map(SearchManager::new);
    private final SearchConfig config;
    private Trie<SearchFilter> trie;
    private Locale locale;

    public SearchManager(SearchConfig config) {
        this.config = config;
    }

    public void boot(String locale) {
        trie = new Trie<>();
        this.locale = Translator.parseLocale(locale);
        McLang lang = new McLang(locale);
        for (Map.Entry<String, SearchFilter> e : config.search.entrySet()) {
            addLookup(e.getKey(), e.getValue());
        }
        addRegistry(Registry.MATERIAL, lang);
        addRegistry(Registry.POTION_EFFECT_TYPE, lang);
        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            addLookup(lang.getTranslation(enchantment.description()), SearchFilter.ofTag(enchantment.getKey().value()));
        }
    }

    private <T extends Translatable & Keyed> void addRegistry(Registry<@NonNull T> r, McLang lang){
        for (T t : r) {
            addLookup(lang.getTranslation(t.translationKey()), SearchFilter.ofTag(t.getKey().value()));
        }
    }

    public Trie<SearchFilter> trie() {
        return trie;
    }

    public Locale locale() {
        return locale;
    }

    public void addLookup(String key, SearchFilter filter) {
        key = key.toLowerCase(Locale.ROOT);
        var set = applyAliases(key);
        for (String s : set) {
            trie.insert(s, filter);
        }
    }

    private Set<String> applyAliases(String src){
        Set<String> set = new HashSet<>();
        set.add(src);
        for (var e : config.aliases.entrySet()) {
            var s = src.replace(e.getKey(), e.getValue());
            set.add(s);
            for (var e1 : config.aliases.entrySet()) {
                set.add(s.replace(e1.getKey(), e1.getValue()));
            }
        }
        return set;
    }


    public static class SearchConfig {
        public static final YamlDecoder<SearchConfig> DECODER = RecordYamlDecoder.mapOf(
                SearchConfig::new,
                YamlDecoder.mapOf(YamlDecoder.STRING, YamlDecoder.STRING)
                        .fieldOf("aliases"),
                YamlDecoder.mapOf(YamlDecoder.STRING, SearchFilterParser.DECODER)
                        .fieldOf("search")
        );
        private final Map<String, String> aliases;
        private final Map<String, SearchFilter> search;

        public SearchConfig(Map<String, String> aliases, Map<String, SearchFilter> search) {
            this.aliases = aliases;
            this.search = search;
        }

        public Map<String, String> aliases() {
            return aliases;
        }

        public Map<String, SearchFilter> search() {
            return search;
        }
    }
}
