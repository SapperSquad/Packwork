package com.sappersquad.packwork;

import com.sappersquad.packwork.pack.PackFluidHandler;
import com.sappersquad.packwork.pack.PackInventory;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.trinket.TrinketAccess;
import com.sappersquad.packwork.trinket.TrinketType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Exposes the pack's stores as standard NeoForge capabilities on the stack, so any
 * mod's automation works against a pack the same way it would any container/tank -
 * no Packwork API required (pillar 3). The item store is always present; the fluid
 * tank appears only when a Waterskin Rack is fitted (trinket-gated, pillar 2).
 */
public final class PackworkCapabilities {

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var holder : ModItems.PACKS.values()) {
            event.registerItem(Capabilities.ItemHandler.ITEM,
                    (stack, ctx) -> new PackInventory(stack, PackItem.tierOf(stack)),
                    holder.get());

            event.registerItem(Capabilities.FluidHandler.ITEM,
                    (stack, ctx) -> TrinketAccess.has(stack, TrinketType.WATERSKIN)
                            ? new PackFluidHandler(stack, PackFluidHandler.capacityFor(stack))
                            : null,
                    holder.get());
        }
    }

    private PackworkCapabilities() {}
}
