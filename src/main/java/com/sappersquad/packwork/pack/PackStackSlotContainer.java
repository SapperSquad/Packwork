package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.block.PackContainerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A one-slot container that stands in for a pack stack that does NOT live in the player's
 * inventory. On the server it delegates to the real host - a <em>placed</em> pack's
 * {@link PackContainerBlockEntity}, or a <em>worn</em> pack's Curios slot (through the
 * accessors the gated compat class builds) - so reads and writes hit the real stack. On the
 * client it holds the copy vanilla's slot-sync delivers. A hidden (inactive) menu slot backed
 * by this container is how the host's pack stack - with all its components - reaches the
 * viewing client, exactly as a carried pack rides its player-inventory slot. Never
 * player-interactable.
 *
 * <p>The getter must resolve the live stack EVERY call, never a captured copy - a captured
 * stack goes stale the moment the host syncs, and the menu draws an empty grid.
 */
public final class PackStackSlotContainer implements Container {

    private final Supplier<ItemStack> getter;   // null on the client
    private final Consumer<ItemStack> setter;   // null on the client
    private final Runnable dirty;               // null on the client
    private ItemStack clientCopy = ItemStack.EMPTY;

    private PackStackSlotContainer(Supplier<ItemStack> getter, Consumer<ItemStack> setter, Runnable dirty) {
        this.getter = getter;
        this.setter = setter;
        this.dirty = dirty;
    }

    /** Server side of a placed pack: delegate straight to the block entity. */
    public static PackStackSlotContainer forBlock(PackContainerBlockEntity be) {
        return new PackStackSlotContainer(be::getPackStack, be::setPackStack, be::setChanged);
    }

    /**
     * Server side of a worn pack. The accessors come from the gated Curios compat class -
     * this class stays free of any curios import. Persistence and client sync ride Curios'
     * own per-tick previous-vs-current stack diff (verified against curios-neoforge 9.5.1:
     * {@code DynamicStackHandler} keeps {@code previousStacks} and the tick handler syncs
     * on mismatch), so in-place component writes on the live stack are picked up without
     * an explicit dirty hook.
     */
    public static PackStackSlotContainer forWorn(Supplier<ItemStack> live, Consumer<ItemStack> writeback) {
        return new PackStackSlotContainer(live, writeback, () -> {});
    }

    /** Client side of any hosted pack: holds whatever the hidden slot syncs down. */
    public static PackStackSlotContainer clientSide() {
        return new PackStackSlotContainer(null, null, null);
    }

    /** The live pack stack this container fronts. */
    public ItemStack getPack() {
        return getter != null ? getter.get() : clientCopy;
    }

    /** Mark the backing host dirty (no-op on the client and for a worn pack, see above). */
    public void markChanged() {
        if (dirty != null) dirty.run();
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
        if (setter != null) setter.accept(stack);
        else clientCopy = stack;
    }

    // The pack stack is never removed through this container (the block is broken, or the
    // pack unequipped, to get it back); these exist only to satisfy the interface.
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
        setItem(0, ItemStack.EMPTY);
    }
}
