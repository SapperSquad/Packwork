package com.sappersquad.packwork.trinket;

import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.reg.ModComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.EnumSet;

/**
 * Reads which trinkets a pack has installed (from the {@code pack_trinkets} component)
 * and derives trinket-dependent numbers like effective capacity. One place so every
 * caller agrees on what "installed" means.
 */
public final class TrinketAccess {

    /** Extra backing slots a Bottomless Lining adds (capped by the 256-slot component max). */
    public static final int BOTTOMLESS_BONUS = 54;

    public static EnumSet<TrinketType> installed(ItemStack pack) {
        EnumSet<TrinketType> set = EnumSet.noneOf(TrinketType.class);
        ItemContainerContents c = pack.get(ModComponents.PACK_TRINKETS.get());
        if (c != null) {
            c.nonEmptyItemCopyStream().forEach(s -> {
                TrinketType t = TrinketType.of(s);
                if (t != null) set.add(t);
            });
        }
        return set;
    }

    public static boolean has(ItemStack pack, TrinketType type) {
        ItemContainerContents c = pack.get(ModComponents.PACK_TRINKETS.get());
        if (c == null) return false;
        return c.nonEmptyItemCopyStream().anyMatch(s -> TrinketType.of(s) == type);
    }

    /** Backing-slot count for this pack: tier capacity, grown by a Bottomless Lining. */
    public static int capacity(ItemStack pack) {
        PackTier tier = PackItem.tierOf(pack);
        int cap = tier.capacity();
        if (has(pack, TrinketType.BOTTOMLESS)) cap += BOTTOMLESS_BONUS;
        return Math.min(256, cap);
    }

    private TrinketAccess() {}
}
