package com.sappersquad.packwork.client;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.block.PackContainerBlockEntity;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.reg.ModBlocks;
import com.sappersquad.packwork.reg.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Packwork.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    // Per-tier tint MULTIPLIERS over the near-neutral leather block texture. The base is
    // roughly grey, so these push each tier to a distinct hue: pale canvas, warm brown,
    // dark brown, cool steel, dusk violet.
    private static final int[] TIER_TINT = {
            0xFFF3E6, // CANVAS     - pale off-white canvas
            0xC28A54, // LEATHER    - warm tan brown
            0x8A6440, // STUDDED    - dark worked leather
            0x8FA0BE, // REINFORCED - cool steel-grey, iron-studded
            0x9A6ED8  // RUNED      - dusk violet, arcane
    };

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PACK.get(), PackScreen::new);
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return -1;
            PackTier tier = PackTier.LEATHER;
            if (level != null && pos != null
                    && level.getBlockEntity(pos) instanceof PackContainerBlockEntity be) {
                tier = be.getTier();
            }
            return TIER_TINT[tier.ordinal()];
        }, ModBlocks.PACK.get());
    }
}
