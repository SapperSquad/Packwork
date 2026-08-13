package com.sappersquad.packwork;

import com.sappersquad.packwork.block.PackContainerBlockEntity;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.reg.ModBlockEntities;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.transfer.PackTransfer;
import com.sappersquad.packwork.trinket.TrinketAccess;
import com.sappersquad.packwork.trinket.TrinketType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;

/**
 * Exposes the pack's stores as standard NeoForge capabilities on the stack, so any
 * mod's automation works against a pack the same way it would any container/tank -
 * no Packwork API required (pillar 3). The item store is always present; the fluid
 * tank appears only when a Waterskin Rack is fitted (trinket-gated, pillar 2).
 *
 * <p><b>21.9+ transfer rework:</b> the standard tokens are {@code Capabilities.Item /
 * Fluid / Energy} now (transactional {@code ResourceHandler}s with an {@link ItemAccess}
 * context on items); the handlers live in {@link PackTransfer}, one file per NeoForge
 * drift surface.
 */
public final class PackworkCapabilities {

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var holder : ModItems.PACKS.values()) {
            // 26.1: PackInventory IS the native handler now - the capability and the
            // menu/trinket internals share one implementation of the three rules.
            event.registerItem(Capabilities.Item.ITEM,
                    (stack, access) -> new com.sappersquad.packwork.pack.PackInventory(access, PackItem.tierOf(stack)),
                    holder.get());

            event.registerItem(Capabilities.Fluid.ITEM,
                    (stack, access) -> TrinketAccess.has(stack, TrinketType.WATERSKIN)
                            ? PackTransfer.fluid(access, stack)
                            : null,
                    holder.get());

            event.registerItem(Capabilities.Energy.ITEM,
                    (stack, access) -> TrinketAccess.has(stack, TrinketType.CHARGE_CRYSTAL)
                            ? PackTransfer.energy(access, stack)
                            : null,
                    holder.get());
        }

        // Mekanism gas store (optional, gated): the Flask Harness tank exposed via Mekanism's
        // own chemical capability on each pack item. One class touches mekanism.*, never
        // classloaded without the mod.
        if (ModList.get().isLoaded("mekanism")) {
            for (var holder : ModItems.PACKS.values()) {
                com.sappersquad.packwork.compat.mekanism.MekanismChemicalStore.registerItem(event, holder);
            }
        }

        registerBlockCaps(event);
    }

    /**
     * The same stores on a PLACED pack, exposed through NeoForge's block capabilities so
     * any mod's hoppers, pipes, and cables interact with it. Because sorting is virtual over
     * one flat store, an item a hopper pushes in just auto-routes into the right compartment.
     * Each store is gated by its trinket, and every write marks the block entity dirty so it
     * persists. Standard FE is exposed here; Forgework's own Flux cap is added (gated) below.
     */
    private static void registerBlockCaps(RegisterCapabilitiesEvent event) {
        // The BE-backed ItemAccess mutates the held pack stack in place on commit and
        // marks the block entity dirty on every successful move (see PackTransfer).
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.PACK.get(),
                (be, side) -> be.isEmpty() ? null
                        : new com.sappersquad.packwork.pack.PackInventory(PackTransfer.forBlockEntity(be),
                                PackItem.tierOf(be.getPackStack())));

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.PACK.get(),
                (be, side) -> !be.isEmpty() && TrinketAccess.has(be.getPackStack(), TrinketType.WATERSKIN)
                        ? PackTransfer.fluid(PackTransfer.forBlockEntity(be), be.getPackStack())
                        : null);

        event.registerBlockEntity(Capabilities.Energy.BLOCK, ModBlockEntities.PACK.get(),
                (be, side) -> !be.isEmpty() && TrinketAccess.has(be.getPackStack(), TrinketType.CHARGE_CRYSTAL)
                        ? PackTransfer.energy(PackTransfer.forBlockEntity(be), be.getPackStack())
                        : null);

        // Forgework's Flux is its OWN block capability, not standard FE, so a Forgework cable
        // won't touch the standard energy cap above. Gated: only when Forgework is loaded do we
        // expose FLOW_ENERGY on the placed pack (one class touches com.forgework.*, never
        // classloaded without the mod).
        if (ModList.get().isLoaded("forgework")) {
            com.sappersquad.packwork.compat.forgework.ForgeworkFluxBridge.register(event, ModBlockEntities.PACK.get());
        }

        // Mekanism chemical cap on the placed pack (gated).
        if (ModList.get().isLoaded("mekanism")) {
            com.sappersquad.packwork.compat.mekanism.MekanismChemicalStore.registerBlock(event, ModBlockEntities.PACK.get());
        }
    }

    private PackworkCapabilities() {}
}
