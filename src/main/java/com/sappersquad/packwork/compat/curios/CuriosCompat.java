package com.sappersquad.packwork.compat.curios;

import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.trinket.TrinketEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * The ONLY class in Packwork allowed to import {@code top.theillusivec4.curios.*}. Reached
 * strictly behind a {@code ModList.isLoaded("curios")} gate (in the mod's common-setup
 * listener), so it never classloads without Curios installed.
 *
 * <p>Registers every pack tier as a Curios curio so it can be worn in the <b>back</b> slot
 * (assigned by the {@code curios:back} item tag). While worn, the pack's active trinkets keep
 * running - the worn tick funnels straight back into {@link TrinketEffects#applyWornPack}, so
 * magnet / restock / repair / soul-vial / charge behave exactly as they do in a pocket. The
 * native inventory use + the B keybind remain the fallback when Curios is absent.
 */
public final class CuriosCompat {

    private CuriosCompat() {}

    public static void register() {
        ICurioItem curio = new ICurioItem() {
            @Override
            public void curioTick(SlotContext ctx, ItemStack stack) {
                if (ctx.entity() instanceof ServerPlayer sp) {
                    TrinketEffects.applyWornPack(sp, stack);
                }
            }

            @Override
            public boolean canEquip(SlotContext ctx, ItemStack stack) {
                return true;
            }
        };
        ModItems.PACKS.values().forEach(holder -> CuriosApi.registerCurio(holder.get(), curio));
    }

    /**
     * Dev harness only: prove the back slot is wired - the player has it, the pack is assigned
     * to it, and it accepts the pack - then equip one there. Logged for the screenshot run.
     */
    public static void devEquip(net.minecraft.server.level.ServerPlayer sp, ItemStack pack) {
        boolean hasBack = CuriosApi.getPlayerSlots(sp).containsKey("back");
        boolean fitsBack = CuriosApi.getItemStackSlots(pack, sp).containsKey("back");
        com.sappersquad.packwork.Packwork.LOGGER.info(
                "[curios] player has a back slot: {}; the pack is assigned to it: {}", hasBack, fitsBack);
        CuriosApi.getCuriosInventory(sp).flatMap(inv -> inv.getStacksHandler("back")).ifPresent(h -> {
            if (h.getStacks().getSlots() > 0) {
                h.getStacks().setStackInSlot(0, pack.copy());
                com.sappersquad.packwork.Packwork.LOGGER.info("[curios] equipped in back slot -> {}",
                        h.getStacks().getStackInSlot(0).getHoverName().getString());
            }
        });
    }
}
