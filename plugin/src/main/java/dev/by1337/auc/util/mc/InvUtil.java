package dev.by1337.auc.util.mc;

import dev.by1337.auc.BAuction;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class InvUtil {

    public static boolean giveOrDrop(Player player, ItemStack... items) {
        Collection<ItemStack> leftover = !canGiveItems(player) ? Arrays.asList(items) : player.getInventory().addItem(items).values();
        for (ItemStack value : leftover) {
            player.getWorld().dropItemNaturally(player.getLocation(), value);
        }
        return leftover.isEmpty();
    }
    public static boolean canGiveItems(Player player){
        var list = BAuction.playerList();
        return !player.isDead() && (list == null || list.isOnline(player.getUniqueId()));
    }

    public static boolean tryAddFully(ItemStack item, Inventory to, boolean dryRun) {
        return tryAddFully(item, to, to.getSize(), dryRun);
    }

    public static boolean tryAddFully(ItemStack item, PlayerInventory to, boolean dryRun) {
        return tryAddFully(item, to, 36, dryRun);
    }

    public static boolean tryAddFully(ItemStack item, Inventory to, int size, boolean dryRun) {
        if (item.isEmpty()) return true;
        int maxStack = item.getMaxStackSize();
        int count = item.getAmount();
        Int2IntArrayMap map = new Int2IntArrayMap();
        for (int slot = 0; slot < size; slot++) {
            if (count == 0) break;
            var current = to.getItem(slot);
            int base;
            if (current == null) {
                base = 0;
            } else if (current.isSimilar(item)) {
                base = current.getAmount();
            } else {
                continue;
            }
            int x = Math.min(maxStack - base, count);
            if (x != 0) {
                count -= x;
                map.put(slot, x + base);
            }
        }
        if (dryRun) return count == 0;
        if (count != 0) return false;
        map.int2IntEntrySet().forEach(entry -> {
            var i = to.getItem(entry.getIntKey());
            if (i != null) {
                i.setAmount(entry.getIntValue());
            } else {
                to.setItem(entry.getIntKey(), item.asQuantity(entry.getIntValue()));
            }
        });
        return true;
    }
}
