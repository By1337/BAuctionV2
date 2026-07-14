package com.by1337.auc.command.args;

import com.by1337.auc.BAuction;
import com.by1337.auc.search.SearchManager;
import com.by1337.auc.search.filter.SearchFilter;
import com.by1337.auc.search.filter.SearchFilterAndNotPair;
import com.by1337.auc.search.filter.SearchFilterList;
import com.by1337.auc.util.ByLocaleSelector;
import dev.by1337.cmd.*;
import dev.by1337.cmd.argument.ArgumentStrings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class SearchArgument<C extends CommandSender> extends Argument<C, SearchFilter> {
    private static final Logger log = LoggerFactory.getLogger(SearchArgument.class);
    private final ByLocaleSelector<SearchManager> search;

    public SearchArgument(String name, ByLocaleSelector<SearchManager> search) {
        super(name);
        this.search = search;
    }

    @Override
    public void parse(C c, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        if (!(c instanceof Player player)) return;
        SearchManager manager = search.select(player.locale());
        if (manager == null) {
            log.error("has no SearchManager for locale {}", player.locale());
            return;
        }
        String str = readAll(reader);
        System.out.println(str);
        var list = manager.trie().parse(str);
        System.out.println(list);
        if (list.isEmpty()) {
            return;
        }
        for (SearchFilter f : list) {
            if (f instanceof SearchFilterAndNotPair p){
                for (String s : p.from()) {
                    System.out.println(s + " " + BAuction.plugin().aucManager().tag2id().getId(s));
                }
            }
        }
        if (list.size() == 1) {
            out.put(name, list.getFirst());
        } else {
            out.put(name, new SearchFilterList(list));
        }
    }

    @Override
    public void suggest(C c, CommandReader reader, SuggestionsList suggestions, ArgumentMap out) throws CommandMsgError {
        if (!(c instanceof Player player)) return;
        SearchManager manager = search.select(player.locale());
        if (manager == null) {
            log.error("has no SearchManager for locale {}", player.locale());
            return;
        }
        String str = readAll(reader);
        System.out.println(str);
        if (str.isEmpty()) {
            for (String v : manager.trie().getSuggestions("", 30)) {
                suggestions.suggest(v);
            }
            return;
        }
        for (String v : manager.trie().getSuggestions(str, 30)) {
            suggestions.suggest(v);
        }
    }
    private String readAll(CommandReader reader){
        String src = reader.src();
        int idx = reader.ridx();
        if (idx < src.length()) {
            reader.ridx(src.length());
            return src.substring(idx).toLowerCase(Locale.ROOT);
        }
        return "";
    }
}
