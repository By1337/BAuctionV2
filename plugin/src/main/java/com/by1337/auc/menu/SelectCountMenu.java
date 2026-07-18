package com.by1337.auc.menu;

import com.by1337.auc.auc.LotData;
import com.by1337.auc.command.args.NumberArgument;
import com.by1337.auc.util.number.NumberFormatter;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.AbstractMenu;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.bmenu.menu.command.MenuCommands;
import dev.by1337.bmenu.slot.SlotFactory;
import dev.by1337.cmd.Command;
import dev.by1337.cmd.CommandMsgError;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;
import dev.by1337.yaml.codec.PipelineYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class SelectCountMenu extends AbstractMenu {
    private static final PlaceholderResolver<SelectCountMenu> PLACEHOLDERS = Placeholders.<SelectCountMenu>create()
            .withContext("result_count", v -> v.count)
            .withContext("result_price", v -> NumberFormatter.format(v.lot.dprice_for_one() * v.count));
    public static Command<ExecuteContext> COMMANDS;
    private final SelectCountConfig cfg;
    private int count = 1;
    private LotData lot;

    public SelectCountMenu(SelectCountConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        cfg = config;
        if (previousMenu != null && previousMenu.lastClickedItemPayload() instanceof LotData lot) {
            this.lot = lot;
        }
        addPlaceholderResolver(PLACEHOLDERS.bindCtx(this));
    }

    @Override
    public void open() {
        if (lot == null) {
            throw new RuntimeException("has no lot");
        }
        super.open();
    }

    @Override
    protected void generate() {
        for (int slot : cfg.show_item.slots()) {
            setItem(cfg.show_item.build(lot.itemStack().itemModel(count), lot.placeholders()), slot);
        }
    }

    static void bootCommands(Command<ExecuteContext> base){
        COMMANDS = base
                .and(Commands.create())
                .sub(new Command<ExecuteContext>("[accept]").executor(ctx -> {
                    if (ctx.menu instanceof SelectCountMenu c) {
                        var cmd = c.args.getOrDefault("on_accept_command", "[console] say bauc:select_count no command!");
                        if (c.previousMenu != null) {
                            c.lastClickedItem = c.previousMenu.lastClickedItem();
                            ctx.menu = c.previousMenu;
                            c.previousMenu.reopen();
                        }
                        c.executeCommand(ctx, c.setPlaceholders(cmd));
                    }
                }))
                .sub(new Command<ExecuteContext>("[add]").executor(
                        new NumberArgument<>("count"),
                        (ctx, count0) -> {
                            if (count0 == null) throw new CommandMsgError("use [add] <count>");
                            if (ctx.menu instanceof SelectCountMenu c) {
                                c.count += count0.intValue();
                                c.count = Math.clamp(c.count, 1, c.lot.count());
                                c.refresh();
                            }
                        })
                )
                .sub(new Command<ExecuteContext>("[sub]").executor(
                        new NumberArgument<>("count"),
                        (ctx, count0) -> {
                            if (count0 == null) throw new CommandMsgError("use [add] <count>");
                            if (ctx.menu instanceof SelectCountMenu c) {
                                c.count -= count0.intValue();
                                c.count = Math.clamp(c.count, 1, c.lot.count());
                                c.refresh();
                            }
                        })
                );
    }

    @Override
    public Command<ExecuteContext> getCommands() {
        return COMMANDS;
    }

    public static class SelectCountConfig extends MenuConfig {
        public static final YamlCodec<SelectCountConfig> CODEC = new PipelineYamlCodecBuilder<>(SelectCountConfig::new)
                .and(MenuConfig.RAW_CODEC)
                .field(SlotFactory.CODEC, "show_item", v -> v.show_item, (m, v) -> m.show_item = v)
                .build();

        private SlotFactory show_item;

        @Override
        public Menu create(Player viewer, @Nullable Menu previousMenu) {
            return new SelectCountMenu(this, viewer, previousMenu);
        }
    }

}
