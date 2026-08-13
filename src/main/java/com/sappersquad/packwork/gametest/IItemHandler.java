package com.sappersquad.packwork.gametest;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

/**
 * TEST-ONLY legacy-shaped view over a Fabric {@code SlottedStorage<ItemVariant>}, named
 * after NeoForge's interface so the 57 test bodies read word-for-word across every
 * branch. The tests still PROVE the real standard lookup is exposed (pillar 3) - they
 * query {@code ItemStorage.ITEM}/{@code SIDED} and drive whatever comes back through
 * this view's root transactions, the same path any automation mod takes.
 */
public interface IItemHandler {

    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    static IItemHandler of(SlottedStorage<ItemVariant> storage) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return storage.getSlotCount();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                SingleSlotStorage<ItemVariant> s = storage.getSlot(slot);
                return s.isResourceBlank() ? ItemStack.EMPTY : s.getResource().toStack((int) s.getAmount());
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (stack.isEmpty()) return ItemStack.EMPTY;
                try (Transaction tx = Transaction.openOuter()) {
                    long moved = storage.getSlot(slot).insert(ItemVariant.of(stack), stack.getCount(), tx);
                    if (!simulate && moved > 0) tx.commit();
                    return moved >= stack.getCount() ? ItemStack.EMPTY
                            : stack.copyWithCount(stack.getCount() - (int) moved);
                }
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                SingleSlotStorage<ItemVariant> s = storage.getSlot(slot);
                if (s.isResourceBlank() || amount <= 0) return ItemStack.EMPTY;
                ItemVariant r = s.getResource();
                try (Transaction tx = Transaction.openOuter()) {
                    long moved = s.extract(r, amount, tx);
                    if (!simulate && moved > 0) tx.commit();
                    return moved <= 0 ? ItemStack.EMPTY : r.toStack((int) moved);
                }
            }
        };
    }
}
