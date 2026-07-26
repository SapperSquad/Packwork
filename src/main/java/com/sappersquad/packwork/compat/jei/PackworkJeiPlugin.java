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
import net.neoforged.neoforge.common.crafting.SizedIngredient;

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
 *   crafting recipe, and without help JEI has nothing to draw for it - which left "how do I
 *   make each pack" showing lore instead of a craft. The {@link PackUpgradeExtension} below
 *   teaches JEI's own crafting category the layout: previous-tier pack + its material cells
 *   in, next-tier pack out, with the contents-preserving behaviour spelled out on the
 *   result's tooltip. Canvas and every trinket/handbook recipe are plain vanilla recipe
 *   JSONs, which JEI renders with no help.</li>
 *   <li><b>Info pages as supplements</b> for every pack tier, every trinket, and the
 *   handbook. The tier numbers come straight from {@link PackTier}, and the trinket blurbs
 *   reuse their tooltip lang keys.</li>
 * </ul>
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
     * way it is placed at a bench: the previous tier's pack plus one cell per material item
     * (a material's {@code count} is a number of CELLS - crafting consumes one item per
     * occupied cell, and {@link PackUpgradeRecipe#matches} demands the same). Laid out
     * shapeless, because the real recipe accepts the cells in any arrangement.
     */
    private static class PackUpgradeExtension implements ICraftingCategoryExtension<PackUpgradeRecipe> {
        @Override
        public void setRecipe(RecipeHolder<PackUpgradeRecipe> holder, IRecipeLayoutBuilder builder,
                              ICraftingGridHelper helper, IFocusGroup focuses) {
            PackUpgradeRecipe r = holder.value();
            List<List<ItemStack>> inputs = new ArrayList<>();
            inputs.add(List.of(new ItemStack(ModItems.pack(r.from()).get())));
            for (SizedIngredient m : r.materials()) {
                List<ItemStack> options = Arrays.asList(m.ingredient().getItems());
                for (int i = 0; i < m.count(); i++) inputs.add(options);
            }
            helper.createAndSetInputs(builder, inputs, 0, 0); // 0x0 = shapeless 3x3 fill
            helper.createAndSetOutputs(builder, List.of(new ItemStack(ModItems.pack(r.to()).get())))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                            Component.translatable("packwork.jei.upgrade.preserves")
                                    .withStyle(ChatFormatting.DARK_GREEN)));
        }
        // getWidth/getHeight keep their 0 defaults, so JEI shows its shapeless marker.
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        Component packInfo = Component.translatable("packwork.jei.pack.info");
        for (PackTier tier : PackTier.values()) {
            reg.addItemStackInfo(new ItemStack(ModItems.pack(tier).get()),
                    packInfo,
                    Component.translatable("packwork.jei.pack.tier", tier.capacity(), tier.trinketSlots()));
        }
        for (TrinketType type : TrinketType.values()) {
            reg.addItemStackInfo(new ItemStack(ModItems.trinket(type).get()),
                    Component.translatable("packwork.jei.trinket.header"),
                    type.description());
        }
        reg.addItemStackInfo(new ItemStack(ModItems.HANDBOOK.get()),
                Component.translatable("packwork.jei.handbook.info"));
    }
}
