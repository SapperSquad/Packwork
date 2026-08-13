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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * THE way up the ladder, drawn as a picture: the previous tier's pack sits in the CENTER
 * of the bench, and the new tier is built as a full RING around it - the tier's hide or
 * plating on the four EDGES, its fittings on the four CORNERS. All nine cells filled,
 * always (Alex's call, 2026-07-26: no gaps, pack centered). The result carries every
 * component - items, layout, trinkets, custom name, and all five stores - so no recipe in
 * this mod ever consumes a filled pack's contents.
 *
 * <p><b>Positions matter; the picture IS the recipe.</b> Edges and corners are not
 * interchangeable - iron plates go on the edges, diamonds on the corners, and a swapped
 * ring does not match. Rotations and mirrors are all accepted for free, because every
 * edge cell asks for the same thing and every corner cell does too.
 *
 * <p><b>Consumption is honest by construction.</b> Vanilla crafting consumes exactly one
 * item per occupied cell when the result is taken ({@code ResultSlot.onTake} shrinks each
 * slot by 1 - verified in the 1.21.1 sources), and this recipe demands all nine cells
 * occupied, so the full price is always paid; payment can never be concentrated into a
 * stacked cell (the old summed-count matcher allowed exactly that underpay).
 *
 * <p><b>1.21.11 port:</b> {@code getIngredients()} (the old JEI-validator hook) is gone
 * from the Recipe interface; its honest row-major role is carried by BOTH successors -
 * {@link #placementInfo()} (the same nine cells, so nothing reports this recipe as
 * unplaceable-special) and {@link #display()} (a real positioned
 * {@code ShapedCraftingRecipeDisplay}, which is what recipe viewers consume now).
 */
public record PackUpgradeRecipe(PackTier from, PackTier to, Ingredient edges, Ingredient corners,
                                CraftingBookCategory category) implements CraftingRecipe {

    /** Row-major 3x3 cell roles: the centre is the pack; odd cells are edges, the rest corners. */
    public static final int CENTER_CELL = 4;

    /** 26.1: {@code group()} became abstract on Recipe; upgrades belong to no book group. */
    @Override
    public String group() {
        return "";
    }

    /** 26.1: {@code showNotification()} went abstract too; keep vanilla's usual yes. */
    @Override
    public boolean showNotification() {
        return true;
    }

    private static boolean isEdgeCell(int cell) {
        return cell % 2 == 1;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        // CraftingInput trims empty rows/columns, so a full ring is the only way to be 3x3.
        if (input.width() != 3 || input.height() != 3) return false;
        for (int cell = 0; cell < 9; cell++) {
            ItemStack s = input.getItem(cell);
            if (cell == CENTER_CELL) {
                if (!(s.getItem() instanceof PackItem p) || p.tier() != from) return false;
            } else if (isEdgeCell(cell)) {
                if (!edges.test(s)) return false;
            } else {
                if (!corners.test(s)) return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack pack = input.getItem(CENTER_CELL);
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

    /**
     * The honest row-major 3x3: corner, edge, corner / edge, PACK, edge / corner, edge,
     * corner. Exactly what {@link #matches} accepts and what the display draws.
     *
     * <p>Deliberately harmless elsewhere: vanilla's recipe book only shows unlocked
     * recipes (we award none), and the pack's own Recipe Ledger will not list an upgrade
     * from pack stock because a pack cannot contain a pack (nesting is blocked) - the
     * stacked-contents check says no, and the all-or-nothing lay-out can never cover the
     * pack cell from stock.
     */
    public java.util.List<Ingredient> ringCells() {
        java.util.List<Ingredient> list = new java.util.ArrayList<>(9);
        Ingredient pack = Ingredient.of(ModItems.pack(from).get());
        for (int cell = 0; cell < 9; cell++) {
            if (cell == CENTER_CELL) list.add(pack);
            else if (isEdgeCell(cell)) list.add(edges);
            else list.add(corners);
        }
        return list;
    }

    /** The bare next-tier pack (display only - {@link #assemble} carries the components). */
    public ItemStack resultStack() {
        return new ItemStack(ModItems.pack(to).get());
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(ringCells());
    }

    @Override
    public java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        return java.util.List.of(new net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay(
                3, 3,
                ringCells().stream()
                        .map(Ingredient::display)
                        .map(d -> (net.minecraft.world.item.crafting.display.SlotDisplay) d)
                        .toList(),
                new net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay(
                        net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(resultStack())),
                new net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay(
                        net.minecraft.world.item.Items.CRAFTING_TABLE.builtInRegistryHolder())));
    }

    @Override
    public RecipeSerializer<PackUpgradeRecipe> getSerializer() {
        return ModRecipes.PACK_UPGRADE.get();
    }

    /** 26.1: {@code RecipeSerializer} became a record of (codec, streamCodec); this builds ours. */
    public static RecipeSerializer<PackUpgradeRecipe> createSerializer() {
        return new RecipeSerializer<>(Serializer.CODEC, Serializer.STREAM_CODEC);
    }

    private static final class Serializer {
        private static final MapCodec<PackUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                StringRepresentable.fromEnum(PackTier::values).fieldOf("from").forGetter(PackUpgradeRecipe::from),
                StringRepresentable.fromEnum(PackTier::values).fieldOf("to").forGetter(PackUpgradeRecipe::to),
                Ingredient.CODEC.fieldOf("edges").forGetter(PackUpgradeRecipe::edges),
                Ingredient.CODEC.fieldOf("corners").forGetter(PackUpgradeRecipe::corners),
                CraftingBookCategory.CODEC.optionalFieldOf("category", CraftingBookCategory.EQUIPMENT)
                        .forGetter(PackUpgradeRecipe::category)
        ).apply(inst, PackUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, PackUpgradeRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.idMapper(i -> PackTier.values()[i], Enum::ordinal), PackUpgradeRecipe::from,
                        ByteBufCodecs.idMapper(i -> PackTier.values()[i], Enum::ordinal), PackUpgradeRecipe::to,
                        Ingredient.CONTENTS_STREAM_CODEC, PackUpgradeRecipe::edges,
                        Ingredient.CONTENTS_STREAM_CODEC, PackUpgradeRecipe::corners,
                        CraftingBookCategory.STREAM_CODEC, PackUpgradeRecipe::category,
                        PackUpgradeRecipe::new);
    }
}
