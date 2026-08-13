package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.trinket.TrinketAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

/**
 * The pack's item store: ONE handler, native on the NeoForge transfer API, backing the
 * standard {@code Capabilities.Item} capability AND the menu/trinkets/sorting internals.
 * (Up to 1.21.11 these were two layers - a legacy {@code ComponentItemHandler} inside,
 * a transactional wrapper outside; 26.x removed the store from vanilla's holder
 * entirely, so the layers merged here over {@link PackContents}.)
 *
 * <p>One flat inventory - tabs are virtual views computed over it, never physical
 * partitions, so sorting never moves an item and can never lose one.
 *
 * <p><b>The three pack rules live here, at the native choke points:</b>
 * <ul>
 * <li><b>DEPTH</b> - {@link #getCapacity}: each slot holds the item's own max stack x
 *     the tier's multiplier ({@link PackTier#slotDepth}).</li>
 * <li><b>NESTING</b> - {@link #isValid}: no pack-in-pack, ever (see DECISIONS.md).</li>
 * <li><b>ONE-STACK EXTRACT</b> - {@link #extract}: every pull out of the pack is
 *     clamped to one vanilla stack of the item, however deep the slot. Automation,
 *     cursors, and trinkets only ever see legal stacks.</li>
 * </ul>
 *
 * <p>The legacy-shaped conveniences ({@code insertItem}/{@code extractItem}/...) each
 * run one root transaction over the native path, so internal callers and external
 * automation are provably enforcing the same rules.
 *
 * <p>Slot count is live: a Bottomless Lining trinket grows it (BREADTH), and slots past
 * the current capacity are never truncated on write - removing the Lining hides the
 * extra items rather than voiding them.
 */
public class PackInventory extends ItemAccessResourceHandler<ItemResource> {

    private final PackTier tier;

    /** Fixed-stack form: commits mutate the given stack's components in place. */
    public PackInventory(ItemStack packStack, PackTier tier) {
        this(ItemAccess.forStack(packStack), tier);
    }

    /** Supplier form for fixed-stack callers: resolved ONCE at construction - commits
     *  mutate that stack in place. Live multi-stack hosts (the menu) bind a real
     *  {@link ItemAccess} instead. */
    public PackInventory(java.util.function.Supplier<ItemStack> stack, PackTier tier) {
        this(stack.get(), tier);
    }

    /**
     * Native form over any {@link ItemAccess} - the capability context, a player
     * inventory slot, or a menu host container. The access is resolved live on every
     * read, so a menu built a tick before its slot syncs simply reads empty until the
     * pack arrives (the old captured-stack silent-swallow bug cannot come back).
     */
    public PackInventory(ItemAccess access, PackTier tier) {
        super(access, 256);
        this.tier = tier;
    }

    private static PackContents contentsOf(ItemResource accessResource) {
        return accessResource.getItem() instanceof PackItem
                ? accessResource.getOrDefault(ModComponents.PACK_CONTENTS.get(), PackContents.EMPTY)
                : PackContents.EMPTY;
    }

    // ---- the native ResourceHandler surface ----

    /** Live BREADTH: a Bottomless Lining grows the slot count (and never truncates). */
    @Override
    public int size() {
        ItemResource r = itemAccess.getResource();
        if (!(r.getItem() instanceof PackItem)) return 0;
        return Math.min(256, TrinketAccess.capacity(r.toStack(1)));
    }

    @Override
    protected ItemResource getResourceFrom(ItemResource accessResource, int index) {
        ItemStack s = contentsOf(accessResource).getStackInSlot(index);
        return s.isEmpty() ? ItemResource.EMPTY : ItemResource.of(s);
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return contentsOf(accessResource).getStackInSlot(index).getCount();
    }

    @Override
    @Nullable
    protected ItemResource update(ItemResource accessResource, int index, ItemResource newResource, int newAmount) {
        PackContents updated = contentsOf(accessResource).withSlot(index, newResource.toStack(newAmount), size());
        return (ItemResource) accessResource.with(ModComponents.PACK_CONTENTS, updated);
    }

    /** No pack-in-pack, ever - blocks the dupe/lag surface. (Also refuses to answer for
     *  a non-pack access item, e.g. after the bound slot's stack was swapped out.) */
    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (resource.getItem() instanceof PackItem) return false;
        return itemAccess.getResource().getItem() instanceof PackItem;
    }

    /** Per-slot DEPTH: the item's own max stack x the tier's multiplier. */
    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return resource.isEmpty()
                ? tier.slotDepth(Item.DEFAULT_MAX_STACK_SIZE)
                : tier.slotDepth(resource.getMaxStackSize());
    }

    /** Every pull out of the pack is at most ONE vanilla stack of the item. */
    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
        return super.extract(index, resource, Math.min(amount, resource.getMaxStackSize()), tx);
    }

    // ---- legacy-shaped conveniences (the menu/trinkets/sorting/test surface) ----

    public int getSlots() {
        return size();
    }

    /** A fresh copy of the slot's stack, deep count intact. */
    public ItemStack getStackInSlot(int slot) {
        return contentsOf(itemAccess.getResource()).getStackInSlot(slot);
    }

    /** Insert up to the tier's depth; returns what did not fit (EMPTY when all did). */
    public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate) {
        if (toInsert.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = insert(slot, ItemResource.of(toInsert), toInsert.getCount(), tx);
            if (!simulate && moved > 0) tx.commit();
            return moved >= toInsert.getCount() ? ItemStack.EMPTY
                    : toInsert.copyWithCount(toInsert.getCount() - moved);
        }
    }

    /** Extract - clamped to one vanilla stack by the native rule above. */
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemResource r = getResource(slot);
        if (r.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = extract(slot, r, amount, tx);
            if (!simulate && moved > 0) tx.commit();
            return moved <= 0 ? ItemStack.EMPTY : r.toStack(moved);
        }
    }

    /**
     * Arbitrary slot write (the view-slot set and Tidy Up's rewrite) - not an
     * insert/extract, so it goes through the access exchange directly. A silent no-op
     * when the access holds no pack (the old live-resolve contract).
     */
    public void setStackInSlot(int slot, ItemStack stack) {
        ItemResource accessResource = itemAccess.getResource();
        if (!(accessResource.getItem() instanceof PackItem)) return;
        PackContents updated = contentsOf(accessResource).withSlot(slot, stack, size());
        ItemResource newResource = (ItemResource) accessResource.with(ModComponents.PACK_CONTENTS, updated);
        try (Transaction tx = Transaction.openRoot()) {
            itemAccess.exchange(newResource, itemAccess.getAmount(), tx);
            tx.commit();
        }
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        return isValid(slot, ItemResource.of(stack));
    }

    /** Empty-slot ceiling for generic consumers: the depth of an ordinary 64-stackable. */
    public int getSlotLimit(int slot) {
        return tier.slotDepth(Item.DEFAULT_MAX_STACK_SIZE);
    }

    /**
     * How deep a slot goes for this item at this tier. The rule itself lives on the tier
     * SSOT ({@link PackTier#slotDepth}); this just feeds it the item's own max stack.
     */
    public int depthFor(ItemStack stack) {
        return tier.slotDepth(stack.getMaxStackSize());
    }
}
