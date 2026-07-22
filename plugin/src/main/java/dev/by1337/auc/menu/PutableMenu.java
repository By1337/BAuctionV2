package dev.by1337.auc.menu;

import dev.by1337.bmenu.loader.MenuConfig;
import dev.by1337.bmenu.menu.AbstractMenu;
import dev.by1337.bmenu.menu.Menu;
import dev.by1337.bmenu.slot.SlotContent;
import dev.by1337.bmenu.slot.impl.SimpleSlotContent;
import dev.by1337.core.BCore;
import dev.by1337.item.ItemModel;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class PutableMenu extends AbstractMenu {
    private final IntSet putableSlots = new IntArraySet();
    private final SlotContent[] playerItems;
    private static final SimpleSlotContent EMPTY = new SimpleSlotContent(ItemModel.AIR);

    public PutableMenu(MenuConfig config, Player viewer, @Nullable Menu previousMenu) {
        super(config, viewer, previousMenu);
        playerItems = layers.getMatrix(3);
    }

    public void addPutableSlot(int slot) {
        putableSlots.add(slot);
        playerItems[slot] = EMPTY;
    }

    public void clearPutableSlots() {
        var inv = getInventory();
        var it = putableSlots.intIterator();
        while (it.hasNext()) {
            inv.setItem(it.nextInt(), null);
        }
        BCore.getInventoryUtil().flushInv(viewer);
    }

    public List<ItemStack> collectItems() {
        var inv = getInventory();
        List<ItemStack> result = new ArrayList<>();
        var it = putableSlots.intIterator();
        while (it.hasNext()) {
            var item = inv.getItem(it.nextInt());
            if (item != null && !item.getType().isAir()) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public void onClick(InventoryDragEvent e) {
        int size = getInventory().getSize();
        if (e.getRawSlots().stream().allMatch(i -> i >= size || putableSlots.contains(i.intValue()))) {
            e.setCancelled(false);
            return;
        }
        super.onClick(e);
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() != getInventory() || putableSlots.contains(e.getSlot())) {
            e.setCancelled(false);
            return;
        }
        super.onClick(e);
    }
}
