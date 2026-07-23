package com.sappersquad.packwork;

import com.mojang.logging.LogUtils;
import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.reg.ModCreativeTabs;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.reg.ModMenus;
import net.minecraft.resources.ResourceLocation;
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

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public Packwork(IEventBus modEventBus, ModContainer modContainer) {
        // Components before items: the pack item's inventory capability leans on
        // its data component types being registered.
        ModComponents.COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modEventBus.addListener(PackworkCapabilities::registerCapabilities);
        modEventBus.addListener(PackworkNetwork::register);

        LOGGER.info("Packwork slung over one shoulder.");
    }
}
