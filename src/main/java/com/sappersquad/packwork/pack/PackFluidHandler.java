package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.ItemAccessFluidHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Waterskin Rack's tank: a single component-backed fluid tank on the pack stack,
 * native on NeoForge's transfer API ({@link ItemAccessFluidHandler}) so any mod's fluid
 * automation works against a pack with no Packwork API. Framed as a waterskin, not a
 * tank. (26.x: the legacy {@code FluidHandlerItemStack} base is deprecated-for-removal;
 * the stack-shaped {@code fill}/{@code drain} conveniences below keep the internal
 * callers and the conservation gametests on their battle-tested vocabulary, each one
 * running a root transaction over the same native path automation uses.)
 */
public class PackFluidHandler extends ItemAccessFluidHandler {

    /** Fixed-stack form: commits mutate the given pack stack's components in place. */
    public PackFluidHandler(ItemStack pack, int capacity) {
        this(ItemAccess.forStack(pack), capacity);
    }

    public PackFluidHandler(ItemAccess access, int capacity) {
        super(access, ModComponents.PACK_FLUID.get(), capacity);
    }

    /** Tank size for a pack: scales with the material tier. */
    public static int capacityFor(ItemStack pack) {
        PackTier tier = PackItem.tierOf(pack);
        return 8000 * tier.step(); // Canvas 8 buckets ... Sculkhide 48 buckets
    }

    // ---- stack-shaped conveniences (simulate = don't commit) ----

    /** Fill from the given stack; returns the amount accepted. */
    public int fill(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = insert(0, FluidResource.of(resource), resource.getAmount(), tx);
            if (!simulate && moved > 0) tx.commit();
            return moved;
        }
    }

    /** Drain up to maxDrain of whatever the tank holds; returns what came out. */
    public FluidStack drain(int maxDrain, boolean simulate) {
        FluidResource r = getResource(0);
        if (r.isEmpty() || maxDrain <= 0) return FluidStack.EMPTY;
        try (Transaction tx = Transaction.openRoot()) {
            int moved = extract(0, r, maxDrain, tx);
            if (!simulate && moved > 0) tx.commit();
            return moved <= 0 ? FluidStack.EMPTY : r.toStack(moved);
        }
    }

    /** What the tank currently holds (a copy). */
    public FluidStack getFluidInTank(int tank) {
        FluidResource r = getResource(0);
        return r.isEmpty() ? FluidStack.EMPTY : r.toStack(getAmountAsInt(0));
    }

    // Legacy-action overloads so the conservation gametests keep their exact wording
    // (FluidAction still ships in 26.1, deprecated; drop these when NeoForge drops it).
    @SuppressWarnings("removal")
    public int fill(FluidStack resource, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
        return fill(resource, action.simulate());
    }

    @SuppressWarnings("removal")
    public FluidStack drain(int maxDrain, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
        return drain(maxDrain, action.simulate());
    }
}
