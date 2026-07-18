package com.by1337.auc.menu;

import com.by1337.auc.auc.LotData;
import com.by1337.auc.auc.category.Category;
import com.by1337.auc.auc.sort.Sorting;
import com.by1337.auc.search.LotsResult;
import com.by1337.auc.search.filter.SearchFilter;
import com.by1337.auc.util.CyclicListIterator;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.cmd.Command;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;
import dev.by1337.yaml.codec.PipelineYamlCodecBuilder;
import dev.by1337.yaml.codec.RecordYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HomeMenu extends LotsMenu {
    private static final PlaceholderResolver<HomeMenu> PLACEHOLDERS = Placeholders.<HomeMenu>create()
            .withContext("current_category_id", v -> v.category.id())
            .withContext("search_input", v -> v.category.id().equals("search") ? v.searchInput : "")
            .withContext("all_categories", v -> {
                StringBuilder sb = new StringBuilder();
                var cfg = v.cfg.categoryLine;
                if (cfg == null) return "has no category_line in config!";
                var selected = v.category;
                for (Category category : v.categoryIterator.list()) {
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
            })
            .withContext("all_sorting", v -> {
                StringBuilder sb = new StringBuilder();
                var cfg = v.cfg.sortingLine;
                if (cfg == null) return "has no sorting_line in config!";
                var selected = v.sorting;
                for (var sort : v.sortingIterator.list()) {
                    if (sort == selected) {
                        sb.append(cfg.if_selected.replace("{id}", sort.id()));
                    } else {
                        sb.append(cfg.if_not_selected.replace("{id}", sort.id()));
                    }
                    sb.append("\n");
                }
                if (sb.isEmpty()) return "";
                sb.setLength(sb.length() - 1);
                return sb.toString();
            });
    private static Command<ExecuteContext> COMMANDS;

    private CyclicListIterator<Sorting> sortingIterator;
    private Sorting sorting;
    private CyclicListIterator<Category> categoryIterator;
    private Category category;
    private final HomeMenuV2Config cfg;
    private String searchInput = "";

    public HomeMenu(HomeMenuV2Config config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        cfg = config;
        categoryIterator = auction.registries().category.cycle();
        category = categoryIterator.current();

        sortingIterator = auction.registries().sorting.cycle();
        sorting = sortingIterator.current();
        addPlaceholderResolver(PLACEHOLDERS.bindCtx(this));
    }

    @Override
    protected LotsResult search() {
        //  SearchFilter f = search != null ? search : category.filter();
        long nanos = System.nanoTime();
        var v = auction.search(category.filter(), sorting);
        System.out.println((System.nanoTime() - nanos) / 1000D + "us " + category.id());
        return v;
    }

    @Override
    protected LotData getByUid(int uid, LotData old) {
        return auction.getLot(uid);
    }

    public void setSearch(@Nullable SearchFilter search) {
        if (search == null) {
            categoryIterator = auction.registries().category.cycle();
            category = categoryIterator.current();
        } else {
            category = new Category("search", search);
            List<Category> categories = new ArrayList<>();
            categories.add(category);
            categories.addAll(auction.registries().category.values());
            categoryIterator = new CyclicListIterator<>(categories);
        }
    }

    public String searchInput() {
        return searchInput;
    }

    public void setSearchInput(String searchInput) {
        this.searchInput = searchInput;
    }

    static void bootCommands(Command<ExecuteContext> base) {
        COMMANDS = base
                .and(LotsMenu.LOTS_COMMANDS)
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
    }

    @Override
    public Command<ExecuteContext> getCommands() {
        return COMMANDS;
    }


    public static class HomeMenuV2Config extends LotsMenuConfig {
        public static final YamlCodec<HomeMenuV2Config> CODEC = new PipelineYamlCodecBuilder<>(HomeMenuV2Config::new)
                .and(LotsMenuConfig.RAW_CODEC)
                .field(DataLine.CODEC, "category_line", v -> v.categoryLine, (m, v) -> m.categoryLine = v)
                .field(DataLine.CODEC, "sorting_line", v -> v.sortingLine, (m, v) -> m.sortingLine = v)
                .build();
        public DataLine categoryLine;
        public DataLine sortingLine;

        @Override
        public Menu create(Player viewer, @Nullable Menu previousMenu) {
            return new HomeMenu(this, viewer, previousMenu);
        }

        public record DataLine(String if_selected, String if_not_selected) {
            public static final YamlCodec<DataLine> CODEC = RecordYamlCodecBuilder.mapOf(
                    DataLine::new,
                    YamlCodec.STRING.fieldOf("if_selected", v -> v.if_selected),
                    YamlCodec.STRING.fieldOf("else", v -> v.if_not_selected)
            );
        }
    }
}
