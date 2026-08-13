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

    static {
        // Fabric's client recipe sync is OPT-IN per serializer (recipes stopped syncing to
        // clients in 1.21.2; fabric-api's recipe-sync restores it for serializers that ask).
        // Without this the upgrades never reach the client's synced RecipeMap - which is
        // exactly where JEI's crafting index reads from, so the pack ladder rendered as
        // info pages but never as REAL recipes (the same failure wave 4 fixed on NeoForge,
        // wearing its Fabric clothes; found via the autoshot's index probe: 0 indexed).
        net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization
                .synchronizeRecipeSerializer(PACK_UPGRADE.get());
    }

    /** Touch to classload + register everything above (called once from mod init). */
    public static void init() {}
}
