package com.sappersquad.packwork.pack;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.reg.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;

/**
 * THE way up the ladder: a pack of the {@code from} tier plus this tier's materials
 * becomes the {@code to} tier with every component - items, layout, trinkets, custom
 * name, and all five stores - carried over. Since 2026-07-25 (SapperSquad's call) this
 * preserving craft IS the visible recipe for every tier above Canvas; there are no
 * raw-material recipes that could sit beside it and confuse anyone into a craft that
 * eats a pack. No recipe in this mod ever consumes a filled pack's contents.
 *
 * <p>{@code materials} is a list of sized ingredients, so a tier may demand any number
 * of distinct reagents (the Dragonhide upgrade wants shulker shells AND dragon's
 * breath) without the recipe shape growing a field per reagent.
 */
public record PackUpgradeRecipe(PackTier from, PackTier to, List<SizedIngredient> materials,
                                CraftingBookCategory category) implements CraftingRecipe {

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int packs = 0;
        int[] found = new int[materials.size()];
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof PackItem p && p.tier() == from) {
                packs++;
                continue;
            }
            int matched = -1;
            for (int m = 0; m < materials.size(); m++) {
                if (materials.get(m).ingredient().test(s)) {
                    matched = m;
                    break;
                }
            }
            if (matched < 0) return false; // any unexpected item disqualifies the recipe
            found[matched] += s.getCount();
        }
        if (packs != 1) return false;
        for (int m = 0; m < materials.size(); m++) {
            if (found[m] < materials.get(m).count()) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack pack = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.getItem() instanceof PackItem p && p.tier() == from) {
                pack = s;
                break;
            }
        }
        ItemStack result = new ItemStack(ModItems.pack(to).get());
        copy(pack, result, ModComponents.PACK_CONTENTS.get());
        copy(pack, result, ModComponents.PACK_LAYOUT.get());
        copy(pack, result, ModComponents.PACK_TRINKETS.get());
        copy(pack, result, ModComponents.PACK_FLUID.get());
        copy(pack, result, ModComponents.PACK_XP.get());
        copy(pack, result, ModComponents.PACK_ENERGY.get());
        copy(pack, result, ModComponents.PACK_EMBERS.get());
        copy(pack, result, ModComponents.PACK_CHEMICAL.get());
        copy(pack, result, DataComponents.CUSTOM_NAME);
        return result;
    }

    private static <T> void copy(ItemStack src, ItemStack dst, DataComponentType<T> comp) {
        T v = src.get(comp);
        if (v != null) dst.set(comp, v);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ModItems.pack(to).get());
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.PACK_UPGRADE.get();
    }

    public static final class Serializer implements RecipeSerializer<PackUpgradeRecipe> {
        private static final MapCodec<PackUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                StringRepresentable.fromEnum(PackTier::values).fieldOf("from").forGetter(PackUpgradeRecipe::from),
                StringRepresentable.fromEnum(PackTier::values).fieldOf("to").forGetter(PackUpgradeRecipe::to),
                SizedIngredient.NESTED_CODEC.listOf().fieldOf("materials").forGetter(PackUpgradeRecipe::materials),
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.EQUIPMENT)
                        .forGetter(PackUpgradeRecipe::category)
        ).apply(inst, PackUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PackUpgradeRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.idMapper(i -> PackTier.values()[i], Enum::ordinal), PackUpgradeRecipe::from,
                        ByteBufCodecs.idMapper(i -> PackTier.values()[i], Enum::ordinal), PackUpgradeRecipe::to,
                        SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), PackUpgradeRecipe::materials,
                        CraftingBookCategory.STREAM_CODEC, PackUpgradeRecipe::category,
                        PackUpgradeRecipe::new);

        @Override
        public MapCodec<PackUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PackUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
