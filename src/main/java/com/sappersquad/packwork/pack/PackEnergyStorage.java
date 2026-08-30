package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * The Charge Crystal's reservoir: a component-backed store on the pack (an arcane
 * charge in a copper-wound crystal, never a "battery"). The standard capability is the
 * transfer-API handler in {@code PackTransfer.energy}; this class is the plain
 * component-math view the internals and the (gated, 1.21.1-era) Forgework bridge ride.
 * 26.1: it no longer implements the deprecated-for-removal {@code IEnergyStorage} -
 * the receive/extract vocabulary stays, the NeoForge interface goes.
 */
public class PackEnergyStorage {

    private final Supplier<ItemStack> live;
    private final int capacity;
    private final int maxTransfer;
    private final Runnable onChange;

    public PackEnergyStorage(Supplier<ItemStack> live, int capacity, int maxTransfer) {
        this(live, capacity, maxTransfer, () -> {});
    }

    /** {@code onChange} lets a placed pack mark its block entity dirty when a cable charges it. */
    public PackEnergyStorage(Supplier<ItemStack> live, int capacity, int maxTransfer, Runnable onChange) {
        this.live = live;
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
        this.onChange = onChange;
    }

    /** Reservoir size for a pack: per-tier, packmaker-tunable ({@code tiers.<name>.energy_fe}). */
    public static int capacityFor(ItemStack pack) {
        return com.sappersquad.packwork.config.PackworkConfig.get()
                .energyFeFor(PackItem.tierOf(pack)); // default: Canvas 100k FE .. Sculkhide 600k
    }

    /** Transfer rate rides the capacity (capacity/50 per op - 2,000 FE at the Canvas default). */
    public static int transferFor(ItemStack pack) {
        return Math.max(1, capacityFor(pack) / 50);
    }

    private int get() {
        return live.get().getOrDefault(ModComponents.PACK_ENERGY.get(), 0);
    }

    private void set(int v) {
        ItemStack s = live.get();
        if (!s.isEmpty()) {
            s.set(ModComponents.PACK_ENERGY.get(), Math.max(0, Math.min(capacity, v)));
            onChange.run();
        }
    }

    public int receiveEnergy(int toReceive, boolean simulate) {
        int accepted = Math.min(capacity - get(), Math.min(toReceive, maxTransfer));
        if (accepted > 0 && !simulate) set(get() + accepted);
        return Math.max(0, accepted);
    }

    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = Math.min(get(), Math.min(toExtract, maxTransfer));
        if (extracted > 0 && !simulate) set(get() - extracted);
        return Math.max(0, extracted);
    }

    public int getEnergyStored() {
        return get();
    }

    public int getMaxEnergyStored() {
        return capacity;
    }

    public boolean canExtract() {
        return true;
    }

    public boolean canReceive() {
        return true;
    }
}
