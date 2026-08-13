package com.sappersquad.packwork.compat.trinkets;

import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.trinket.TrinketEffects;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The ONLY class in Packwork allowed to import {@code eu.pb4.trinkets.*} (Trinkets
 * Updated - the maintained fork that carries the Fabric wear-slot standard on 26.x; it
 * {@code provides} the classic {@code trinkets} id, which is what every gate checks).
 * Reached strictly behind a {@code FabricLoader.isModLoaded("trinkets")} gate, so it
 * never classloads without the mod installed.
 *
 * <p>The packs live in the built-in <b>chest/back</b> slot (assigned by the
 * {@code trinkets:chest/back} item tag this mod ships). While worn, the pack's active
 * trinkets keep running - the worn tick funnels straight back into
 * {@link TrinketEffects#applyWornPack}, so magnet / restock / repair / soul-vial /
 * charge behave exactly as they do in a pocket. The native inventory use + the B
 * keybind remain the fallback when Trinkets is absent. (This class mirrors the
 * NeoForge branches' CuriosCompat surface method for method.)
 */
public final class TrinketsCompat {

    /** The built-in Trinkets slot the packs ride: group "chest", slot "back". */
    private static final String GROUP = "chest";
    private static final String SLOT = "back";
    private static final String SLOT_KEY = GROUP + "/" + SLOT;

    private TrinketsCompat() {}

    public static void register() {
        TrinketCallback callback = new TrinketCallback() {
            @Override
            public void tick(ItemStack stack, TrinketSlotAccess access, LivingEntity entity) {
                if (entity instanceof ServerPlayer sp) {
                    TrinketEffects.applyWornPack(sp, stack);
                }
            }

            @Override
            public boolean canEquip(ItemStack stack, TrinketSlotAccess access, LivingEntity entity) {
                return true;
            }
        };
        ModItems.PACKS.values().forEach(holder -> TrinketCallback.setCallback(holder.get(), callback));
    }

    /**
     * The pack worn in the back slot, or EMPTY. Used by the pack-first pickup routing so a
     * worn pack's Lodestone catches mined drops exactly like a pocketed one's. Only ever
     * called behind a {@code isModLoaded("trinkets")} gate ({@code TrinketEffects}, the
     * gametests). Takes any player so the gametests' mock players qualify.
     */
    public static ItemStack wornPack(net.minecraft.world.entity.player.Player player) {
        TrinketSlotAccess access = wornPackAccess(player);
        return access == null ? ItemStack.EMPTY : access.get();
    }

    /** The live slot access holding the first worn pack, or null. */
    private static TrinketSlotAccess wornPackAccess(net.minecraft.world.entity.player.Player player) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(player);
        if (attachment == null) return null;
        return attachment.findFirst(
                s -> s.getItem() instanceof com.sappersquad.packwork.pack.PackItem).orElse(null);
    }

    /**
     * The worn pack as a menu host, or null when no pack is worn. The accessors re-resolve
     * the Trinkets attachment on EVERY access (never a captured stack or handler - a
     * captured copy goes stale and draws an empty grid), and they collapse to EMPTY the
     * moment the slot stops holding a pack, which is exactly what flips the menu's
     * stillValid and closes it without a dupe window. Writes go through the slot's own
     * {@code set}, so Trinkets' sync and persistence see every change. Only ever called
     * behind a {@code isModLoaded("trinkets")} gate.
     */
    public static com.sappersquad.packwork.pack.PackStackSlotContainer wornHost(
            net.minecraft.world.entity.player.Player player) {
        TrinketSlotAccess first = wornPackAccess(player);
        if (first == null) return null;
        final String slotName = first.slotType().getId();
        final int index = first.index();
        return com.sappersquad.packwork.pack.PackStackSlotContainer.forWorn(
                () -> {
                    TrinketSlotAccess access = accessAt(player, slotName, index);
                    if (access == null) return ItemStack.EMPTY;
                    ItemStack s = access.get();
                    return s.getItem() instanceof com.sappersquad.packwork.pack.PackItem ? s : ItemStack.EMPTY;
                },
                stack -> {
                    TrinketSlotAccess access = accessAt(player, slotName, index);
                    if (access != null) access.set(stack);
                });
    }

    private static TrinketSlotAccess accessAt(net.minecraft.world.entity.player.Player player,
                                              String slotName, int index) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(player);
        if (attachment == null) return null;
        TrinketSlotAccess access = attachment.getSlotAccess(slotName, index);
        if (access == null || !access.isValid()) {
            // key shape fallback: some paths key by bare slot name rather than group/name
            int cut = slotName.lastIndexOf('/');
            if (cut >= 0) access = attachment.getSlotAccess(slotName.substring(cut + 1), index);
        }
        return access != null && access.isValid() ? access : null;
    }

    /**
     * Open the organizer bound to the worn back-slot pack. True if one was worn and opened.
     * Called from the open-packet handler behind its {@code isModLoaded("trinkets")} gate.
     */
    public static boolean openWornPack(ServerPlayer sp) {
        var host = wornHost(sp);
        if (host == null) return false;
        com.sappersquad.packwork.pack.PackItem.openWornPack(sp, host);
        return true;
    }

    /** Test helper: is the chest/back slot registered for players in this level? */
    public static boolean backSlotRegistered(net.minecraft.world.level.Level level) {
        var groups = TrinketsApi.getPlayerSlots(level);
        return groups.containsKey(GROUP) && groups.get(GROUP).slots().containsKey(SLOT);
    }

    /**
     * Test/dev helper: equip the given stack (NOT a copy - the caller may want to keep the
     * reference) in the first back slot. True if the player has one and it took.
     */
    public static boolean equipWorn(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(player);
        if (attachment == null) return false;
        TrinketSlotAccess access = attachment.getSlotAccess(SLOT_KEY, 0);
        if (access == null || !access.isValid()) {
            // some builds key the inventory map by slot name alone - try both shapes
            access = attachment.getSlotAccess(SLOT, 0);
        }
        if (access == null || !access.isValid()) return false;
        return access.set(stack);
    }

    /**
     * Dev harness only: prove the back slot is wired - the player has it and it accepts
     * the pack - then equip one there. Logged for the screenshot run.
     */
    public static void devEquip(ServerPlayer sp, ItemStack pack) {
        boolean hasBack = TrinketsApi.getPlayerSlots(sp).containsKey(GROUP)
                && TrinketsApi.getPlayerSlots(sp).get(GROUP).slots().containsKey(SLOT);
        com.sappersquad.packwork.Packwork.LOGGER.info(
                "[trinkets] player has a chest/back slot: {}", hasBack);
        boolean took = equipWorn(sp, pack.copy());
        com.sappersquad.packwork.Packwork.LOGGER.info("[trinkets] equipped in back slot -> {}",
                took ? wornPack(sp).getHoverName().getString() : "FAILED");
    }
}
