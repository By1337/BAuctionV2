/*
package com.by1337.auc.command;

import com.by1337.auc.BAuction;
import com.by1337.auc.command.args.NumberArgument;
import com.by1337.auc.util.mc.ItemStackRef;
import com.by1337.auc.util.mc.MCUtil;
import dev.by1337.cmd.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public class SellaCommand extends Command<CommandSender> {

    public SellaCommand(String name) {
        super(name);
        requires(s -> s instanceof Player);
        executor(
                new NumberArgument<>("price"),
                new NumberArgument<>("count"),
                this::run
        );
    }

    private void run(CommandSender sender, Number price0, Number count0) {
        if (!(sender instanceof Player player)) return;
        if (price0 == null) {
            BAuction.sendMessage("sell_req_price", player);
            return;
        }
        var playerItem = player.getInventory().getItemInMainHand();
        if (playerItem.isEmpty()) {
            BAuction.sendMessage("sell_req_item", player);
            return;
        }
        int count = count0 == null ? playerItem.getAmount() : Math.clamp(count0.intValue(), 1, playerItem.getAmount());

        testSlot(player, 0, player.getInventory(), price0.doubleValue(), count, playerItem.asOne());
    }

    private void testSlot(Player player, int slot, PlayerInventory inv, double price, int count, ItemStack baseCopy) {
        MCUtil.assertMain();
        UUID who = player.getUniqueId();
        if (!BAuction.playerList().isOnline(who)) return;
        if (slot >= 36) return;
        var item = inv.getItem(slot);
        if (item == null || !baseCopy.isSimilar(item) || item.getAmount() < count) {
            testSlot(player, slot + 1, inv, price, count, baseCopy);
            return;
        }
        SellCommand.doSell(player, new ItemStackRef(
                baseCopy,
                () -> inv.getItem(slot),
                i -> inv.setItem(slot, i),
                () -> BAuction.playerList().isOnline(who)
        ), price, count, baseCopy, item.getAmount()).then(v -> {
            if (v != null && BAuction.playerList().isOnline(who) && !Bukkit.isStopping()) {
                MCUtil.ensureMain(() -> testSlot(player, slot, inv, price, count, baseCopy));
            }
        });
    }

}
*/
