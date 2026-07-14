package com.by1337.auc.command;

import com.by1337.auc.command.args.SearchArgument;
import com.by1337.auc.menu.HomeMenu;
import com.by1337.auc.search.SearchManager;
import com.by1337.auc.search.filter.SearchFilter;
import com.by1337.auc.util.ByLocaleSelector;
import dev.by1337.bmenu.BMenu;
import dev.by1337.cmd.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SearchCommand extends Command<CommandSender> {

    public SearchCommand(String name, Map<String, SearchManager> search) {
        super(name);
        requires(s -> s instanceof Player);
        ByLocaleSelector<SearchManager> selector = new ByLocaleSelector<>("ru_RU");
        search.values().forEach(v -> selector.add(v.locale(), v));
        executor(
                new SearchArgument<>("filter", selector),
                this::run
        );
    }

    private void run(CommandSender sender, SearchFilter filter) {
        if (!(sender instanceof Player player)) return;
        if (filter == null) {
            BMenu.menuLoader().create("bauc:home", player, null).open();
            return;
        }
        var menu = BMenu.menuLoader().create("bauc:home", player, null);
        if (menu instanceof HomeMenu h) {
            h.setFilter(filter);
        }
        menu.open();
    }
}
