package com.sappersquad.packwork;

import com.sappersquad.packwork.block.PackContainerBlockEntity;
import com.sappersquad.packwork.pack.PackEnergyStorage;
import com.sappersquad.packwork.pack.PackFluidHandler;
import com.sappersquad.packwork.pack.PackInventory;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.reg.ModBlockEntities;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.trinket.TrinketAccess;
import com.sappersquad.packwork.trinket.TrinketType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;

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

            event.registerItem(Capabilities.EnergyStorage.ITEM,
                    (stack, ctx) -> TrinketAccess.has(stack, TrinketType.CHARGE_CRYSTAL)
                            ? new PackEnergyStorage(() -> stack,
                                    PackEnergyStorage.capacityFor(stack),
                                    PackEnergyStorage.transferFor(stack))
                            : null,
                    holder.get());
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
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PACK.get(),
                (be, side) -> new PackInventory(be::getPackStack, PackItem.tierOf(be.getPackStack())) {
                    @Override
                    protected void onContentsChanged(int slot, ItemStack oldStack, ItemStack newStack) {
                        be.setChanged();
                    }
                });

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.PACK.get(),
                (be, side) -> TrinketAccess.has(be.getPackStack(), TrinketType.WATERSKIN)
                        ? new PackFluidHandler(be.getPackStack(), PackFluidHandler.capacityFor(be.getPackStack())) {
                            @Override
                            protected void setFluid(FluidStack fluid) {
                                super.setFluid(fluid);
                                be.setChanged();
                            }

                            @Override
                            protected void setContainerToEmpty() {
                                super.setContainerToEmpty();
                                be.setChanged();
                            }
                        }
                        : null);

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.PACK.get(),
                (be, side) -> TrinketAccess.has(be.getPackStack(), TrinketType.CHARGE_CRYSTAL)
                        ? new PackEnergyStorage(be::getPackStack,
                                PackEnergyStorage.capacityFor(be.getPackStack()),
                                PackEnergyStorage.transferFor(be.getPackStack()), be::setChanged)
                        : null);

        // Forgework's Flux is its OWN block capability, not standard FE, so a Forgework cable
        // won't touch the standard energy cap above. Gated: only when Forgework is loaded do we
        // expose FLOW_ENERGY on the placed pack (one class touches com.forgework.*, never
        // classloaded without the mod).
        if (ModList.get().isLoaded("forgework")) {
            com.sappersquad.packwork.compat.forgework.ForgeworkFluxBridge.register(event, ModBlockEntities.PACK.get());
        }
    }

    private PackworkCapabilities() {}
}
