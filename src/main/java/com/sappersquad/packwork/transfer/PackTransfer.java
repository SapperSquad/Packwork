package com.sappersquad.packwork.transfer;

import com.sappersquad.packwork.block.PackContainerBlockEntity;
import com.sappersquad.packwork.pack.PackEnergyStorage;
import com.sappersquad.packwork.pack.PackFluidHandler;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.world.item.ItemStack;

/**
 * Packwork's transfer-face factories on Fabric: the Waterskin tank, the Charge
 * Crystal's Reborn Energy face, and the block-entity context. The ITEM store's handler
 * is {@code PackInventory} itself - the internals are native on the transfer API (one
 * handler, one set of rules, standard lookup and menu alike), mirroring the NeoForge
 * 26.x branch. (VERSION-SPECIFIC: this file's NeoForge sibling rides ItemAccess.)
 */
public final class PackTransfer {

    /** The Waterskin Rack's tank (tier-scaled capacity) over the {@code pack_fluid} component. */
    public static PackFluidHandler fluid(ContainerItemContext context, ItemStack packStack) {
        return new PackFluidHandler(context, PackFluidHandler.capacityFor(packStack));
    }

    /** The Charge Crystal's arcane charge (tier-scaled) over the {@code pack_energy} component. */
    public static PackEnergyFace energy(ContainerItemContext context, ItemStack packStack) {
        return new PackEnergyFace(context,
                PackEnergyStorage.capacityFor(packStack),
                PackEnergyStorage.transferFor(packStack));
    }

    /**
     * A {@link ContainerItemContext} over the stack a placed pack's block entity holds.
     * Commits mutate that held stack in place ({@link LiveStackStorage}), and every
     * committed transaction marks the block entity dirty so the change persists.
     */
    public static ContainerItemContext forBlockEntity(PackContainerBlockEntity be) {
        return ContainerItemContext.ofSingleSlot(
                new LiveStackStorage(be::getPackStack, be::setChanged));
    }

    /** Fixed-stack context: commits mutate the given stack's components in place. */
    public static ContainerItemContext forStack(ItemStack stack) {
        return ContainerItemContext.ofSingleSlot(new LiveStackStorage(stack));
    }

    private PackTransfer() {}
}
