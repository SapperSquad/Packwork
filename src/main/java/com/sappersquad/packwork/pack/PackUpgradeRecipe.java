package com.sappersquad.packwork.pack;

import com.mojang.serialization.Codec;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Upgrades a filled pack one tier <em>without</em> a plain craft eating its contents:
 * a pack of the {@code from} tier plus its upgrade material becomes the {@code to}
 * tier with every component (items, layout, trinkets, custom name) carried over.
 * This is why the plain tier recipes never consume a lower pack.
 */
public record PackUpgradeRecipe(PackTier from, PackTier to, Ingredient material, int count,
                                CraftingBookCategory category) implements CraftingRecipe {

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int packs = 0, mats = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof PackItem p && p.tier() == from) {
                packs++;
            } else if (material.test(s)) {
                mats += s.getCount();
            } else {
                return false; // any unexpected item disqualifies the recipe
            }
        }
        return packs == 1 && mats >= count;
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
                Ingredient.CODEC.fieldOf("material").forGetter(PackUpgradeRecipe::material),
                Codec.INT.fieldOf("count").forGetter(PackUpgradeRecipe::count),
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.EQUIPMENT)
                        .forGetter(PackUpgradeRecipe::category)
        ).apply(inst, PackUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PackUpgradeRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.idMapper(i -> PackTier.values()[i], Enum::ordinal), PackUpgradeRecipe::from,
                        ByteBufCodecs.idMapper(i -> PackTier.values()[i], Enum::ordinal), PackUpgradeRecipe::to,
                        Ingredient.CONTENTS_STREAM_CODEC, PackUpgradeRecipe::material,
                        ByteBufCodecs.VAR_INT, PackUpgradeRecipe::count,
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
