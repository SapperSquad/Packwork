package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.trinket.TrinketItem;
import com.sappersquad.packwork.trinket.TrinketType;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.function.Supplier;

/**
 * The pack's trinket sockets: a tiny handler over the {@code pack_trinkets} component,
 * native on Fabric's transfer API. Socket counts never exceed a vanilla stack, so the
 * sockets stay on vanilla's {@link ItemContainerContents} - only the pack's DEEP store
 * needed its own holder.
 *
 * <p>Slot count is the tier's fitting count. Only trinket items fit, and never two of
 * the same kind (a second identical fitting does nothing). Reads re-derive off the
 * context's CURRENT stack every call - a client menu is built a tick before its host
 * stack syncs, and a captured read would be air forever (the Phase-1 resolve-live
 * lesson; it showed as empty trinket sockets on the first 26.1 autoshot).
 */
public class PackTrinketInventory {

    private final ContainerItemContext context;
    private final int slots;

    public PackTrinketInventory(ContainerItemContext context, PackTier tier) {
        this.context = context;
        this.slots = Math.max(1, tier.trinketSlots());
    }

    /**
     * Supplier form kept for the fixed-stack callers (trinket effects, tests, the dev
     * harness): the supplier is resolved ONCE - commits mutate that stack's components
     * in place. Live multi-stack hosts (the menu) use the {@link ContainerItemContext} form.
     */
    public PackTrinketInventory(Supplier<ItemStack> stack, PackTier tier) {
        this(ContainerItemContext.ofSingleSlot(
                new com.sappersquad.packwork.transfer.LiveStackStorage(stack.get())), tier);
    }

    public int size() {
        return slots;
    }

    private ItemVariant host() {
        return context.getItemVariant();
    }

    private ItemContainerContents contents() {
        ItemVariant host = host();
        if (!(host.getItem() instanceof PackItem)) return ItemContainerContents.EMPTY;
        ItemContainerContents c = host.toStack().get(ModComponents.PACK_TRINKETS.get());
        return c == null ? ItemContainerContents.EMPTY : c;
    }

    /** Slot count of a vanilla contents value (pure vanilla 26.1 has no getSlots). */
    private static int slotCount(ItemContainerContents contents) {
        return (int) contents.allItemsCopyStream().count();
    }

    private static ItemStack stackAt(ItemContainerContents contents, int index) {
        int n = slotCount(contents);
        if (index >= n) return ItemStack.EMPTY;
        NonNullList<ItemStack> list = NonNullList.withSize(n, ItemStack.EMPTY);
        contents.copyInto(list);
        return list.get(index);
    }

    private boolean write(int index, ItemStack stack, TransactionContext tx) {
        ItemVariant host = host();
        if (!(host.getItem() instanceof PackItem) || context.getAmount() < 1) return false;
        ItemContainerContents contents = contents();
        NonNullList<ItemStack> list = NonNullList.withSize(Math.max(slotCount(contents), size()), ItemStack.EMPTY);
        contents.copyInto(list);
        list.set(index, stack);
        ItemStack carrier = host.toStack();
        carrier.set(ModComponents.PACK_TRINKETS.get(), ItemContainerContents.fromItems(list));
        return context.exchange(ItemVariant.of(carrier), 1, tx) == 1;
    }

    /** Only fittings, one of each kind - a duplicate grants nothing, so refuse it. */
    public boolean isValid(int index, ItemStack stack) {
        if (!(host().getItem() instanceof PackItem)) return false; // live host check
        if (!(stack.getItem() instanceof TrinketItem)) return false;
        TrinketType type = TrinketType.of(stack);
        for (int i = 0; i < size(); i++) {
            if (i == index) continue;
            if (TrinketType.of(getStackInSlot(i)) == type) return false;
        }
        return true;
    }

    // ---- legacy-shaped conveniences (menu socket slots, effects, tests) ----

    public int getSlots() {
        return slots;
    }

    public ItemStack getStackInSlot(int slot) {
        return stackAt(contents(), slot).copy();
    }

    public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate) {
        if (toInsert.isEmpty()) return ItemStack.EMPTY;
        if (!isValid(slot, toInsert)) return toInsert;
        ItemStack cur = getStackInSlot(slot);
        if (!cur.isEmpty() && !ItemStack.isSameItemSameComponents(cur, toInsert)) return toInsert;
        int limit = Math.min(toInsert.getMaxStackSize(), 64);
        int room = limit - cur.getCount();
        if (room <= 0) return toInsert;
        int moved = Math.min(room, toInsert.getCount());
        try (Transaction tx = Transaction.openOuter()) {
            boolean ok = write(slot, toInsert.copyWithCount(cur.getCount() + moved), tx);
            if (!ok) return toInsert;
            if (!simulate) tx.commit();
        }
        return moved >= toInsert.getCount() ? ItemStack.EMPTY
                : toInsert.copyWithCount(toInsert.getCount() - moved);
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack cur = getStackInSlot(slot);
        if (cur.isEmpty()) return ItemStack.EMPTY;
        int moved = Math.min(amount, Math.min(cur.getCount(), cur.getMaxStackSize()));
        ItemStack remaining = cur.copyWithCount(cur.getCount() - moved);
        try (Transaction tx = Transaction.openOuter()) {
            boolean ok = write(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining, tx);
            if (!ok) return ItemStack.EMPTY;
            if (!simulate) tx.commit();
        }
        return cur.copyWithCount(moved);
    }

    /**
     * Direct socket write - the menu slot's set path (clearing a socket must be
     * allowed: sync sets air). Bypasses {@link #isValid} exactly as the old copy-slot
     * did for programmatic writes.
     */
    public void setSlot(int index, ItemStack stack) {
        try (Transaction tx = Transaction.openOuter()) {
            if (write(index, stack == null ? ItemStack.EMPTY : stack, tx)) tx.commit();
        }
    }
}
