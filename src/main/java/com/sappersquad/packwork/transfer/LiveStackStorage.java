package com.sappersquad.packwork.transfer;

import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * The Fabric stand-in for NeoForge's {@code ItemAccess.forStack}: a one-slot storage over a
 * LIVE {@link ItemStack} instance whose writes land IN PLACE on that very instance - count
 * and component patch - so a pack sitting in a player inventory, a block entity, or a
 * gametest local keeps its identity while its stores are worked. (Replacing the reference
 * would orphan the real stack; NeoForge's wrapper restores the patch onto the original
 * instance for exactly this reason, verified in the 21.11 sources during wave 2.)
 *
 * <p>Transaction-safe via {@link SingleStackStorage}'s snapshot journal: the snapshot is a
 * copy, and a rollback writes that copy back through the same in-place path.
 *
 * <p>The item itself never changes through this storage - the pack stays a pack. A write
 * that would swap the item is refused (left un-applied) rather than half-applied; only
 * emptying (count 0) and re-filling with the same item are meaningful here.
 */
public class LiveStackStorage extends SingleStackStorage {

    private final Supplier<ItemStack> live;
    private final Runnable onCommit;

    public LiveStackStorage(ItemStack stack) {
        this(() -> stack, () -> {});
    }

    /** Live-resolving form (e.g. a block entity's held stack) with a dirty hook on commit. */
    public LiveStackStorage(Supplier<ItemStack> live, Runnable onCommit) {
        this.live = live;
        this.onCommit = onCommit;
    }

    @Override
    protected ItemStack getStack() {
        return live.get();
    }

    @Override
    protected void setStack(ItemStack newStack) {
        ItemStack target = live.get();
        if (newStack == target) return;
        if (newStack.isEmpty()) {
            target.setCount(0);
            return;
        }
        // An empty stack masks its item as AIR, so resurface a zeroed instance's real item
        // BEFORE comparing (an exchange runs extract-to-zero then insert; the refill and a
        // rollback's snapshot restore both arrive while the instance reads empty).
        boolean wasEmpty = target.isEmpty();
        if (wasEmpty) target.setCount(newStack.getCount());
        if (target.getItem() != newStack.getItem()) {
            // a write that would swap the item - refuse rather than half-apply (the pack's
            // stores never exchange the pack itself; automation cannot reach this)
            if (wasEmpty) target.setCount(0);
            return;
        }
        // Mirror count + components onto the live instance. applyComponents is ADDITIVE,
        // so first strip any component the target carries that the new patch does not
        // mention - that makes the write a true REPLACE of the patch (a store cleared to
        // empty must not leave its stale component behind).
        DataComponentPatch patch = newStack.getComponentsPatch();
        java.util.Set<DataComponentType<?>> mentioned = new java.util.HashSet<>();
        for (var entry : patch.entrySet()) {
            mentioned.add(entry.getKey());
        }
        for (var entry : target.getComponentsPatch().entrySet()) {
            DataComponentType<?> type = entry.getKey();
            if (!mentioned.contains(type)) {
                target.remove(type);
            }
        }
        target.applyComponents(patch);
        target.setCount(newStack.getCount());
    }

    @Override
    protected void onFinalCommit() {
        onCommit.run();
    }
}
