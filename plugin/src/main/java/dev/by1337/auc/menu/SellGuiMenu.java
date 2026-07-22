package dev.by1337.auc.menu;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.transaction.AddLotTransaction;
import dev.by1337.auc.util.mc.InvUtil;
import dev.by1337.auc.util.number.NumberFormatter;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.bmenu.slot.SlotContent;
import dev.by1337.bmenu.slot.SlotFactory;
import dev.by1337.cmd.Command;
import dev.by1337.edsl.context.EventContext;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;
import dev.by1337.yaml.codec.PipelineYamlCodecBuilder;
import dev.by1337.yaml.codec.YamlCodec;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class SellGuiMenu extends PutableMenu {
    private static final PlaceholderResolver<SellGuiMenu> PLACEHOLDERS = Placeholders.<SellGuiMenu>create()
            .withContext("price", v -> NumberFormatter.format(v.price));
    private static Command<ExecuteContext> COMMANDS;
    private final int menuSize;
    private double price;
    private final int zone;
    private final int putable;
    private final SellGuiMenuConfig cfg;

    public SellGuiMenu(SellGuiMenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        cfg = config;
        addPlaceholderResolver(PLACEHOLDERS.bindCtx(this));

        int slots = BAuction.plugin().config().slots.collectSlots(viewer) - BAuction.auction().getPlayerOwnedLotsCount(viewer.getUniqueId());
        zone = Math.clamp(upTo9(slots), 9, 45);
        menuSize = zone + 9;
        putable = Math.min(slots, zone);
        for (int i = 0; i < putable; i++) {
            addPutableSlot(i);
        }
    }

    @Override
    protected int getItemOffsets() {
        return menuSize - 9;
    }

    @Override
    protected int menuSize() {
        if (menuSize == 0) return 54;
        return menuSize;
    }

    @Override
    public void open() {
        super.open();
    }

    private int upTo9(int x) {
        return ((x / 9) * 9) + 9;
    }

    @Override
    protected void generate() {
        for (int i = putable; i < zone; i++) {
            setItem(cfg.blockedSlot(), i);
        }
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        InvUtil.giveOrDrop(viewer, collectItems().toArray(new ItemStack[0]));
        clearPutableSlots();
        super.onClose(event);
    }

    @Override
    public Command<ExecuteContext> getCommands() {
        return COMMANDS;
    }

    static void bootCommands(Command<ExecuteContext> base) {
        COMMANDS = base.sub(new Command<ExecuteContext>("[do_sell]").executor(c -> {
            if (c.menu instanceof SellGuiMenu sell) {
                var auc = BAuction.auction();
                if (auc == null) {
                    sell.close();
                    return;
                }
                var items = sell.collectItems();
                sell.clearPutableSlots();
                sell.close();
                AtomicInteger counter = new AtomicInteger();
                auc.parallel(
                        items.iterator(),
                        () -> {
                            int count = counter.get();
                            if (count == 0) return;
                            BAuction.sendMessage("sell_gui", sell.viewer, PlaceholderResolver.<EventContext>of("count", count)
                                    .append("price", NumberFormatter.format(sell.price)));
                        },
                        item -> auc.apply(new AddLotTransaction(item, sell.viewer.getUniqueId(), sell.price, item.getAmount())),
                        (item, result) -> {
                            if (result == null) {
                                InvUtil.giveOrDrop(sell.viewer, item);
                            } else {
                                counter.incrementAndGet();
                            }
                        }
                );
            }
        }));
    }

    public static class SellGuiMenuConfig extends MenuConfig {
        public static final YamlCodec<SellGuiMenuConfig> CODEC = new PipelineYamlCodecBuilder<>(SellGuiMenuConfig::new)
                .and(MenuConfig.RAW_CODEC)
                .field(SlotFactory.CODEC, "blocked", v -> v.blocked, (m, v) -> m.blocked = v)
                .build();
        private SlotFactory blocked;
        private SlotContent blockedSlot;

        public SlotContent blockedSlot() {
            if (blockedSlot == null) return blockedSlot = blocked.build();
            return blockedSlot;
        }

        @Override
        public Menu create(Player viewer, @Nullable Menu previousMenu) {
            return new SellGuiMenu(this, viewer, previousMenu);
        }
    }
}
