package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ComponentItemHandler;

/**
 * The pack's item store: a {@link ComponentItemHandler} whose backing is the
 * {@code pack_contents} data component on the pack stack. One flat inventory -
 * tabs are virtual views computed over it, never physical partitions, so sorting
 * never moves an item and can never lose one.
 */
public class PackInventory extends ComponentItemHandler {

    public PackInventory(ItemStack packStack, PackTier tier) {
        super(packStack, ModComponents.PACK_CONTENTS.get(), tier.capacity());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // No pack-in-pack in v1 (see DECISIONS.md): blocks the dupe/lag surface.
        if (stack.getItem() instanceof PackItem) return false;
        return super.isItemValid(slot, stack);
    }
}
