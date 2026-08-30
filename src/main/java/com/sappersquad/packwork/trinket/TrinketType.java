package com.sappersquad.packwork.trinket;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * The single source of truth for pack trinkets - the adventurer-flavoured fittings
 * a player crafts and slots into a pack to unlock a capability. Adding a trinket is
 * one entry here plus its texture and behaviour; {@code ModItems} registers an item
 * per entry off this enum, and {@link com.sappersquad.packwork.trinket.TrinketEffects}
 * dispatches behaviour on the same ids.
 *
 * <p>Effect kind tells the framework how to run it: {@link Kind#PASSIVE} trinkets are
 * simply "installed or not" (checked where they matter); {@link Kind#TICK} trinkets do
 * something each server tick while the player carries the pack; {@link Kind#STORE}
 * trinkets gate a resource store (Phase 3) and add gauges.
 */
public enum TrinketType {
    LODESTONE("lodestone_charm", Kind.TICK),
    COMPASS_ROSE("compass_rose", Kind.PASSIVE),
    RESTOCK("restock_strap", Kind.TICK),
    BOTTOMLESS("bottomless_lining", Kind.PASSIVE),
    REPAIR("repair_kit", Kind.TICK),
    QUICK_DRAW("quick_draw_straps", Kind.PASSIVE),
    QUILL_LEDGER("quill_and_ledger", Kind.PASSIVE),
    /** A leather tool roll that unrolls across the open pack: a 3x3 grid fed from pack stock. */
    TINKERS_KIT("tinkers_kit", Kind.PASSIVE),
    /** Banked campfire embers: slowly cooks raw ore and raw food in the pack, burning pack fuel. */
    FIELD_FURNACE("field_furnace", Kind.TICK),
    /** Eats from pack stock when you're going hungry - the cheapest thing first. */
    PROVISIONER("provisioners_pouch", Kind.TICK),
    /** A waxed sleeve of charts: adds a Charts compartment for maps, compasses and clocks. */
    CARTOGRAPHER("cartographers_sleeve", Kind.PASSIVE),
    /** A wicker creel: your catch lands in the pack, in its own Catch compartment. */
    ANGLERS_CREEL("anglers_creel", Kind.PASSIVE),
    /** A loop of torches: lights the dark around you from pack stock. */
    TORCHBEARER("torchbearers_loop", Kind.TICK),
    /** A bundle of seed pouches: replants what you harvest, from your own stock. */
    HERBALIST("herbalists_bundle", Kind.PASSIVE),
    /** A brass bleed-off cock on the pack's seam: carries only so much of a marked item and
     *  lets the surplus run out. Reads the SAME discard list the Compass Rose does - the
     *  Rose bins a listed item at the door, the Valve gives it a keep level instead. */
    OVERFLOW_VALVE("overflow_valve", Kind.TICK),
    /** A screw press in the pack's floor: squeezes nine into one, and only ever things that
     *  squeeze back out again. */
    COMPACTING_PRESS("compacting_press", Kind.TICK),
    // resource-store fittings (Phase 3)
    WATERSKIN("waterskin_rack", Kind.STORE),
    SOUL_VIAL("soul_vial", Kind.STORE),
    CHARGE_CRYSTAL("charge_crystal", Kind.STORE),
    FLASK_HARNESS("flask_harness", Kind.STORE);

    public enum Kind { PASSIVE, TICK, STORE }

    private final String id;
    private final Kind kind;

    TrinketType(String id, Kind kind) {
        this.id = id;
        this.kind = kind;
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    public MutableComponent displayName() {
        return Component.translatable("item.packwork." + id);
    }

    public MutableComponent description() {
        return Component.translatable("packwork.trinket." + id + ".desc");
    }

    public static TrinketType of(ItemStack stack) {
        return stack.getItem() instanceof TrinketItem t ? t.type() : null;
    }
}
