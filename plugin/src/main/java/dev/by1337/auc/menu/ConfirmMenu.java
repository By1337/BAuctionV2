package dev.by1337.auc.menu;

import dev.by1337.auc.auc.LotData;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.AbstractMenu;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.bmenu.menu.command.MenuCommands;
import dev.by1337.bmenu.slot.SlotFactory;
import dev.by1337.cmd.Command;
import dev.by1337.yaml.codec.PipelineYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ConfirmMenu extends AbstractMenu {
    public static Command<ExecuteContext> COMMANDS;
    private final ConfirmMenuConfig cfg;

    public ConfirmMenu(ConfirmMenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        cfg = config;
    }

    @Override
    protected void generate() {
        if (previousMenu != null && previousMenu.lastClickedItemPayload() instanceof LotData lot) {
            for (int slot : cfg.show_item.slots()) {
                setItem(cfg.show_item.build(lot.itemStack().itemModel(lot.count()), lot.placeholders()), slot);
            }
        }
    }

    @Override
    public Command<ExecuteContext> getCommands() {
        return COMMANDS;
    }
    static void bootCommands(Command<ExecuteContext> base){
        COMMANDS = MenuCommands.getCommands()
                .and(base)
                .sub(new Command<ExecuteContext>("[accept]").executor(ctx -> {
                    if (ctx.menu instanceof ConfirmMenu c) {
                        var cmd = c.args.getOrDefault("on_accept_command", "[console] say bauc:confirm no command!");
                        if (c.previousMenu != null) {
                            c.lastClickedItem = c.previousMenu.lastClickedItem();
                            ctx.menu = c.previousMenu;
                            c.previousMenu.reopen();
                        }
                        c.executeCommand(ctx, c.setPlaceholders(cmd));
                    }
                }));
    }

    public static class ConfirmMenuConfig extends MenuConfig {
        public static final YamlCodec<ConfirmMenuConfig> CODEC = new PipelineYamlCodecBuilder<>(ConfirmMenuConfig::new)
                .and(MenuConfig.RAW_CODEC)
                .field(SlotFactory.CODEC, "show_item", v -> v.show_item, (m, v) -> m.show_item = v)
                .build();

        private SlotFactory show_item;

        @Override
        public Menu create(Player viewer, @Nullable Menu previousMenu) {
            return new ConfirmMenu(this, viewer, previousMenu);
        }
    }

}
