package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * The Waterskin Rack's tank: a single component-backed fluid tank on the pack stack,
 * native on Fabric's transfer API so any mod's fluid automation works against a pack
 * with no Packwork API. Framed as a waterskin, not a tank.
 *
 * <p><b>Units:</b> the {@code pack_fluid} component stores MILLIBUCKETS (1000/bucket -
 * loader-neutral, and what the gauge, tier capacities, and store copy all speak), while
 * Fabric's transfer face speaks DROPLETS (81000/bucket). The conversion lives here and
 * only here: 81 droplets per mB, and the native face only ever moves whole millibuckets
 * so no droplet can be stranded or minted by rounding. Bucket-sized moves are exact
 * (81000 droplets = 1000 mB).
 *
 * <p>The stack-shaped {@code fill}/{@code drain} conveniences keep the internal callers
 * and the conservation gametests on their battle-tested vocabulary, each one running a
 * root transaction over the same native path automation uses.
 */
public class PackFluidHandler implements SingleSlotStorage<FluidVariant> {

    /** Droplets per millibucket: {@code FluidConstants.BUCKET / 1000} = 81. */
    public static final long DROPLETS_PER_MB = net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET / 1000;

    private final ContainerItemContext context;
    private final int capacityMb;

    /** Fixed-stack form: commits mutate the given pack stack's components in place. */
    public PackFluidHandler(ItemStack pack, int capacityMb) {
        this(ContainerItemContext.ofSingleSlot(
                new com.sappersquad.packwork.transfer.LiveStackStorage(pack)), capacityMb);
    }

    public PackFluidHandler(ContainerItemContext context, int capacityMb) {
        this.context = context;
        this.capacityMb = capacityMb;
    }

    /** Tank size for a pack, in mB: scales with the material tier. */
    public static int capacityFor(ItemStack pack) {
        PackTier tier = PackItem.tierOf(pack);
        return 8000 * tier.step(); // Canvas 8 buckets ... Sculkhide 48 buckets
    }

    private PackFluidContent content() {
        ItemVariant host = context.getItemVariant();
        if (!(host.getItem() instanceof PackItem)) return PackFluidContent.EMPTY;
        PackFluidContent c = host.toStack().get(ModComponents.PACK_FLUID.get());
        return c == null ? PackFluidContent.EMPTY : c;
    }

    private boolean write(PackFluidContent updated, TransactionContext tx) {
        ItemVariant host = context.getItemVariant();
        if (!(host.getItem() instanceof PackItem) || context.getAmount() < 1) return false;
        ItemStack carrier = host.toStack();
        if (updated.isEmpty()) {
            carrier.remove(ModComponents.PACK_FLUID.get());
        } else {
            carrier.set(ModComponents.PACK_FLUID.get(), updated);
        }
        return context.exchange(ItemVariant.of(carrier), 1, tx) == 1;
    }

    // ---- the native SingleSlotStorage<FluidVariant> surface (DROPLETS) ----

    @Override
    public boolean isResourceBlank() {
        return content().isEmpty();
    }

    @Override
    public FluidVariant getResource() {
        PackFluidContent c = content();
        return c.isEmpty() ? FluidVariant.blank() : FluidVariant.of(c.fluid(), c.components());
    }

    @Override
    public long getAmount() {
        return content().getAmount() * DROPLETS_PER_MB;
    }

    @Override
    public long getCapacity() {
        return (long) capacityMb * DROPLETS_PER_MB;
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext tx) {
        if (resource.isBlank() || maxAmount <= 0) return 0;
        PackFluidContent cur = content();
        if (!cur.isEmpty()
                && !(cur.fluid() == resource.getFluid()
                        && resource.componentsMatch(cur.components()))) {
            return 0; // one fluid at a time
        }
        int roomMb = capacityMb - cur.getAmount();
        int movedMb = (int) Math.min(maxAmount / DROPLETS_PER_MB, roomMb); // whole mB only
        if (movedMb <= 0) return 0;
        PackFluidContent updated = new PackFluidContent(resource.getFluid(),
                cur.getAmount() + movedMb, resource.getComponentsPatch());
        return write(updated, tx) ? movedMb * DROPLETS_PER_MB : 0;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext tx) {
        if (resource.isBlank() || maxAmount <= 0) return 0;
        PackFluidContent cur = content();
        if (cur.isEmpty() || cur.fluid() != resource.getFluid()
                || !resource.componentsMatch(cur.components())) {
            return 0;
        }
        int movedMb = (int) Math.min(maxAmount / DROPLETS_PER_MB, cur.getAmount()); // whole mB only
        if (movedMb <= 0) return 0;
        return write(cur.withAmount(cur.getAmount() - movedMb), tx) ? movedMb * DROPLETS_PER_MB : 0;
    }

    // ---- stack-shaped conveniences in mB (simulate = don't commit) ----

    /** Fill with the given fluid; returns the amount accepted, in mB. */
    public int fill(Fluid fluid, int amountMb, boolean simulate) {
        if (amountMb <= 0) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            long moved = insert(FluidVariant.of(fluid), (long) amountMb * DROPLETS_PER_MB, tx);
            if (!simulate && moved > 0) tx.commit();
            return (int) (moved / DROPLETS_PER_MB);
        }
    }

    /** Drain up to maxDrain mB of whatever the tank holds; returns what came out. */
    public PackFluidContent drain(int maxDrainMb, boolean simulate) {
        FluidVariant r = getResource();
        if (r.isBlank() || maxDrainMb <= 0) return PackFluidContent.EMPTY;
        try (Transaction tx = Transaction.openOuter()) {
            long moved = extract(r, (long) maxDrainMb * DROPLETS_PER_MB, tx);
            if (!simulate && moved > 0) tx.commit();
            return moved <= 0 ? PackFluidContent.EMPTY
                    : new PackFluidContent(r.getFluid(), (int) (moved / DROPLETS_PER_MB), r.getComponentsPatch());
        }
    }

    /** What the tank currently holds (mB view). */
    public PackFluidContent getFluidInTank(int tank) {
        return content();
    }
}
