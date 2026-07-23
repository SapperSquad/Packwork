package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.PackUpgradeRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Packwork.MODID);

    /** Preserving tier upgrade: keeps a pack's contents when it moves up the ladder. */
    public static final DeferredHolder<RecipeSerializer<?>, PackUpgradeRecipe.Serializer> PACK_UPGRADE =
            SERIALIZERS.register("pack_upgrade", PackUpgradeRecipe.Serializer::new);
}
