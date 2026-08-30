package com.sappersquad.packwork.compat.jei;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.client.PackScreen;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.pack.PackUpgradeRecipe;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.trinket.TrinketType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The ONLY class in Packwork allowed to import {@code mezz.jei.*}. It is never referenced by
 * mod code - JEI discovers it by the {@link JeiPlugin} annotation and loads it only when JEI
 * itself is present, so it can't classload without the mod (the same self-gating every JEI
 * plugin relies on; no ModList check needed).
 *
 * <p>Two jobs:
 * <ul>
 *   <li><b>Real recipes for the ladder.</b> {@code packwork:pack_upgrade} is a custom
 *   crafting recipe; TWO pieces make it render. (1) The recipe carries an honest
 *   {@code getIngredients()} list - JEI's own scan ({@code VanillaRecipes} +
 *   {@code CategoryRecipeValidator}, verified in the 19.21.1.312 sources) silently DROPS
 *   any non-special crafting recipe with an empty ingredient list before any extension is
 *   consulted, which is exactly the "info pages but no recipes" failure seen in the field.
 *   (2) The {@link PackUpgradeExtension} below teaches JEI's crafting category the layout:
 *   previous-tier pack + its material cells in, next-tier pack out, with the
 *   contents-preserving behaviour spelled out on the result's tooltip. Canvas and every
 *   trinket/handbook recipe are plain vanilla recipe JSONs, which JEI renders with no
 *   help.</li>
 *   <li><b>Info pages as supplements</b> for every pack tier, every trinket, and the
 *   handbook. The tier numbers come straight from {@link PackTier}, and the trinket blurbs
 *   reuse their tooltip lang keys.</li>
 * </ul>
 *
 * <p>{@link #registerRecipes} logs one greppable INFO line ("Packwork JEI: ...") so plugin
 * discovery and the upgrade-recipe count are provable from any log, no pixels needed.
 */
@JeiPlugin
public class PackworkJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Packwork.id("jei");
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration reg) {
        reg.getCraftingCategory().addExtension(PackUpgradeRecipe.class, new PackUpgradeExtension());
    }

    /**
     * Tell JEI where the pack GUI really ends: the tab rail, the fittings rail with its
     * gauges, and whichever parchment sheet (Recipe Ledger / rule editor) is open all hang
     * beyond {@code imageWidth}, and without this JEI's ingredient list would draw right
     * over them.
     */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration reg) {
        reg.addGuiContainerHandler(PackScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(PackScreen screen) {
                return screen.jeiExtraAreas();
            }
        });
    }

    // ---- dev-harness hook (autoshot only; see DevAutoShot, which reaches this by reflection
    // so nothing outside compat/jei ever references a JEI type) ----

    private static IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(IJeiRuntime rt) {
        runtime = rt;
    }

    /** Open JEI's recipe view on the Studded pack - the autoshot proof that the ladder renders. */
    public static void devShowUpgradeRecipes() {
        IJeiRuntime rt = runtime;
        if (rt == null) {
            Packwork.LOGGER.warn("[autoshot][jei] runtime not available yet");
            return;
        }
        rt.getRecipesGui().show(rt.getJeiHelpers().getFocusFactory().createFocus(
                RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK,
                new ItemStack(ModItems.pack(PackTier.STUDDED).get())));
    }

    /**
     * Draws a {@code packwork:pack_upgrade} in JEI's standard crafting category exactly the
     * way it is placed at a bench: the previous tier's pack in the CENTER, the tier's bulk
     * material on the four edges, its fittings on the four corners - the same full-ring
     * picture {@link PackUpgradeRecipe#matches} demands (all nine cells, positions matter,
     * rotations free). Drawn shaped (3x3), so no shapeless marker.
     */
    private static class PackUpgradeExtension implements ICraftingCategoryExtension<PackUpgradeRecipe> {
        @Override
        public void setRecipe(RecipeHolder<PackUpgradeRecipe> holder, IRecipeLayoutBuilder builder,
                              ICraftingGridHelper helper, IFocusGroup focuses) {
            PackUpgradeRecipe r = holder.value();
            List<ItemStack> pack = List.of(new ItemStack(ModItems.pack(r.from()).get()));
            List<ItemStack> edge = Arrays.asList(r.edges().getItems());
            List<ItemStack> corner = Arrays.asList(r.corners().getItems());
            List<List<ItemStack>> inputs = new ArrayList<>(9);
            for (int cell = 0; cell < 9; cell++) {
                if (cell == PackUpgradeRecipe.CENTER_CELL) inputs.add(pack);
                else inputs.add(cell % 2 == 1 ? edge : corner);
            }
            helper.createAndSetInputs(builder, inputs, 3, 3); // positioned: the ring as drawn
            helper.createAndSetOutputs(builder, List.of(new ItemStack(ModItems.pack(r.to()).get())))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                            Component.translatable("packwork.jei.upgrade.preserves")
                                    .withStyle(ChatFormatting.DARK_GREEN)));
        }

        @Override
        public int getWidth(RecipeHolder<PackUpgradeRecipe> holder) {
            return 3; // shaped: the ring has positions, so no shapeless marker
        }

        @Override
        public int getHeight(RecipeHolder<PackUpgradeRecipe> holder) {
            return 3;
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        // The discovery proof: one greppable line. Counts the upgrade recipes exactly as
        // JEI's own scan sees them (the client RecipeManager), so a 0 here means the
        // ladder will NOT render and points straight at recipe loading.
        long upgrades = 0;
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (level != null) {
            upgrades = level.getRecipeManager()
                    .getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)
                    .stream().filter(h -> h.value() instanceof PackUpgradeRecipe).count();
        }
        Packwork.LOGGER.info(
                "Packwork JEI: plugin discovered; {} pack-upgrade recipes carry ingredients for the "
                        + "crafting category; info pages for {} packs + {} trinkets",
                upgrades, PackTier.values().length, TrinketType.values().length);

        Component packInfo = Component.translatable("packwork.jei.pack.info");
        for (PackTier tier : PackTier.values()) {
            reg.addItemStackInfo(new ItemStack(ModItems.pack(tier).get()),
                    packInfo,
                    Component.translatable("packwork.jei.pack.tier", tier.capacity(), tier.trinketSlots()));
        }
        for (TrinketType type : TrinketType.values()) {
            // A config-retired fitting keeps no info page either - its recipe is already
            // condition-pulled, so JEI shows nothing of it at all.
            if (!com.sappersquad.packwork.config.PackworkConfig.get().enabled(type)) continue;
            reg.addItemStackInfo(new ItemStack(ModItems.trinket(type).get()),
                    Component.translatable("packwork.jei.trinket.header"),
                    type.description());
        }
        reg.addItemStackInfo(new ItemStack(ModItems.HANDBOOK.get()),
                Component.translatable("packwork.jei.handbook.info"));
    }
}
