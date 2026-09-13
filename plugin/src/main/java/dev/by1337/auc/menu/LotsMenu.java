package dev.by1337.auc.menu;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.ClientVaultLot;
import dev.by1337.auc.auc.LotData;
import dev.by1337.auc.handler.Auction;
import dev.by1337.auc.search.LotsResult;
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
import it.unimi.dsi.fastutil.ints.IntIterators;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class LotsMenu extends AbstractMenu {
    private static final PlaceholderResolver<LotsMenu> PLACEHOLDERS = Placeholders.<LotsMenu>create()
            .of("example", (value, menu) -> value.equals("skull") ? "123" : 321)
            .withContext("current_page", v -> v.currentPage + 1)
            .withContext("max_page", v -> v.maxPage + 1)
            .withContext("has_next_page", v -> v.hasNextPage);
    public static final Command<ExecuteContext> LOTS_COMMANDS = MenuCommands.getCommands()
            .sub(new Command<ExecuteContext>("[research]").executor(ctx -> {
                if (!ctx.menu.isOpened()) return;
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
    private static Command<ExecuteContext> COMMANDS;
    private static final Executor WORKER = Executors.newFixedThreadPool(4);

    protected int currentPage = 0;
    protected int maxPage = 0;
    private final LotsMenuConfig cfg;

    private final List<LotData> lots = new ArrayList<>(45);
    private LotsResult searchResult;
    protected final Auction auction;
    private final LotData[] viewingLots;
    private boolean hasNextPage;
    private final Map<Object, SlotContent> lotRewriter = new IdentityHashMap<>();
    private final AtomicBoolean inWork = new AtomicBoolean();
    private final AtomicBoolean reqRefresh = new AtomicBoolean();
    private final SlotContent[] lotsLayer;
    private final SlotContent[] lotsLayerBuffer;

    public LotsMenu(LotsMenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        cfg = config;
        viewingLots = new LotData[animationMask().length];
        auction = BAuction.auction();
        addPlaceholderResolver(PLACEHOLDERS.bindCtx(this));
        lotsLayer = layers.getMatrix(2);
        lotsLayerBuffer = new SlotContent[lotsLayer.length];
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
            if (old != null && !lotRewriter.containsKey(old)) {
                var actual = old.update(auction);
                if (actual != old) {
                    setLotToBuffer(i, actual != null ? actual : old, actual == null);
                }
            }
        }
        super.tick();
        if (reqRefresh.get()) {
            asyncBuildMenu();
        }
    }

    public void research() {
        lots.clear();
        if (searchResult != null) searchResult.release();
        searchResult = null;
        refresh();
    }

    protected abstract LotsResult search();

    static void bootCommands(Command<ExecuteContext> base) {
        COMMANDS = base.and(LOTS_COMMANDS);
    }

    @Override
    public Command<ExecuteContext> getCommands() {
        return COMMANDS;
    }

    private void asyncBuildMenu() {
        if (!inWork.compareAndSet(false, true)) return;
        WORKER.execute(() -> {
            reqRefresh.set(false);
            Arrays.fill(lotsLayerBuffer, null);
            try {
                var lots = this.lots;
                LotsResult searchResult = this.searchResult;
                if (searchResult == null) {
                    lots.clear();
                    this.searchResult = searchResult = search();
                }
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
                        var rewrite = lotRewriter.get(lot0);
                        if (rewrite != null) {
                            setItem(rewrite, slot, lotsLayerBuffer);
                        } else {
                            var actual = lot0.update(auction);
                            LotData lot = actual == null ? lot0 : actual;
                            setLotToBuffer(slot, lot, actual == null);
                            viewingLots[slot] = actual;
                        }
                    } else {
                        break loop;
                    }
                }
                if (lots.size() < (currentPage + 1) * slots.length) {
                    var next = searchResult.next();
                    if (next != null) lots.add(next);
                    else maxPage = currentPage;
                }
                hasNextPage = lots.size() > (currentPage + 1) * slots.length;
                this.title.setData(this.setPlaceholders(this.config.title()));
            } finally {
                System.arraycopy(lotsLayerBuffer, 0, lotsLayer, 0, lotsLayer.length);
                inWork.set(false);
            }
        });
    }

    @Override
    protected void generate() {
        Arrays.fill(layers.getMatrix(3), null);
        reqRefresh.set(true);
        asyncBuildMenu();
    }

    private void setLotToBuffer(int slot, LotData lot, boolean outdated) {
        Material itemType = lot.itemStack().material();
        SlotFactory maker;
        ItemModel base = null;
        if (this instanceof VaultMenu && cfg.always_show_vault_lot) {
            maker = cfg.vault_lot;
            base = lot.itemStack().itemModel().withAmount(lot.normalCount());
        } else if (lot.getClass() == ClientVaultLot.class) {
            maker = cfg.vault_lot;
            base = lot.itemStack().itemModel().withAmount(lot.normalCount());
        } else if (outdated) {
            maker = cfg.sold;
        } else if (lot.isOwner(viewer.getUniqueId())) {
            maker = cfg.owned;
            base = lot.itemStack().itemModel().withAmount(lot.normalCount());
        } else if (Tag.SHULKER_BOXES.isTagged(itemType) || Tag.ITEMS_BUNDLES.isTagged(itemType)) {
            maker = cfg.container;
            base = lot.itemStack().itemModel();
        } else if (lot.normalCount() == 1) {
            maker = cfg.one;
            base = lot.itemStack().itemModel();
        } else {
            maker = cfg.many;
            base = lot.itemStack().itemModel().withAmount(lot.normalCount());
        }
        var slotData = maker.build(base, lot.placeholders());
        slotData.setPayload(lot);
        setItem(slotData, slot, lotsLayerBuffer);
    }

    public void rewriteLotDisplay(Object lot, SlotContent content) {
        lotRewriter.put(lot, content);
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
                .field(SlotFactory.CODEC, "vault_lot", v -> v.vault_lot, (m, v) -> m.vault_lot = v)
                .bool("always_show_vault_lot", v -> v.always_show_vault_lot, (m, v) -> m.always_show_vault_lot = v);

        public int[] slots;
        public SlotFactory sold;
        public SlotFactory owned;
        public SlotFactory container;
        public SlotFactory one;
        public SlotFactory many;
        public SlotFactory taken;
        public SlotFactory purchased;
        public SlotFactory vault_lot;
        public boolean always_show_vault_lot;

    }
}
