package com.sappersquad.packwork.pack;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.StackCopySlot;

/**
 * A grid cell in the pack GUI. Unlike a normal slot it is not welded to one
 * backing index - it is <em>rebound</em> to whichever backing slot the current
 * tab/search/page maps to this position. When a position has nothing to show it
 * goes inactive and renders/behaves as empty.
 *
 * <p>Extends {@link StackCopySlot} so it plays nicely with vanilla's mutate-in-place
 * container helpers against the immutable component-backed store.
 */
public class PackViewSlot extends StackCopySlot {

    private final IItemHandlerModifiable handler;
    private int backingIndex = -1;
    private boolean active = false;

    public PackViewSlot(IItemHandlerModifiable handler, int x, int y) {
        super(x, y);
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

    @Override
    protected ItemStack getStackCopy() {
        return isActive() ? handler.getStackInSlot(backingIndex) : ItemStack.EMPTY;
    }

    @Override
    protected void setStackCopy(ItemStack stack) {
        if (isActive()) {
            handler.setStackInSlot(backingIndex, stack);
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isActive() && handler.isItemValid(backingIndex, stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return isActive() && !getStackCopy().isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return isActive() ? handler.getSlotLimit(backingIndex) : 64;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(stack.getMaxStackSize(), getMaxStackSize());
    }
}
