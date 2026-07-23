package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.trinket.TrinketAccess;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * The pack's item store: a component-backed handler over the {@code pack_contents}
 * component. One flat inventory - tabs are virtual views computed over it, never
 * physical partitions, so sorting never moves an item and can never lose one.
 *
 * <p>Its slot count is live: a Bottomless Lining trinket grows it. Slots beyond the
 * current capacity are never truncated on write, so removing Bottomless hides the
 * extra items rather than voiding them.
 */
public class PackInventory extends LiveComponentHandler {

    /** Fixed-stack form for capability providers on a standalone/placed pack. */
    public PackInventory(ItemStack packStack, PackTier tier) {
        this(() -> packStack, tier);
    }

    /** Live form for the menu: always reads whichever stack currently sits in the bound slot. */
    public PackInventory(Supplier<ItemStack> live, PackTier tier) {
        super(live, ModComponents.PACK_CONTENTS.get(), 256);
    }

    @Override
    public int getSlots() {
        return TrinketAccess.capacity(live.get());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // No pack-in-pack in v1 (see DECISIONS.md): blocks the dupe/lag surface.
        if (stack.getItem() instanceof PackItem) return false;
        return super.isItemValid(slot, stack);
    }
}
