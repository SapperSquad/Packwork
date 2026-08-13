package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.trinket.TrinketAccess;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The pack's item store: ONE handler, native on Fabric's transfer API, backing the
 * standard {@code ItemStorage} lookups AND the menu/trinkets/sorting internals. (The
 * NeoForge 26.x branch is the same design over the same-shaped API - NeoForge's
 * transactional rework copied Fabric's - so the two implementations mirror each other
 * rule for rule.)
 *
 * <p>One flat inventory - tabs are virtual views computed over it, never physical
 * partitions, so sorting never moves an item and can never lose one.
 *
 * <p><b>The three pack rules live here, at the native choke points:</b>
 * <ul>
 * <li><b>DEPTH</b> - {@link PackSlot#getCapacity()}: each slot holds the item's own max
 *     stack x the tier's multiplier ({@link PackTier#slotDepth}).</li>
 * <li><b>NESTING</b> - {@link PackSlot#insert}: no pack-in-pack, ever (see DECISIONS.md).</li>
 * <li><b>ONE-STACK EXTRACT</b> - {@link PackSlot#extract}: every pull out of the pack is
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
 * extra items rather than voiding them. The context resolves the CURRENT stack on every
 * read, so a menu built a tick before its slot syncs simply reads empty until the pack
 * arrives (the old captured-stack silent-swallow bug cannot come back).
 */
public class PackInventory {

    /** Hard ceiling on addressable slots (matches the NeoForge branches). */
    public static final int MAX_SLOTS = 256;

    private final ContainerItemContext context;
    private final PackTier tier;
    private final NativeStorage nativeStorage = new NativeStorage();

    /** Fixed-stack form: commits mutate the given stack's components in place. */
    public PackInventory(ItemStack packStack, PackTier tier) {
        this(ContainerItemContext.ofSingleSlot(
                new com.sappersquad.packwork.transfer.LiveStackStorage(packStack)), tier);
    }

    /** Supplier form for fixed-stack callers: resolved ONCE at construction - commits
     *  mutate that stack in place. Live multi-stack hosts (the menu) bind a real
     *  {@link ContainerItemContext} instead. */
    public PackInventory(java.util.function.Supplier<ItemStack> stack, PackTier tier) {
        this(stack.get(), tier);
    }

    /**
     * Native form over any {@link ContainerItemContext} - the standard lookup's context,
     * a player inventory slot, or a menu host container.
     */
    public PackInventory(ContainerItemContext context, PackTier tier) {
        this.context = context;
        this.tier = tier;
    }

    private ItemVariant host() {
        return context.getItemVariant();
    }

    private static PackContents contentsOf(ItemVariant host) {
        if (!(host.getItem() instanceof PackItem)) return PackContents.EMPTY;
        PackContents c = host.toStack().get(ModComponents.PACK_CONTENTS.get());
        return c == null ? PackContents.EMPTY : c;
    }

    /** Write one slot's stack back onto the host pack, inside the given transaction.
     *  Returns true when the host accepted the write (it still holds exactly one pack). */
    private boolean writeSlot(int index, ItemStack newStack, TransactionContext tx) {
        ItemVariant host = host();
        if (!(host.getItem() instanceof PackItem) || context.getAmount() < 1) return false;
        PackContents updated = contentsOf(host).withSlot(index, newStack, size());
        ItemStack carrier = host.toStack();
        carrier.set(ModComponents.PACK_CONTENTS.get(), updated);
        return context.exchange(ItemVariant.of(carrier), 1, tx) == 1;
    }

    // ---- the native SlottedStorage surface ----

    /** Live BREADTH: a Bottomless Lining grows the slot count (and never truncates). */
    public int size() {
        ItemVariant r = host();
        if (!(r.getItem() instanceof PackItem)) return 0;
        return Math.min(MAX_SLOTS, TrinketAccess.capacity(r.toStack(1)));
    }

    /**
     * The native transfer face - what the standard {@code ItemStorage} lookups hand to
     * automation. A nested view (not the class itself) because the legacy-shaped
     * {@code getSlots()} convenience below and {@code SlottedStorage}'s own default of
     * the same name collide; the RULES all live in {@link PackSlot} either way.
     */
    public SlottedStorage<ItemVariant> storage() {
        return nativeStorage;
    }

    private class NativeStorage implements SlottedStorage<ItemVariant> {

        @Override
        public int getSlotCount() {
            return size();
        }

        @Override
        public SingleSlotStorage<ItemVariant> getSlot(int index) {
            return new PackSlot(index);
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) {
            long moved = 0;
            int n = size();
            // merge into part-filled matching slots first, then fill empties - the same order
            // the legacy insertAll walked, so automation and trinkets file identically
            for (int pass = 0; pass < 2 && moved < maxAmount; pass++) {
                for (int i = 0; i < n && moved < maxAmount; i++) {
                    boolean empty = contentsOf(host()).getStackInSlot(i).isEmpty();
                    if ((pass == 0) == empty) continue;
                    moved += new PackSlot(i).insert(resource, maxAmount - moved, tx);
                }
            }
            return moved;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
            long moved = 0;
            int n = size();
            for (int i = 0; i < n && moved < maxAmount; i++) {
                moved += new PackSlot(i).extract(resource, maxAmount - moved, tx);
            }
            return moved;
        }

        @Override
        public Iterator<StorageView<ItemVariant>> iterator() {
            int n = size();
            return new Iterator<>() {
                private int next = 0;

                @Override
                public boolean hasNext() {
                    return next < n;
                }

                @Override
                public StorageView<ItemVariant> next() {
                    if (next >= n) throw new NoSuchElementException();
                    return new PackSlot(next++);
                }
            };
        }
    }

    /** One slot of the pack, all reads live off the host's current component state. */
    private class PackSlot implements SingleSlotStorage<ItemVariant> {

        private final int index;

        PackSlot(int index) {
            this.index = index;
        }

        private ItemStack current() {
            return contentsOf(host()).getStackInSlot(index);
        }

        @Override
        public boolean isResourceBlank() {
            return current().isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            ItemStack s = current();
            return s.isEmpty() ? ItemVariant.blank() : ItemVariant.of(s);
        }

        @Override
        public long getAmount() {
            return current().getCount();
        }

        /** Per-slot DEPTH: the item's own max stack x the tier's multiplier. */
        @Override
        public long getCapacity() {
            ItemStack s = current();
            return s.isEmpty()
                    ? tier.slotDepth(Item.DEFAULT_MAX_STACK_SIZE)
                    : tier.slotDepth(s.getMaxStackSize());
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext tx) {
            if (resource.isBlank() || maxAmount <= 0) return 0;
            // NESTING: no pack-in-pack, ever; and refuse to answer for a non-pack host
            if (resource.getItem() instanceof PackItem) return 0;
            if (!(host().getItem() instanceof PackItem)) return 0;
            ItemStack cur = current();
            if (!cur.isEmpty() && !resource.matches(cur)) return 0;
            int depth = tier.slotDepth(resource.toStack().getMaxStackSize());
            int room = depth - cur.getCount();
            if (room <= 0) return 0;
            int moved = (int) Math.min(maxAmount, room);
            ItemStack updated = resource.toStack(cur.getCount() + moved);
            return writeSlot(index, updated, tx) ? moved : 0;
        }

        /** Every pull out of the pack is at most ONE vanilla stack of the item. */
        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext tx) {
            if (resource.isBlank() || maxAmount <= 0) return 0;
            ItemStack cur = current();
            if (cur.isEmpty() || !resource.matches(cur)) return 0;
            int clamp = (int) Math.min(maxAmount, resource.toStack().getMaxStackSize());
            int moved = Math.min(clamp, cur.getCount());
            if (moved <= 0) return 0;
            ItemStack updated = cur.copyWithCount(cur.getCount() - moved);
            return writeSlot(index, updated.isEmpty() ? ItemStack.EMPTY : updated, tx) ? moved : 0;
        }
    }

    // ---- legacy-shaped conveniences (the menu/trinkets/sorting/test surface) ----

    public int getSlots() {
        return size();
    }

    /** A fresh copy of the slot's stack, deep count intact. */
    public ItemStack getStackInSlot(int slot) {
        return contentsOf(host()).getStackInSlot(slot);
    }

    /** Insert up to the tier's depth; returns what did not fit (EMPTY when all did). */
    public ItemStack insertItem(int slot, ItemStack toInsert, boolean simulate) {
        if (toInsert.isEmpty()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openOuter()) {
            long moved = new PackSlot(slot).insert(ItemVariant.of(toInsert), toInsert.getCount(), tx);
            if (!simulate && moved > 0) tx.commit();
            return moved >= toInsert.getCount() ? ItemStack.EMPTY
                    : toInsert.copyWithCount(toInsert.getCount() - (int) moved);
        }
    }

    /** Extract - clamped to one vanilla stack by the native rule above. */
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        PackSlot s = new PackSlot(slot);
        ItemVariant r = s.getResource();
        if (r.isBlank()) return ItemStack.EMPTY;
        try (Transaction tx = Transaction.openOuter()) {
            long moved = s.extract(r, amount, tx);
            if (!simulate && moved > 0) tx.commit();
            return moved <= 0 ? ItemStack.EMPTY : r.toStack((int) moved);
        }
    }

    /**
     * Arbitrary slot write (the view-slot set and Tidy Up's rewrite) - not an
     * insert/extract, so it goes through the host exchange directly. A silent no-op
     * when the host holds no pack (the old live-resolve contract).
     */
    public void setStackInSlot(int slot, ItemStack stack) {
        if (!(host().getItem() instanceof PackItem)) return;
        try (Transaction tx = Transaction.openOuter()) {
            if (writeSlot(slot, stack, tx)) tx.commit();
        }
    }

    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.getItem() instanceof PackItem) return false;
        return host().getItem() instanceof PackItem;
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
