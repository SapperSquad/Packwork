package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.reg.ModComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Soul Vial's experience reservoir, stored as points on the pack. No vanilla
 * capability exists for XP, so this is Packwork-internal; siphon/pour move points
 * between a player and the vial, and the vault never voids - it stops at capacity.
 */
public final class PackXpStore {

    /** Vial capacity in points, scaling with the material tier. */
    public static int capacityFor(ItemStack pack) {
        PackTier tier = PackItem.tierOf(pack);
        return 5000 * tier.step(); // Canvas 5k .. Dragonhide 30k points
    }

    public static int stored(ItemStack pack) {
        return pack.getOrDefault(ModComponents.PACK_XP.get(), 0);
    }

    private static void setStored(ItemStack pack, int value) {
        pack.set(ModComponents.PACK_XP.get(), Math.max(0, value));
    }

    /** Pull as much of the player's XP into the vial as fits; returns points moved. */
    public static int siphon(ItemStack pack, Player player) {
        int room = capacityFor(pack) - stored(pack);
        if (room <= 0) return 0;
        int available = totalXp(player);
        int moved = Math.min(room, available);
        if (moved <= 0) return 0;
        player.giveExperiencePoints(-moved);
        setStored(pack, stored(pack) + moved);
        return moved;
    }

    /** Pour the vial back into the player; returns points moved. */
    public static int pour(ItemStack pack, Player player) {
        int have = stored(pack);
        if (have <= 0) return 0;
        player.giveExperiencePoints(have);
        setStored(pack, 0);
        return have;
    }

    /** Spend up to {@code max} points from the vial (e.g. for auto-mend); returns spent. */
    public static int spend(ItemStack pack, int max) {
        int have = stored(pack);
        int spent = Math.min(have, Math.max(0, max));
        if (spent > 0) setStored(pack, have - spent);
        return spent;
    }

    /** The player's current spendable XP total, via the vanilla level curve. */
    public static int totalXp(Player p) {
        int total = 0;
        for (int i = 0; i < p.experienceLevel; i++) total += xpForLevelUp(i);
        total += Math.round(p.experienceProgress * p.getXpNeededForNextLevel());
        return total;
    }

    /** Points needed to go from {@code level} to {@code level+1} (vanilla formula). */
    private static int xpForLevelUp(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }

    private PackXpStore() {}
}
