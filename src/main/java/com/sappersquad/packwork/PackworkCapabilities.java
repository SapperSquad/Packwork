package com.sappersquad.packwork;

import com.sappersquad.packwork.pack.PackInventory;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.reg.ModItems;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Exposes the pack's item store as a standard NeoForge item-handler capability on
 * the stack, so any mod's automation can read/insert against a pack the same way
 * it would any container - no Packwork API required (pillar 3).
 */
public final class PackworkCapabilities {

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var holder : ModItems.PACKS.values()) {
            event.registerItem(Capabilities.ItemHandler.ITEM,
                    (stack, ctx) -> new PackInventory(stack, PackItem.tierOf(stack)),
                    holder.get());
        }
    }

    private PackworkCapabilities() {}
}
