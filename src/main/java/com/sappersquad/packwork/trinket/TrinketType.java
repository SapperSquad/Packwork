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
    FEATHER("feather_charm", Kind.PASSIVE),
    COMPASS_ROSE("compass_rose", Kind.PASSIVE),
    RESTOCK("restock_strap", Kind.TICK),
    BOTTOMLESS("bottomless_lining", Kind.PASSIVE),
    REPAIR("repair_kit", Kind.TICK),
    QUICK_DRAW("quick_draw_straps", Kind.PASSIVE),
    QUILL_LEDGER("quill_and_ledger", Kind.PASSIVE);

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
