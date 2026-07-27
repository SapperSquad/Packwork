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
     * The pack worn in the back slot, or EMPTY. Used by the pack-first pickup routing so a
     * worn pack's Lodestone catches mined drops exactly like a pocketed one's. Only ever
     * called behind a {@code ModList.isLoaded("curios")} gate ({@code TrinketEffects},
     * the gametests). Takes any player so the gametests' mock players qualify.
     */
    public static ItemStack wornPack(net.minecraft.world.entity.player.Player player) {
        int idx = wornPackIndex(player);
        return idx < 0 ? ItemStack.EMPTY : wornStackAt(player, idx);
    }

    /** The back-slot index holding the first worn pack, or -1. */
    private static int wornPackIndex(net.minecraft.world.entity.player.Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.getStacksHandler("back"))
                .map(h -> {
                    for (int i = 0; i < h.getStacks().getSlots(); i++) {
                        if (h.getStacks().getStackInSlot(i).getItem()
                                instanceof com.sappersquad.packwork.pack.PackItem) {
                            return i;
                        }
                    }
                    return -1;
                }).orElse(-1);
    }

    /** The live stack in the given back-slot index, EMPTY when it is not a pack any more. */
    private static ItemStack wornStackAt(net.minecraft.world.entity.player.Player player, int idx) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.getStacksHandler("back"))
                .map(h -> {
                    if (idx >= h.getStacks().getSlots()) return ItemStack.EMPTY;
                    ItemStack s = h.getStacks().getStackInSlot(idx);
                    return s.getItem() instanceof com.sappersquad.packwork.pack.PackItem
                            ? s : ItemStack.EMPTY;
                }).orElse(ItemStack.EMPTY);
    }

    /**
     * The worn pack as a menu host, or null when no pack is worn. The getter re-resolves the
     * Curios inventory on EVERY access (never a captured stack or handler - a captured copy
     * goes stale and draws an empty grid; a captured handler outlives a slot-count rebuild),
     * and it collapses to EMPTY the moment the slot stops holding a pack, which is exactly
     * what flips the menu's stillValid and closes it without a dupe window. Writes land
     * in-place on the live equipped stack; Curios' own per-tick previous-vs-current diff
     * (DynamicStackHandler keeps previousStacks - verified against 9.5.1) picks the change
     * up for client sync, and the live stacks serialize with the player. Only ever called
     * behind a {@code ModList.isLoaded("curios")} gate.
     */
    public static com.sappersquad.packwork.pack.PackStackSlotContainer wornHost(
            net.minecraft.world.entity.player.Player player) {
        int idx = wornPackIndex(player);
        if (idx < 0) return null;
        return com.sappersquad.packwork.pack.PackStackSlotContainer.forWorn(
                () -> wornStackAt(player, idx),
                stack -> CuriosApi.getCuriosInventory(player)
                        .flatMap(inv -> inv.getStacksHandler("back"))
                        .ifPresent(h -> {
                            if (idx < h.getStacks().getSlots()) {
                                h.getStacks().setStackInSlot(idx, stack);
                            }
                        }));
    }

    /**
     * Open the organizer bound to the worn back-slot pack. True if one was worn and opened.
     * Called from the open-packet handler behind its {@code ModList.isLoaded("curios")} gate.
     */
    public static boolean openWornPack(ServerPlayer sp) {
        var host = wornHost(sp);
        if (host == null) return false;
        com.sappersquad.packwork.pack.PackItem.openWornPack(sp, host);
        return true;
    }

    /**
     * Test/dev helper: equip the given stack (NOT a copy - the caller may want to keep the
     * reference) in the first back slot. True if the player has one and it took.
     *
     * <p>A gametest mock player is constructed but never ADDED to the level, so the join
     * event Curios initializes its slot handlers on never fires - {@code reset()} performs
     * that same initialization from the loaded slot data, so the helper works for mock
     * players exactly as it does for real ones.
     */
    public static boolean equipWorn(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        var invOpt = CuriosApi.getCuriosInventory(player);
        if (invOpt.isEmpty()) return false;
        var inv = invOpt.get();
        var handler = inv.getStacksHandler("back").orElse(null);
        if (handler == null || handler.getStacks().getSlots() == 0) {
            inv.reset();
            handler = inv.getStacksHandler("back").orElse(null);
        }
        if (handler == null || handler.getStacks().getSlots() == 0) return false;
        handler.getStacks().setStackInSlot(0, stack);
        return true;
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
