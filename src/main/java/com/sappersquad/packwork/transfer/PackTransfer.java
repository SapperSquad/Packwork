package com.sappersquad.packwork.transfer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Packwork's non-item stores on the NeoForge transfer API, plus the block-entity
 * {@link ItemAccess}. The ITEM store's handler is {@code PackInventory} itself now -
 * since the 26.1 port the internals are native on the transfer API (one handler, one
 * set of rules, capability and menu alike), so the old capability-facing wrapper that
 * lived here is gone. (VERSION-SPECIFIC: this file does not exist on the 1.21.1 branch.)
 */
public final class PackTransfer {

    /** The Waterskin Rack's tank (tier-scaled capacity) over the {@code pack_fluid} component. */
    public static com.sappersquad.packwork.pack.PackFluidHandler fluid(ItemAccess access, ItemStack packStack) {
        return new com.sappersquad.packwork.pack.PackFluidHandler(access,
                com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(packStack));
    }

    /** The Charge Crystal's arcane charge (tier-scaled) over the {@code pack_energy} component. */
    public static ItemAccessEnergyHandler energy(ItemAccess access, ItemStack packStack) {
        int capacity = com.sappersquad.packwork.pack.PackEnergyStorage.capacityFor(packStack);
        int transfer = com.sappersquad.packwork.pack.PackEnergyStorage.transferFor(packStack);
        return new ItemAccessEnergyHandler(access, com.sappersquad.packwork.reg.ModComponents.PACK_ENERGY.get(),
                capacity, transfer, transfer);
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
