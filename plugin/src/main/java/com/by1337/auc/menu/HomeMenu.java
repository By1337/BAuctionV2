package com.by1337.auc.menu;

import com.by1337.auc.auc.LotData;
import com.by1337.auc.auc.sort.Sorting;
import com.by1337.auc.auc.sort.SortingRegistry;
import com.by1337.auc.search.filter.SearchFilter;
import com.by1337.auc.search.filter.SearchFilterAndNotPair;
import com.by1337.auc.handler.index.search.LotsResult;
import com.by1337.auc.util.CyclicListIterator;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.bmenu.menu.command.MenuCommands;
import dev.by1337.cmd.Command;
import dev.by1337.yaml.codec.PipelineYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class HomeMenu extends LotsMenu {
    private static final Command<ExecuteContext> COMMANDS = MenuCommands.getCommands()
            .and(LotsMenu.COMMANDS)
            .sub(new Command<ExecuteContext>("[next_sorting]").executor(ctx -> {
                if (ctx.menu instanceof HomeMenu h){
                    h.sorting = h.sortingIterator.next();
                    h.research();
                }
            })).sub(new Command<ExecuteContext>("[previous_sorting]").executor(ctx -> {
                if (ctx.menu instanceof HomeMenu h){
                    h.sorting = h.sortingIterator.previous();
                    h.research();
                }
            }));

    private final CyclicListIterator<Sorting> sortingIterator = SortingRegistry.cycle();
    private Sorting sorting;
    private @Nullable SearchFilter filter;


    public HomeMenu(LotsMenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
    }

    @Override
    protected LotsResult search() {
        if (sorting == null) {
            sorting = sortingIterator.current();
        }
        return auction.search(filter, sorting);
    }

    @Override
    protected LotData getByUid(int uid, LotData old) {
        return auction.getLot(uid);
    }

    public void setFilter(@Nullable SearchFilter filter) {
        this.filter = filter;
    }

    public void setSorting(Sorting sorting) {
        this.sorting = sorting;
        research();
    }

    @Override
    public Command<ExecuteContext> getCommands() {
        return COMMANDS;
    }


    public static class HomeMenuV2Config extends LotsMenuConfig {
        public static final YamlCodec<HomeMenuV2Config> CODEC = new PipelineYamlCodecBuilder<>(HomeMenuV2Config::new)
                .and(LotsMenuConfig.RAW_CODEC)
                .build();

        @Override
        public Menu create(Player viewer, @Nullable Menu previousMenu) {
            return new HomeMenu(this, viewer, previousMenu);
        }
    }
}
