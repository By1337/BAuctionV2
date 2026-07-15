package com.by1337.auc.menu;

import com.by1337.auc.BAuction;
import com.by1337.auc.auc.ClientVaultLot;
import com.by1337.auc.auc.LotData;
import com.by1337.auc.handler.Auction;
import com.by1337.auc.search.LotsResult;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.factory.MenuCodecs;
import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.AbstractMenu;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.bmenu.menu.command.MenuCommands;
import dev.by1337.bmenu.slot.SlotContent;
import dev.by1337.bmenu.slot.SlotFactory;
import dev.by1337.cmd.Command;
import dev.by1337.item.ItemModel;
import dev.by1337.plc.PlaceholderResolver;
import dev.by1337.plc.Placeholders;
import dev.by1337.yaml.codec.PipelineYamlCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterators;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class LotsMenu extends AbstractMenu {
    private static final PlaceholderResolver<LotsMenu> PLACEHOLDERS = Placeholders.<LotsMenu>create()
            .withContext("current_page", v -> v.currentPage + 1)
            .withContext("max_page", v -> v.maxPage + 1)
            .withContext("has_next_page", v -> v.hasNextPage);
    public static final Command<ExecuteContext> COMMANDS = MenuCommands.getCommands()
            .and(Commands.commands())
            .sub(new Command<ExecuteContext>("[research]").executor(ctx -> {
                if (ctx.menu instanceof LotsMenu h) {
                    h.research();
                    h.refresh();
                }
            })).sub(new Command<ExecuteContext>("[next_page]").executor(ctx -> {
                if (ctx.menu instanceof LotsMenu h) {
                    if (!h.hasNextPage) return;
                    h.currentPage++;
                    h.refresh();
                }
            })).sub(new Command<ExecuteContext>("[previous_page]").executor(ctx -> {
                if (ctx.menu instanceof LotsMenu h) {
                    if (h.currentPage == 0) return;
                    h.currentPage--;
                    h.refresh();
                }
            }));
    protected int currentPage = 0;
    protected int maxPage = 0;
    private final LotsMenuConfig cfg;

    private final List<LotData> lots = new ArrayList<>(45);
    private LotsResult searchResult;
    protected final Auction auction;
    private final LotData[] viewingLots;
    private boolean hasNextPage;
    private final Int2ObjectOpenHashMap<SlotContent> lotRewriter = new Int2ObjectOpenHashMap<>();

    public LotsMenu(LotsMenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        cfg = config;
        viewingLots = new LotData[animationMask().length];
        auction = BAuction.auction();
        addPlaceholderResolver(PLACEHOLDERS.bindCtx(this));
    }

    @Override
    public void open(boolean isReopen) {
        if (auction == null) {
            BAuction.sendMessage("auction_is_disabled", viewer);
            return;
        }
        super.open(isReopen);
    }

    @Override
    public void refresh() {
        super.refresh();
    }

    @Override
    public void tick() {
        for (int i = 0; i < viewingLots.length; i++) {
            var old = viewingLots[i];
            if (old != null && !lotRewriter.containsKey(old.uid())) {
                var actual = getByUid(old.uid(), old);
                if (actual != old) {
                    setLot(i, actual != null ? actual : old, actual == null);
                }
            }
        }
        super.tick();
    }

    public void research() {
        lots.clear();
        if (searchResult != null) searchResult.release();
        searchResult = search();
        refresh();
    }

    protected abstract LotsResult search();

    protected abstract LotData getByUid(int uid, LotData old);

    @Override
    public Command<ExecuteContext> getCommands() {
        return COMMANDS;
    }

    @Override
    protected void generate() {
        if (searchResult == null) {
            research();
        }
        Arrays.fill(layers.getMatrix(3), null);

        int[] slots = cfg.slots;

        int pageCount = (searchResult.size() + slots.length - 1) / slots.length;
        maxPage = Math.max(0, pageCount - 1);

        currentPage = Math.min(currentPage, maxPage);

        Arrays.fill(viewingLots, null);
        var slotsIterator = IntIterators.wrap(slots);
        loop:
        for (int x = currentPage * slots.length; x < searchResult.size(); x++) {
            while (lots.size() - 1 < x) {
                var next = searchResult.next();
                if (next == null) break loop;
                lots.add(next);
            }
            LotData lot0 = lots.get(x);
            if (slotsIterator.hasNext()) {
                int slot = slotsIterator.nextInt();
                var rewrite = lotRewriter.get(lot0.uid());
                if (rewrite != null) {
                    setItem(rewrite, slot);
                } else {
                    var actual = getByUid(lot0.uid(), lot0);
                    LotData lot = actual == null ? lot0 : actual;
                    setLot(slot, lot, actual == null);
                    viewingLots[slot] = actual;
                }
            } else {
                break loop;
            }
        }
        var next = searchResult.next();
        if (next != null) lots.add(next);
        hasNextPage = lots.size() > (currentPage + 1) * slots.length;
    }

    private void setLot(int slot, LotData lot, boolean outdated) {
        Material itemType = lot.itemStack().material();
        SlotFactory maker;
        ItemModel base = null;
        if (lot.getClass() == ClientVaultLot.class) {
            maker = cfg.vault_lot;
            base = lot.itemStack().itemModel().withAmount(lot.count());
        } else if (outdated) {
            maker = cfg.sold;
        } else if (lot.owner().equals(viewer.getUniqueId())) {
            maker = cfg.owned;
            base = lot.itemStack().itemModel().withAmount(lot.count());
        } else if (Tag.SHULKER_BOXES.isTagged(itemType) || Tag.ITEMS_BUNDLES.isTagged(itemType)) {
            maker = cfg.container;
            base = lot.itemStack().itemModel();
        } else if (lot.count() == 1) {
            maker = cfg.one;
            base = lot.itemStack().itemModel();
        } else {
            maker = cfg.many;
            base = lot.itemStack().itemModel().withAmount(lot.count());
        }
        var slotData = maker.build(base, lot.placeholders());
        slotData.setPayload(lot);
        setItem(slotData, slot);
    }

    public void rewriteLotDisplay(LotData lot, SlotContent content) {
        lotRewriter.put(lot.uid(), content);
    }

    public LotsMenuConfig cfg() {
        return cfg;
    }

    public static class LotsMenuConfig extends MenuConfig {
        public static final PipelineYamlCodecBuilder<LotsMenuConfig> RAW_CODEC = new PipelineYamlCodecBuilder<>(LotsMenuConfig::new)
                .and(MenuConfig.RAW_CODEC)
                .field(MenuCodecs.SLOTS_CODEC, "slots", v -> v.slots, (m, v) -> m.slots = v)
                .field(SlotFactory.CODEC, "sold", v -> v.sold, (m, v) -> m.sold = v)
                .field(SlotFactory.CODEC, "owned", v -> v.owned, (m, v) -> m.owned = v)
                .field(SlotFactory.CODEC, "container", v -> v.container, (m, v) -> m.container = v)
                .field(SlotFactory.CODEC, "one", v -> v.one, (m, v) -> m.one = v)
                .field(SlotFactory.CODEC, "many", v -> v.many, (m, v) -> m.many = v)
                .field(SlotFactory.CODEC, "taken", v -> v.taken, (m, v) -> m.taken = v)
                .field(SlotFactory.CODEC, "purchased", v -> v.purchased, (m, v) -> m.purchased = v)
                .field(SlotFactory.CODEC, "vault_lot", v -> v.vault_lot, (m, v) -> m.vault_lot = v);

        public int[] slots;
        public SlotFactory sold;
        public SlotFactory owned;
        public SlotFactory container;
        public SlotFactory one;
        public SlotFactory many;
        public SlotFactory taken;
        public SlotFactory purchased;
        public SlotFactory vault_lot;

    }
}
