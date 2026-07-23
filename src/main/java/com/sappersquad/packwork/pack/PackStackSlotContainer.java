package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.block.PackContainerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A one-slot container that stands in for a <em>placed</em> pack's stack inside the menu.
 * On the server it delegates to the {@link PackContainerBlockEntity} (so reads/writes hit
 * the real block); on the client it holds the copy vanilla's slot-sync delivers. A hidden
 * (inactive) menu slot backed by this container is how the block-entity's pack stack - with
 * all its components - reaches the viewing client, exactly as a carried pack rides its
 * player-inventory slot. Never player-interactable.
 */
public final class PackStackSlotContainer implements Container {

    private final PackContainerBlockEntity be; // non-null on the server; null on the client
    private ItemStack clientCopy = ItemStack.EMPTY;

    public PackStackSlotContainer(PackContainerBlockEntity be) {
        this.be = be;
    }

    /** The live pack stack this container fronts. */
    public ItemStack getPack() {
        return be != null ? be.getPackStack() : clientCopy;
    }

    /** Mark the backing block-entity dirty (no-op on the client). */
    public void markChanged() {
        if (be != null) be.setChanged();
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getPack().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? getPack() : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        if (be != null) be.setPackStack(stack);
        else clientCopy = stack;
    }

    // The pack stack is never removed through this container (the block is broken to get it
    // back); these exist only to satisfy the interface.
    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setChanged() {
        markChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        if (be != null) be.setPackStack(ItemStack.EMPTY);
        else clientCopy = ItemStack.EMPTY;
    }
}
