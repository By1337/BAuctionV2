package dev.by1337.auc.command.args;

import dev.by1337.auc.config.Config;
import dev.by1337.auc.search.SearchManager;
import dev.by1337.auc.search.filter.PriceLimiterSearchFilter;
import dev.by1337.auc.search.filter.SearchFilter;
import dev.by1337.auc.search.filter.SearchFilterAndList;
import dev.by1337.auc.util.ByLocaleSelector;
import dev.by1337.auc.util.number.EconomyUtil;
import dev.by1337.cmd.*;
import dev.by1337.core.util.misc.Pair;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Locale;

public class SearchArgument<C extends CommandSender> extends Argument<C, Pair<String, SearchFilter>> {
    private static final Logger log = LoggerFactory.getLogger(SearchArgument.class);
    private final ByLocaleSelector<SearchManager> search;
    private final ArgumentNumber<C> argumentNumber = new ArgumentNumber<>("$number");
    private final Config config;

    public SearchArgument(String name, ByLocaleSelector<SearchManager> search, Config config) {
        super(name);
        this.search = search;
        this.config = config;
    }

    @Override
    public void parse(C c, CommandReader reader, ArgumentMap out) throws CommandMsgError {
        if (!(c instanceof Player player)) return;
        SearchManager manager = search.select(player.locale());
        if (manager == null) {
            log.error("has no SearchManager for locale {}", player.locale());
            return;
        }
        String input = readAll(reader);
        String[] in = input.split(" ");
        long maxPrice = -1;
        if (config.ah_search_max_price_perm != null && player.hasPermission(config.ah_search_max_price_perm)) {
            if (in.length > 1 && Character.isDigit(in[in.length - 1].charAt(0))) {
                argumentNumber.parse(c, new CommandReader(in[in.length - 1]), out);
                Number number = (Number) out.get(argumentNumber.name());
                if (number != null) {
                    maxPrice = EconomyUtil.toCents(number.doubleValue());
                }
                input = input.substring(0, input.lastIndexOf(' '));
            }
        }

        var list = manager.trie().parse(input);
        if (list.isEmpty()) {
            return;
        }
        SearchFilter result;
        if (list.size() == 1) {
            result = list.getFirst();
        } else {
            result = new SearchFilterAndList(list);
        }
        if (maxPrice != -1) {
            //
            out.put(name, Pair.of(input, new PriceLimiterSearchFilter(result, maxPrice)));
        } else {
            out.put(name, Pair.of(input, result));
        }
    }

    @Override
    public void suggest(C c, CommandReader reader, SuggestionsList suggestions, ArgumentMap ignored) throws CommandMsgError {
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

        int lastSpace;
        if ((lastSpace = input.lastIndexOf(' ')) != -1) {
            suggestions.setStart(suggestions.start() + lastSpace + 1);
        }
        String[] in = input.split(" ");
        if (config.ah_search_max_price_perm != null && player.hasPermission(config.ah_search_max_price_perm)) {
            if (in.length > 1 && Character.isDigit(in[in.length - 1].charAt(0))) {
                suggestions.suggest(in[in.length - 1] + "0");
                argumentNumber.suggest(c, new CommandReader(in[in.length - 1]), suggestions, ignored);
                return;
            }
        }

        var s = manager.trie().getSuggestions(input, 100);
        s.sort(Comparator.comparingInt(String::length));
        for (String suggestion : s) {
            int idx = suggestion.lastIndexOf(' ', input.length() - 1);

            String completion = idx == -1
                    ? suggestion
                    : suggestion.substring(idx + 1);
            suggestions.suggest(completion);
            if (!suggestions.hasFree()) break;
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
