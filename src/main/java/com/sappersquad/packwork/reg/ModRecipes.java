package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.PackUpgradeRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {

    /** Preserving tier upgrade: keeps a pack's contents when it moves up the ladder.
     *  (26.1: {@code RecipeSerializer} is a plain record of codecs now, not an interface.) */
    public static final RegHandle<RecipeSerializer<PackUpgradeRecipe>> PACK_UPGRADE =
            new RegHandle<>(Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                    Packwork.id("pack_upgrade"), PackUpgradeRecipe.createSerializer()));

    /** Touch to classload + register everything above (called once from mod init). */
    public static void init() {}
}
