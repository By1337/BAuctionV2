package com.by1337.auc.menu;

import com.by1337.auc.auc.LotData;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.AbstractMenu;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.bmenu.menu.command.MenuCommands;
import dev.by1337.bmenu.slot.SlotContent;
import dev.by1337.bmenu.slot.impl.SimpleSlotContent;
import dev.by1337.cmd.Command;
import dev.by1337.item.ItemComponents;
import dev.by1337.yaml.codec.PipelineYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ContainerViewMenu extends AbstractMenu {
    public static Command<ExecuteContext> COMMANDS;
    private final ContainerViewConfig cfg;
    private LotData lot;
    private int offset;

    public ContainerViewMenu(ContainerViewConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        cfg = config;
        if (previousMenu != null && previousMenu.lastClickedItemPayload() instanceof LotData lot) {
            this.lot = lot;
        }
        if (this.lot == null) return;
        addPlaceholderResolver(lot.placeholders());
        var model = this.lot.itemStack().itemModel();

        SlotContent[] display = layers.getMatrix(2);
        var container = model.get(ItemComponents.CONTAINER);
        if (container != null) {
            var items = container.items();
            offset = Math.clamp(items.size(), 27, 45);
            for (int i = 0; i < offset; i++) {
                var m = items.get(i);
                if (m != null) {
                    display[i] = new SimpleSlotContent(m);
                }
            }
        } else {
            var bundle = model.get(ItemComponents.BUNDLE_CONTENTS);
            if (bundle != null) {
                var contents = bundle.contents();
                offset = Math.clamp(upTo9(contents.size()), 9, 45);
                for (int i = 0; i < offset; i++) {
                    if (i >= contents.size()) continue;
                    var m = contents.get(i);
                    display[i] = new SimpleSlotContent(m);
                }
            }
        }
    }
    private int upTo9(int x) {
        return ((x / 9) * 9) + 9;
    }

    @Override
    protected int menuSize() {
        if (offset == 0) return 54;
        return offset + 9;
    }

    @Override
    protected int getItemOffsets() {
        return offset;
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
        //  var item = layers.getBaseLayer()[5+offset];
        //  if (item != null)
        //      item.setPayload(lot);
    }

    static void bootCommands(Command<ExecuteContext> base){
        COMMANDS = base;
    }
    @Override
    public Command<ExecuteContext> getCommands() {
        if (previousMenu != null){
            lastClickedItem = previousMenu.lastClickedItem();
            lastClickedSlot = previousMenu.lastClickedSlot();
        }
        return COMMANDS;
    }

    public static class ContainerViewConfig extends MenuConfig {
        public static final YamlCodec<ContainerViewConfig> CODEC = new PipelineYamlCodecBuilder<>(ContainerViewConfig::new)
                .and(MenuConfig.RAW_CODEC)
                .build();

        @Override
        public Menu create(Player viewer, @Nullable Menu previousMenu) {
            return new ContainerViewMenu(this, viewer, previousMenu);
        }
    }

}
