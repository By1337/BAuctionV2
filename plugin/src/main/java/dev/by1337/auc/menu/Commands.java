package dev.by1337.auc.menu;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.ClientAucLot;
import dev.by1337.auc.auc.ClientVaultLot;
import dev.by1337.auc.command.args.NumberArgument;
import dev.by1337.auc.search.filter.SearchFilterAndNotPair;
import dev.by1337.auc.transaction.BuyLotTransaction;
import dev.by1337.auc.transaction.TakeLotTransaction;
import dev.by1337.auc.transaction.TakeVaultLotTransaction;
import dev.by1337.bmenu.BMenu;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.cmd.Command;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Commands {
    private static final Logger log = LoggerFactory.getLogger(Commands.class);
    private static final Command<ExecuteContext> MENU_COMMANDS = new Command<ExecuteContext>("root")
            .sub(new Command<ExecuteContext>("[take_lot]").executor(ctx -> {
                var menu = ctx.menu;
                Player viewer = menu.viewer();
                var auction = BAuction.plugin().auction();
                if (auction == null) {
                    BAuction.sendMessage("auction_is_disabled", viewer);
                    return;
                }
                if (menu.lastClickedItemPayload() instanceof ClientAucLot lot) {
                    auction.apply(new TakeLotTransaction(viewer.getUniqueId(), lot))
                            .then(v -> {
                                if (v == null || !v.success) return;
                                if (menu.isOpened()) {
                                    if (menu instanceof LotsMenu lotsMenu) {
                                        lotsMenu.rewriteLotDisplay(lot, lotsMenu.cfg().taken.build(lot.itemStack.itemModel(), lot.placeholders()));
                                    }
                                    menu.refresh();
                                }
                            });

                } else if (menu.lastClickedItemPayload() instanceof ClientVaultLot lot) {
                    auction.apply(new TakeVaultLotTransaction(menu.viewer().getUniqueId(), lot))
                            .then(v -> {
                                if (v == null || !v.success) return;
                                if (menu.isOpened()) {
                                    if (menu instanceof LotsMenu lotsMenu) {
                                        lotsMenu.rewriteLotDisplay(lot, lotsMenu.cfg().taken.build(lot.itemStack().itemModel(), lot.placeholders()));
                                    }
                                    menu.refresh();
                                }
                            });
                } else {
                    log.error("{} is not a auction lot failed to take", menu.lastClickedItemPayload());
                }
            }))
            .sub(new Command<ExecuteContext>("[buy_lot]").executor(
                    new NumberArgument<>("count"),
                    (ctx, count0) -> {
                        var menu = ctx.menu;
                        Player viewer = menu.viewer();
                        var auction = BAuction.auction();
                        if (auction == null) {
                            BAuction.sendMessage("auction_is_disabled", viewer);
                            return;
                        }
                        if (menu.lastClickedItemPayload() instanceof ClientAucLot lot) {
                            int count = count0 != null ? Math.min(lot.count(), count0.intValue()) : lot.count();
                            auction.apply(new BuyLotTransaction(viewer.getUniqueId(), lot, count))
                                    .then(v -> {
                                        if (v == null || !v.success) return;
                                        if (count != lot.count()) return;
                                        if (menu.isOpened()) {
                                            if (menu instanceof LotsMenu lotsMenu) {
                                                lotsMenu.rewriteLotDisplay(lot, lotsMenu.cfg().purchased.build(lot.itemStack.itemModel(), lot.placeholders()));
                                            }
                                            menu.refresh();
                                        }
                                    });

                        } else {
                            log.error("{} is not a auction lot failed to buy_lot", menu.lastClickedItemPayload());
                        }
                    }))
            .sub(new Command<ExecuteContext>("[find_analogs]").executor(
                    (ctx) -> {
                        var menu = ctx.menu;
                        Player viewer = menu.viewer();
                        var auction = BAuction.auction();
                        if (auction == null) {
                            BAuction.sendMessage("auction_is_disabled", viewer);
                            return;
                        }
                        if (menu.lastClickedItemPayload() instanceof ClientAucLot lot) {
                            if (menu instanceof HomeMenu h) {
                                h.setSearch(new SearchFilterAndNotPair(lot.itemStack.tags(), null, new String[0]));
                                h.setSearchInput("<lang:bauctionv2.ah.search.analogs>");
                            } else {
                                var m = BMenu.menuLoader().create(BAuction.plugin().config().commands.ah_menu, menu.viewer(), menu);
                                if (m instanceof HomeMenu h){
                                    h.setSearch(new SearchFilterAndNotPair(lot.itemStack.tags(), null, new String[0]));
                                    h.setSearchInput("<lang:bauctionv2.ah.search.analogs>");
                                }
                                m.open();
                            }
                        } else {
                            log.error("{} is not a auction lot failed to find_analogs", menu.lastClickedItemPayload());
                        }
                    }));

    public static Command<ExecuteContext> create() {
        return MENU_COMMANDS.copy();
    }
}
