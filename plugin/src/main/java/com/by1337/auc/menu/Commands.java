package com.by1337.auc.menu;

import com.by1337.auc.BAuction;
import com.by1337.auc.auc.ClientAucLot;
import com.by1337.auc.auc.ClientVaultLot;
import com.by1337.auc.transaction.TakeLotTransaction;
import com.by1337.auc.transaction.TakeVaultLotTransaction;
import dev.by1337.bmenu.command.ExecuteContext;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.cmd.Command;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Commands {
    private static final Logger log = LoggerFactory.getLogger(Commands.class);
    private static final Command<ExecuteContext> MENU_COMMANDS = new Command<ExecuteContext>("root")
            .sub(new Command<Menu>("[take_lot]").executor(menu -> {
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
            }).map(v -> v.menu));

    public static Command<ExecuteContext> commands() {
        return MENU_COMMANDS;
    }
}
