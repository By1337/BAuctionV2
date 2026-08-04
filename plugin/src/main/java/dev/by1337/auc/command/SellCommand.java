package dev.by1337.auc.command;

import dev.by1337.auc.BAuction;
import dev.by1337.auc.auc.GhostLot;
import dev.by1337.auc.command.args.ArgumentNumber;
import dev.by1337.auc.transaction.AddLotTransaction;
import dev.by1337.auc.util.mc.InvUtil;
import dev.by1337.auc.util.mc.ItemStackRef;
import dev.by1337.auc.util.mc.MCUtil;
import dev.by1337.auc.util.number.EconomyUtil;
import dev.by1337.cmd.Command;
import dev.by1337.sync.common.callback.ResponseFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SellCommand extends Command<CommandSender> {

    private static final Logger log = LoggerFactory.getLogger(SellCommand.class);
    private final boolean dsell;

    public SellCommand(String name, boolean dsell) {
        super(name);
        this.dsell = dsell;
        requires(s -> s instanceof Player);
        executor(
                new ArgumentNumber<>("price"),
                new ArgumentNumber<>("count"),
                this::run
        );
    }

    private void run(CommandSender sender, Number price0, Number count0) {
        if (!(sender instanceof Player player)) return;
        if (price0 == null) {
            BAuction.sendMessage("sell_req_price", player);
            return;
        }
        var inv = player.getInventory();
        var playerItem = player.getInventory().getItemInMainHand();
        if (playerItem.isEmpty()) {
            BAuction.sendMessage("sell_req_item", player);
            return;
        }
        var one = playerItem.asOne();
        int count = count0 == null ? playerItem.getAmount() : Math.clamp(count0.intValue(), 1, playerItem.getAmount());
        doSell(sender, new ItemStackRef(
                one,
                () -> inv.getItem(EquipmentSlot.HAND),
                i -> inv.setItem(EquipmentSlot.HAND, i),
                () -> BAuction.playerList().isOnline(player.getUniqueId())
        ), price0.doubleValue(), count, one, playerItem.getAmount(), dsell);
    }

    public static ResponseFuture<@Nullable GhostLot> doSell(CommandSender sender, ItemStackRef ref, double price, int count, ItemStack one, int totalCount, boolean dsell) {
        if (!(sender instanceof Player player)) return new ResponseFuture<>(null);
        MCUtil.assertMain();
        var auction = BAuction.plugin().auction();
        if (auction == null) {
            BAuction.sendMessage("auction_is_disabled", player);
            return new ResponseFuture<>(null);
        }
        if (one.isEmpty()) {
            BAuction.sendMessage("sell_req_item", player);
            return new ResponseFuture<>(null);
        }
        if (!ref.isValid()) return new ResponseFuture<>(null);
        UUID who = player.getUniqueId();
        ref.remove();
        AddLotTransaction transaction = new AddLotTransaction(one, who, price, count).dsell(dsell);
        return auction.apply(transaction).then(v -> {
            int result;
            if (v == null) {
                result = totalCount;
            } else {
                result = Math.max(0, totalCount - count);
                BAuction.sendMessage("sell_success", player, v.placeholders());
            }
            if (result > 0) {
                MCUtil.ensureMain(() -> {
                    if (ref.isValid()) {
                        ref.setCount(result);
                    } else {
                        if (!BAuction.playerList().isOnline(who) || !InvUtil.tryAddFully(one.asQuantity(result), player.getInventory(), false)) {
                            auction.addToVault(
                                    one,
                                    result,
                                    who,
                                    EconomyUtil.toCents((price / count) * result)
                            ).then(r -> {
                                if (r != null && r.success) {
                                    BAuction.sendMessage("sell_qa_moved_to_vault", player);
                                } else {
                                    log.error("игрок {} потерял {} x {} так как в инв не добавить", player.getName(), one, result);
                                }
                            });
                        }
                    }
                });
            }
        });
    }
}
