package com.sappersquad.packwork.pack;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A grid cell in the pack GUI. Unlike a normal slot it is not welded to one
 * backing index - it is <em>rebound</em> to whichever backing slot the current
 * tab/search/page maps to this position. When a position has nothing to show it
 * goes inactive and renders/behaves as empty.
 *
 * <p>Carries the copy-slot pattern (cache the returned stack, write back on
 * {@code setChanged}) inline rather than extending {@code StackCopySlot}, because
 * that class makes {@link #remove} final - and with per-slot DEPTH, {@code remove}
 * is exactly where the escape hatch has to close: vanilla's pickup path calls
 * {@code tryRemove(count, Integer.MAX_VALUE, ...)} (verified in the 1.21.1
 * sources), which would put a whole 384-deep stack straight onto the cursor.
 * Here every removal is clamped to one vanilla stack of the item.
 */
public class PackViewSlot extends Slot {

    private static final Container EMPTY_INV = new SimpleContainer(0);

    private final PackInventory handler;
    private int backingIndex = -1;
    private boolean active = false;

    @Nullable
    private ItemStack cachedReturnedStack = null;

    public PackViewSlot(PackInventory handler, int x, int y) {
        super(EMPTY_INV, 0, x, y);
        this.handler = handler;
    }

    /** Point this grid cell at a backing slot (or deactivate it). */
    public void bind(int backingIndex, boolean active) {
        this.backingIndex = backingIndex;
        this.active = active;
    }

    public int backingIndex() {
        return backingIndex;
    }

    @Override
    public boolean isActive() {
        return active && backingIndex >= 0;
    }

    private ItemStack getStackCopy() {
        return isActive() ? handler.getStackInSlot(backingIndex) : ItemStack.EMPTY;
    }

    private void setStackCopy(ItemStack stack) {
        if (isActive()) {
            handler.setStackInSlot(backingIndex, stack);
        }
    }

    // ---- copy-slot plumbing (vanilla mutates getItem()'s result, then calls setChanged) ----

    @Override
    public ItemStack getItem() {
        return cachedReturnedStack = getStackCopy();
    }

    @Override
    public void set(ItemStack stack) {
        setStackCopy(stack);
        cachedReturnedStack = stack;
    }

    @Override
    public void setChanged() {
        if (cachedReturnedStack != null) {
            set(cachedReturnedStack);
        }
    }

    /**
     * Take out of the slot - CLAMPED to one vanilla stack of the item, whatever was
     * asked for. A deep slot pays out 64 at a time; the rest stays safely in the pack.
     */
    @Override
    public ItemStack remove(int amount) {
        ItemStack stack = getStackCopy();   // already a fresh copy off the component
        ItemStack ret = stack.split(Math.min(amount, stack.getMaxStackSize()));
        set(stack);
        cachedReturnedStack = null;
        return ret;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!isActive() || !handler.isItemValid(backingIndex, stack)) return false;
        // Swap-guard: swapping the cursor into an OVERSIZED slot hands the whole deep
        // stack to the cursor via setCarried (a path that never touches remove()), so a
        // different-item place is refused while the slot is deeper than one stack.
        ItemStack existing = getStackCopy();
        return existing.isEmpty()
                || ItemStack.isSameItemSameComponents(existing, stack)
                || existing.getCount() <= existing.getMaxStackSize();
    }

    @Override
    public boolean mayPickup(Player player) {
        return isActive() && !getStackCopy().isEmpty();
    }

    /** The slot's ceiling (used when merging unknown items): the tier's depth for a 64-stackable. */
    @Override
    public int getMaxStackSize() {
        return isActive() ? handler.getSlotLimit(backingIndex) : 64;
    }

    /** Per-item ceiling: the tier's DEPTH for this item, so cursor merges fill deep. */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return handler.depthFor(stack);
    }
}
