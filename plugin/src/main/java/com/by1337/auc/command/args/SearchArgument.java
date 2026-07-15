package com.by1337.auc.command.args;

import com.by1337.auc.search.SearchManager;
import com.by1337.auc.search.filter.SearchFilter;
import com.by1337.auc.search.filter.SearchFilterList;
import com.by1337.auc.util.ByLocaleSelector;
import dev.by1337.cmd.*;
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
        var list = manager.trie().parse(str);
        if (list.isEmpty()) {
            return;
        }
/*        for (SearchFilter f : list) {
            if (f instanceof SearchFilterAndNotPair p){
                for (String s : p.from()) {
                    System.out.println(s + " " + BAuction.plugin().aucManager().tag2id().getId(s));
                }
            }
        }*/
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
        String input = readAll(reader);
        if (input.isEmpty()) {
            for (String v : manager.trie().getSuggestions("", 30)) {
                suggestions.suggest(v);
            }
            return;
        }
        var s = manager.trie().getSuggestions(input, 30);
        int lastSpace;
        if ((lastSpace = input.lastIndexOf(' ')) != -1) {
            suggestions.setStart(suggestions.start() + lastSpace + 1);
        }
        for (String suggestion : s) {
            int idx = suggestion.lastIndexOf(' ', input.length() - 1);

            String completion = idx == -1
                    ? suggestion
                    : suggestion.substring(idx + 1);
            suggestions.suggest(completion);
        }
    }

    private String readAll(CommandReader reader) {
        String src = reader.src();
        int idx = reader.ridx();
        if (idx < src.length()) {
            reader.ridx(src.length());
            return src.substring(idx).toLowerCase(Locale.ROOT);
        }
        return "";
    }
}
