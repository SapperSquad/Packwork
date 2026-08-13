package com.sappersquad.packwork.transfer;

import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.trinket.TrinketAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.item.ItemAccessItemHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Packwork's stores on the NeoForge 21.9+ <b>transfer API</b> - the capability-facing
 * layer only. The legacy transfer interfaces were deprecated wholesale in 21.9 and their
 * standard capability tokens replaced ({@code Capabilities.Item/Fluid/Energy}), and the
 * official guidance is explicit that an {@code IItemHandler} cannot be wrapped into a
 * {@code ResourceHandler} (transactionality can't be retrofitted). So external automation
 * speaks these transactional handlers, while the menu, trinket effects, and sorting keep
 * the battle-tested legacy-shaped internals ({@code PackInventory} et al).
 *
 * <p><b>The three pack rules live here too, at the same choke points:</b> per-slot DEPTH
 * as {@link PackItemHandler#getCapacity} (the tier's multiplier over the item's own max
 * stack), NESTING refusal as {@link PackItemHandler#isValid}, and the one-vanilla-stack
 * extract clamp in {@link PackItemHandler#extract}. The automation gametests pin this
 * handler to the same observable behaviour as the internal one.
 *
 * <p>This class is deliberately the only place that touches
 * {@code net.neoforged.neoforge.transfer.*} handler bases, so a future NeoForge drift
 * lands in one file. (VERSION-SPECIFIC: does not exist on the 1.21.1 branch.)
 */
public final class PackTransfer {

    /** The pack's flat item store as a transactional {@code ResourceHandler<ItemResource>}. */
    public static class PackItemHandler extends ItemAccessItemHandler {
        private final PackTier tier;

        public PackItemHandler(ItemAccess access, PackTier tier) {
            super(access, ModComponents.PACK_CONTENTS.get(), 256);
            this.tier = tier;
        }

        /** Live BREADTH: a Bottomless Lining grows the slot count (and never truncates). */
        @Override
        public int size() {
            return Math.min(256, TrinketAccess.capacity(itemAccess.getResource().toStack(1)));
        }

        /** No pack-in-pack, ever (see DECISIONS.md) - blocks the dupe/lag surface. */
        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (resource.getItem() instanceof PackItem) return false;
            return super.isValid(index, resource);
        }

        /** Per-slot DEPTH: the item's own max stack x the tier's multiplier. */
        @Override
        protected int getCapacity(int index, ItemResource resource) {
            return resource.isEmpty()
                    ? tier.slotDepth(Item.DEFAULT_MAX_STACK_SIZE)
                    : tier.slotDepth(resource.getMaxStackSize());
        }

        /** Every pull out of the pack is at most ONE vanilla stack of the item - the same
         *  guarantee the legacy handler makes to hoppers and cursors. */
        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext tx) {
            return super.extract(index, resource, Math.min(amount, resource.getMaxStackSize()), tx);
        }
    }

    /** The Waterskin Rack's tank (tier-scaled capacity) over the {@code pack_fluid} component. */
    public static ItemAccessFluidHandler fluid(ItemAccess access, ItemStack packStack) {
        return new ItemAccessFluidHandler(access, ModComponents.PACK_FLUID.get(),
                com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(packStack));
    }

    /** The Charge Crystal's arcane charge (tier-scaled) over the {@code pack_energy} component. */
    public static ItemAccessEnergyHandler energy(ItemAccess access, ItemStack packStack) {
        int capacity = com.sappersquad.packwork.pack.PackEnergyStorage.capacityFor(packStack);
        int transfer = com.sappersquad.packwork.pack.PackEnergyStorage.transferFor(packStack);
        return new ItemAccessEnergyHandler(access, ModComponents.PACK_ENERGY.get(), capacity, transfer, transfer);
    }

    /**
     * An {@link ItemAccess} over the stack a placed pack's block entity holds. Commit
     * mutates that held stack in place (the wrapper restores the component patch onto the
     * original instance - verified in the 21.11.45 VanillaContainerWrapper sources), and
     * every successful operation marks the block entity dirty so the change persists.
     */
    public static ItemAccess forBlockEntity(com.sappersquad.packwork.block.PackContainerBlockEntity be) {
        ItemAccess delegate = ItemAccess.forStack(be.getPackStack());
        return new ItemAccess() {
            @Override
            public ItemResource getResource() {
                return delegate.getResource();
            }

            @Override
            public int getAmount() {
                return delegate.getAmount();
            }

            @Override
            public int insert(ItemResource resource, int amount, TransactionContext tx) {
                int moved = delegate.insert(resource, amount, tx);
                if (moved > 0) be.setChanged();
                return moved;
            }

            @Override
            public int extract(ItemResource resource, int amount, TransactionContext tx) {
                int moved = delegate.extract(resource, amount, tx);
                if (moved > 0) be.setChanged();
                return moved;
            }
        };
    }

    private PackTransfer() {}
}
