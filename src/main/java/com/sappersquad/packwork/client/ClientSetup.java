package com.sappersquad.packwork.client;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.reg.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Packwork.MODID, value = Dist.CLIENT)
public class ClientSetup {

    // No block colour handler: the placed pack now uses per-tier baked textures/models keyed off
    // the `tier` blockstate property (studs/plates/runes carry into the world), so there is no
    // neutral leather tile to multiply a tint over.

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PACK.get(), PackScreen::new);
    }

}
