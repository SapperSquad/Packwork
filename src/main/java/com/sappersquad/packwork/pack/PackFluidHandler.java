package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

/**
 * The Waterskin Rack's tank: a single component-backed fluid tank on the pack stack,
 * built on NeoForge's own {@link FluidHandlerItemStack} so any mod's fluid automation
 * works against a placed pack with no Packwork API. Framed as a waterskin, not a tank.
 */
public class PackFluidHandler extends FluidHandlerItemStack {

    public PackFluidHandler(ItemStack pack, int capacity) {
        super(ModComponents.PACK_FLUID, pack, capacity);
    }

    /** Tank size for a pack: scales with the material tier. */
    public static int capacityFor(ItemStack pack) {
        PackTier tier = PackItem.tierOf(pack);
        return 8000 * tier.step(); // Canvas 8 buckets ... Dragonhide 48 buckets
    }
}
