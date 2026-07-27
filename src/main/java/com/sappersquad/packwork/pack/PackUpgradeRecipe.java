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
 *
 * <p><b>Count means SLOTS, not stack size.</b> Vanilla crafting consumes exactly one item
 * per occupied grid cell when the result is taken ({@code ResultSlot.onTake} shrinks each
 * slot by 1 - verified in the 1.21.1 sources), so the only honest way to charge four
 * leather is to require four leather-holding CELLS. Counting item totals instead let a
 * stack of four in one cell match while crafting consumed only one - a 75% discount.
 * This also makes the JEI layout literal: what it draws is exactly what you place.
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
            found[matched]++;              // one SLOT pays one material (see class doc)
        }
        if (packs != 1) return false;
        for (int m = 0; m < materials.size(); m++) {
            // exact, like every vanilla recipe: extra material cells would be silently eaten
            if (found[m] != materials.get(m).count()) return false;
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
        // the pack's cell plus one cell per material item (count = slots, see class doc)
        int cells = 1;
        for (SizedIngredient m : materials) cells += m.count();
        return width * height >= cells;
    }

    /**
     * The honest cell-by-cell ingredient list: the previous tier's pack, then one entry
     * per material CELL. This is not decorative - JEI's recipe scan
     * ({@code CategoryRecipeValidator.hasValidInputsAndOutputs}, verified in the
     * 19.21.1.312 sources) silently drops any non-special crafting recipe whose
     * ingredient list is empty ("Skipping Recipe because it has no inputs", DEBUG-only),
     * so with the default empty list the upgrade never reached our JEI extension and the
     * ladder showed info pages instead of crafts. It also matches how {@code matches()}
     * counts and how crafting consumes: one item per cell.
     *
     * <p>Deliberately harmless elsewhere: vanilla's recipe book only shows unlocked
     * recipes (we award none), and the pack's own Recipe Ledger will not list an
     * upgrade from pack stock because a pack cannot contain a pack (nesting is
     * blocked) - {@code StackedContents.canCraft} says no, and the all-or-nothing
     * lay-out can never cover the pack cell from stock. (A pack laid on the tool roll
     * by hand already crafted its upgrade before this change - the roll goes through
     * {@code matches()}, not this list.)
     */
    @Override
    public net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> getIngredients() {
        net.minecraft.core.NonNullList<net.minecraft.world.item.crafting.Ingredient> list =
                net.minecraft.core.NonNullList.create();
        list.add(net.minecraft.world.item.crafting.Ingredient.of(ModItems.pack(from).get()));
        for (SizedIngredient m : materials) {
            for (int i = 0; i < m.count(); i++) list.add(m.ingredient());
        }
        return list;
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
