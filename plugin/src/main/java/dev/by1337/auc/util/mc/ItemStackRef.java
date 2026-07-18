package dev.by1337.auc.util.mc;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemStackRef {
    private final ItemStack base;
    private final Supplier<@Nullable ItemStack> getter;
    private final Consumer<@Nullable ItemStack> setter;
    private final BooleanSupplier isValid;


    public ItemStackRef(ItemStack base, Supplier<@Nullable ItemStack> getter, Consumer<@Nullable ItemStack> setter, BooleanSupplier isValid) {
        this.base = base;
        this.getter = getter;
        this.setter = setter;
        this.isValid = isValid;
    }

    public void remove() {
        MCUtil.assertMain();
        assertRefAlive();
        setter.accept(null);
    }

    public void setCount(int x) {
        MCUtil.assertMain();
        assertRefAlive();
        if (x <= 0) {
            remove();
            return;
        }
        setter.accept(base.asQuantity(x));
    }

    public boolean isValid() {
        if (!isValid.getAsBoolean()) return false;
        var v = getter.get();
        if (v == null || v.isEmpty()) return true;
        return base.isSimilar(getter.get());
    }

    private void assertRefAlive() {
        var v = getter.get();
        if (v == null || v.isEmpty()) return;
        if (!base.isSimilar(getter.get())) {
            throw new IllegalStateException("Item ref is lost!");
        }
    }
}
