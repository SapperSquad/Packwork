package com.sappersquad.packwork.transfer;

import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.reg.ModComponents;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

/**
 * The Charge Crystal's automation face on Fabric: Team Reborn Energy (the ecosystem's
 * de facto standard, jar-in-jar'd per its own convention) over the same {@code
 * pack_energy} component the NeoForge branch stores FE in - component-identical, 1 E =
 * 1 FE, tier caps unchanged ({@link com.sappersquad.packwork.pack.PackEnergyStorage}
 * stays the plain component-math view the GUI and effects read).
 *
 * <p>Writes ride {@link ContainerItemContext#exchange} so every move is transactional
 * against the same host automation touches - the pattern Reborn's own
 * {@code SimpleEnergyItem} uses, with Packwork's component and per-tier transfer caps.
 */
public class PackEnergyFace implements EnergyStorage {

    private final ContainerItemContext context;
    private final int capacity;
    private final int maxTransfer;

    public PackEnergyFace(ContainerItemContext context, int capacity, int maxTransfer) {
        this.context = context;
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
    }

    private int stored() {
        ItemVariant host = context.getItemVariant();
        if (!(host.getItem() instanceof PackItem)) return 0;
        Integer v = host.toStack().get(ModComponents.PACK_ENERGY.get());
        return v == null ? 0 : v;
    }

    private boolean write(int value, TransactionContext tx) {
        ItemVariant host = context.getItemVariant();
        if (!(host.getItem() instanceof PackItem) || context.getAmount() < 1) return false;
        ItemStack carrier = host.toStack();
        carrier.set(ModComponents.PACK_ENERGY.get(), Math.max(0, Math.min(capacity, value)));
        return context.exchange(ItemVariant.of(carrier), 1, tx) == 1;
    }

    @Override
    public long insert(long maxAmount, TransactionContext tx) {
        int cur = stored();
        int accepted = (int) Math.min(Math.min(maxAmount, maxTransfer), capacity - cur);
        if (accepted <= 0) return 0;
        return write(cur + accepted, tx) ? accepted : 0;
    }

    @Override
    public long extract(long maxAmount, TransactionContext tx) {
        int cur = stored();
        int moved = (int) Math.min(Math.min(maxAmount, maxTransfer), cur);
        if (moved <= 0) return 0;
        return write(cur - moved, tx) ? moved : 0;
    }

    @Override
    public long getAmount() {
        return stored();
    }

    @Override
    public long getCapacity() {
        return capacity;
    }
}
