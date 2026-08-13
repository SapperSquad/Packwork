package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.trinket.TrinketItem;
import com.sappersquad.packwork.trinket.TrinketType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemAccessItemHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.function.Supplier;

/**
 * The pack's trinket sockets: a tiny handler over the {@code pack_trinkets} component,
 * native on the transfer API (26.x: the legacy {@code ComponentItemHandler} base is
 * deprecated-for-removal). Socket counts never exceed a vanilla stack, so the sockets
 * stay on vanilla's {@code ItemContainerContents} - only the pack's DEEP store needed
 * its own holder.
 *
 * <p>Slot count is the tier's fitting count. Only trinket items fit, and never two of
 * the same kind (a second identical fitting does nothing).
 */
public class PackTrinketInventory extends ItemAccessItemHandler {

    private final int slots;

    public PackTrinketInventory(ItemAccess access, PackTier tier) {
        super(access, ModComponents.PACK_TRINKETS.get(), Math.max(1, tier.trinketSlots()));
        this.slots = tier.trinketSlots();
    }

    /**
     * Supplier form kept for the fixed-stack callers (trinket effects, tests, the dev
     * harness): the supplier is resolved ONCE - commits mutate that stack's components
     * in place. Live multi-stack hosts (the menu) use the {@link ItemAccess} form.
     */
    public PackTrinketInventory(Supplier<ItemStack> stack, PackTier tier) {
        this(ItemAccess.forStack(stack.get()), tier);
    }

    @Override
    public int size() {
        return slots;
    }

    /** Only fittings, one of each kind - a duplicate grants nothing, so refuse it. */
    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (!super.isValid(index, resource)) return false; // access item changed under us
        if (!(resource.getItem() instanceof TrinketItem)) return false;
        TrinketType type = TrinketType.of(resource.toStack(1));
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
        ItemResource r = getResource(slot);
        return r.isEmpty() ? ItemStack.EMPTY : r.toStack(getAmountAsInt(slot));
    }

    public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate) {
        if (toInsert.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = insert(slot, ItemResource.of(toInsert), toInsert.getCount(), tx);
            if (!simulate && moved > 0) tx.commit();
            return moved >= toInsert.getCount() ? ItemStack.EMPTY
                    : toInsert.copyWithCount(toInsert.getCount() - moved);
        }
    }

    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemResource r = getResource(slot);
        if (r.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = extract(slot, r, Math.min(amount, r.getMaxStackSize()), tx);
            if (!simulate && moved > 0) tx.commit();
            return moved <= 0 ? ItemStack.EMPTY : r.toStack(moved);
        }
    }

    /**
     * Direct socket write - the menu slot's set path (clearing a socket must be
     * allowed: sync sets air). Bypasses {@link #isValid} exactly as the old copy-slot
     * did for programmatic writes.
     */
    public void setSlot(int index, ItemResource resource, int amount) {
        ItemResource accessResource = itemAccess.getResource();
        if (accessResource.isEmpty()) return;
        var contents = getContents(accessResource);
        net.minecraft.core.NonNullList<ItemStack> list =
                net.minecraft.core.NonNullList.withSize(Math.max(contents.getSlots(), size()), ItemStack.EMPTY);
        contents.copyInto(list);
        list.set(index, amount <= 0 || resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(amount));
        ItemResource updated = accessResource.with(() -> component,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(list));
        try (Transaction tx = Transaction.openRoot()) {
            itemAccess.exchange(updated, itemAccess.getAmount(), tx);
            tx.commit();
        }
    }
}
