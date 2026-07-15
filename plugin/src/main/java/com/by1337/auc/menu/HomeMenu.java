package com.by1337.auc.menu;

import com.by1337.auc.BAuction;
import com.by1337.auc.auc.LotData;
import com.by1337.auc.auc.category.Category;
import com.by1337.auc.auc.sort.Sorting;
import com.by1337.auc.auc.sort.SortingRegistry;
import com.by1337.auc.search.LotsResult;
import com.by1337.auc.search.filter.SearchFilter;
import com.by1337.auc.util.CyclicListIterator;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.bmenu.menu.command.MenuCommands;
import dev.by1337.cmd.Command;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;
import dev.by1337.yaml.codec.PipelineYamlCodecBuilder;
import dev.by1337.yaml.codec.RecordYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class HomeMenu extends LotsMenu {
    private static final PlaceholderResolver<HomeMenu> PLACEHOLDERS = Placeholders.<HomeMenu>create()
            .withContext("current_category_id", v -> v.category.id())
            .withContext("all_categories", v -> {
                StringBuilder sb = new StringBuilder();
                var cfg = v.cfg.categoryLine;
                if (cfg == null) return "has no category_line in config!";
                var selected = v.category;
                for (Category category : BAuction.plugin().config().categories.list()) {
                    if (category == selected) {
                        sb.append(cfg.if_selected.replace("{id}", category.id()));
                    } else {
                        sb.append(cfg.if_not_selected.replace("{id}", category.id()));
                    }
                    sb.append("\n");
                }
                if (sb.isEmpty()) return "";
                sb.setLength(sb.length() - 1);
                return sb.toString();
            });
    private static final Command<ExecuteContext> COMMANDS = MenuCommands.getCommands()
            .and(LotsMenu.COMMANDS)
            .sub(new Command<ExecuteContext>("[next_sorting]").executor(ctx -> {
                if (ctx.menu instanceof HomeMenu h) {
                    h.sorting = h.sortingIterator.next();
                    h.research();
                }
            })).sub(new Command<ExecuteContext>("[previous_sorting]").executor(ctx -> {
                if (ctx.menu instanceof HomeMenu h) {
                    h.sorting = h.sortingIterator.previous();
                    h.research();
                }
            }))
            .sub(new Command<ExecuteContext>("[next_category]").executor(ctx -> {
                if (ctx.menu instanceof HomeMenu h) {
                    h.category = h.categoryIterator.next();
                    h.research();
                }
            })).sub(new Command<ExecuteContext>("[previous_category]").executor(ctx -> {
                if (ctx.menu instanceof HomeMenu h) {
                    h.category = h.categoryIterator.previous();
                    h.research();
                }
            }));

    private final CyclicListIterator<Sorting> sortingIterator = SortingRegistry.cycle();
    private Sorting sorting;
    private @Nullable SearchFilter filter;
    private final CyclicListIterator<Category> categoryIterator = BAuction.plugin().config().categories.cycle();
    private Category category = categoryIterator.current();
    private final HomeMenuV2Config cfg;

    public HomeMenu(HomeMenuV2Config config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        cfg = config;
        addPlaceholderResolver(PLACEHOLDERS.bindCtx(this));
    }

    @Override
    protected LotsResult search() {
        if (sorting == null) {
            sorting = sortingIterator.current();
        }
        SearchFilter f = filter != null ? filter : category.filter();
        return auction.search(f, sorting);
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
                .field(CategoryLine.CODEC, "category_line", v -> v.categoryLine, (m, v) -> m.categoryLine = v)
                .build();
        public CategoryLine categoryLine;

        @Override
        public Menu create(Player viewer, @Nullable Menu previousMenu) {
            return new HomeMenu(this, viewer, previousMenu);
        }

        public record CategoryLine(String if_selected, String if_not_selected) {
            public static final YamlCodec<CategoryLine> CODEC = RecordYamlCodecBuilder.mapOf(
                    CategoryLine::new,
                    YamlCodec.STRING.fieldOf("if_selected", v -> v.if_selected),
                    YamlCodec.STRING.fieldOf("else", v -> v.if_not_selected)
            );
        }
    }
}
