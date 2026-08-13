package com.sappersquad.packwork;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/**
 * Packwork - a humble adventurer's pack that holds far more than it should and
 * quietly organizes itself. Sorting is the soul; resource stores come later,
 * each behind its own trinket. Leather and brass, never circuits.
 */
public class Packwork implements ModInitializer {

    public static final String MODID = "packwork";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        // Components before items: the pack item's storage lookups lean on its data
        // component types being registered. (Fabric: registration is eager - each reg
        // class registers in its static init; init() just forces the classload, in order.)
        com.sappersquad.packwork.reg.ModComponents.init();
        com.sappersquad.packwork.reg.ModBlocks.init();
        com.sappersquad.packwork.reg.ModBlockEntities.init();
        com.sappersquad.packwork.reg.ModItems.init();
        com.sappersquad.packwork.reg.ModMenus.init();
        com.sappersquad.packwork.reg.ModCreativeTabs.init();
        com.sappersquad.packwork.reg.ModRecipes.init();
        // 1.21.5+ gametests are registry entries; the registrar scans @PackTest methods.
        com.sappersquad.packwork.gametest.PackworkTestRegistrar.register();

        PackworkCapabilities.register();
        PackworkNetwork.register();
        com.sappersquad.packwork.trinket.TrinketEffects.register();

        // Trinkets (optional): hook the packs into the chest/back wear slot. Gated so
        // compat/trinkets (the only class importing trinkets) never loads without it.
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            com.sappersquad.packwork.compat.trinkets.TrinketsCompat.register();
        }

        LOGGER.info("Packwork slung over one shoulder.");
    }
}
