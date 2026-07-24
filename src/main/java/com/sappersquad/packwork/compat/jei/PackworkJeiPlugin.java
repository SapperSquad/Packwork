package com.sappersquad.packwork.compat.jei;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.trinket.TrinketType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The ONLY class in Packwork allowed to import {@code mezz.jei.*}. It is never referenced by
 * mod code - JEI discovers it by the {@link JeiPlugin} annotation and loads it only when JEI
 * itself is present, so it can't classload without the mod (the same self-gating every JEI
 * plugin relies on; no ModList check needed).
 *
 * <p>Surfaces an in-JEI "info" page for every pack tier, every trinket, and the handbook,
 * so a player can look up what each does from the recipe screen. The tier numbers come
 * straight from {@link PackTier}, and the trinket blurbs reuse their tooltip lang keys.
 */
@JeiPlugin
public class PackworkJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Packwork.id("jei");
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
