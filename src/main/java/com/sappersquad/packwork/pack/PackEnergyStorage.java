package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.function.Supplier;

/**
 * The Charge Crystal's reservoir: a component-backed {@link IEnergyStorage} on the pack
 * (an arcane charge in a copper-wound crystal, never a "battery"). Standard capability,
 * so any mod's charger or conduit fills a placed pack, and equipped tools sip from it.
 */
public class PackEnergyStorage implements IEnergyStorage {

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

    /** Reservoir size for a pack: scales with the material tier. */
    public static int capacityFor(ItemStack pack) {
        PackTier tier = PackItem.tierOf(pack);
        return 100_000 * tier.step(); // Canvas 100k FE .. Sculkhide 600k FE
    }

    public static int transferFor(ItemStack pack) {
        return 2_000 * PackItem.tierOf(pack).step();
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

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int accepted = Math.min(capacity - get(), Math.min(toReceive, maxTransfer));
        if (accepted > 0 && !simulate) set(get() + accepted);
        return Math.max(0, accepted);
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = Math.min(get(), Math.min(toExtract, maxTransfer));
        if (extracted > 0 && !simulate) set(get() - extracted);
        return Math.max(0, extracted);
    }

    @Override
    public int getEnergyStored() {
        return get();
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
