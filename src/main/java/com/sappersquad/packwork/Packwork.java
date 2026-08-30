package com.sappersquad.packwork;

import com.mojang.logging.LogUtils;
import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.reg.ModCreativeTabs;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.reg.ModMenus;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Packwork - a humble adventurer's pack that holds far more than it should and
 * quietly organizes itself. Sorting is the soul; resource stores come later,
 * each behind its own trinket. Leather and brass, never circuits.
 */
@Mod(Packwork.MODID)
public class Packwork {

    public static final String MODID = "packwork";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public Packwork(IEventBus modEventBus, ModContainer modContainer) {
        // The packmaker's lever: config/packwork-server.toml, read before anything else
        // consults it (same file, same keys on the Fabric build). The client cosmetics
        // file only exists on the client dist.
        com.sappersquad.packwork.config.PackworkConfig.loadServer(
                net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) {
            com.sappersquad.packwork.config.PackworkConfig.loadClient(
                    net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
        }

        // Components before items: the pack item's inventory capability leans on
        // its data component types being registered.
        ModComponents.COMPONENTS.register(modEventBus);
        com.sappersquad.packwork.reg.ModBlocks.BLOCKS.register(modEventBus);
        com.sappersquad.packwork.reg.ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        com.sappersquad.packwork.reg.ModRecipes.SERIALIZERS.register(modEventBus);
        // 1.21.5+ gametests are registry entries; the registrar scans @PackTest methods.
        com.sappersquad.packwork.gametest.PackworkTestRegistrar.TEST_FUNCTIONS.register(modEventBus);
        com.sappersquad.packwork.reg.ModConditions.CONDITIONS.register(modEventBus);
        com.sappersquad.packwork.reg.ModAttachments.ATTACHMENTS.register(modEventBus);

        modEventBus.addListener(PackworkCapabilities::registerCapabilities);
        modEventBus.addListener(PackworkNetwork::register);

        // Curios (optional): register the packs as back-slot curios during common setup.
        // Gated so compat/curios (the only class importing curios) never loads without it.
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) -> {
            if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
                e.enqueueWork(() -> com.sappersquad.packwork.compat.curios.CuriosCompat.register());
            }
        });

        LOGGER.info("Packwork slung over one shoulder.");
    }
}
