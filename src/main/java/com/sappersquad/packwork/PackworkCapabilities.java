package com.sappersquad.packwork;

import com.sappersquad.packwork.pack.PackInventory;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.reg.ModBlockEntities;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.transfer.PackTransfer;
import com.sappersquad.packwork.trinket.TrinketAccess;
import com.sappersquad.packwork.trinket.TrinketType;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.world.level.ItemLike;
import team.reborn.energy.api.EnergyStorage;

/**
 * Exposes the pack's stores through Fabric's standard API lookups, so any mod's
 * automation works against a pack the same way it would any container/tank - no
 * Packwork API required (pillar 3). The item store is always present; the fluid tank
 * appears only when a Waterskin Rack is fitted, the energy face only with a Charge
 * Crystal (trinket-gated, pillar 2). Energy speaks Team Reborn Energy - the Fabric
 * ecosystem's standard, jar-in-jar'd so it is never a mod the player installs.
 *
 * <p>Fabric API's own mixins route vanilla hoppers (and every transfer-API mod's
 * pipes) through these same lookups, so a placed pack stays hopper-automatable
 * exactly as it is on the NeoForge branches.
 */
public final class PackworkCapabilities {

    public static void register() {
        ItemLike[] packLikes = ModItems.PACKS.values().stream().map(h -> (ItemLike) h.get()).toArray(ItemLike[]::new);

        // ---- the pack ITEM, wherever automation can reach it as a stack ----

        ItemStorage.ITEM.registerForItems(
                (stack, context) -> new PackInventory(context, PackItem.tierOf(stack)).storage(),
                packLikes);

        FluidStorage.ITEM.registerForItems(
                (stack, context) -> TrinketAccess.has(stack, TrinketType.WATERSKIN)
                        ? PackTransfer.fluid(context, stack)
                        : null,
                packLikes);

        EnergyStorage.ITEM.registerForItems(
                (stack, context) -> TrinketAccess.has(stack, TrinketType.CHARGE_CRYSTAL)
                        ? PackTransfer.energy(context, stack)
                        : null,
                packLikes);

        // ---- the same stores on a PLACED pack, via the block lookups ----
        // Because sorting is virtual over one flat store, an item a hopper pushes in just
        // auto-routes into the right compartment. Each store is gated by its trinket, and
        // every committed write marks the block entity dirty (see PackTransfer.forBlockEntity).

        ItemStorage.SIDED.registerForBlockEntity(
                (be, side) -> be.isEmpty() ? null
                        : new PackInventory(PackTransfer.forBlockEntity(be), PackItem.tierOf(be.getPackStack())).storage(),
                ModBlockEntities.PACK.get());

        FluidStorage.SIDED.registerForBlockEntity(
                (be, side) -> !be.isEmpty() && TrinketAccess.has(be.getPackStack(), TrinketType.WATERSKIN)
                        ? PackTransfer.fluid(PackTransfer.forBlockEntity(be), be.getPackStack())
                        : null,
                ModBlockEntities.PACK.get());

        EnergyStorage.SIDED.registerForBlockEntity(
                (be, side) -> !be.isEmpty() && TrinketAccess.has(be.getPackStack(), TrinketType.CHARGE_CRYSTAL)
                        ? PackTransfer.energy(PackTransfer.forBlockEntity(be), be.getPackStack())
                        : null,
                ModBlockEntities.PACK.get());

        // Mekanism (gas) and Forgework (Flux) are NeoForge-only mods: no Fabric artifact
        // exists, so those gates have nothing to light here and their compat classes do
        // not exist on this branch. The pack_chemical component still loads/saves so no
        // vapors are ever voided (pause, never punish).
    }

    private PackworkCapabilities() {}
}
