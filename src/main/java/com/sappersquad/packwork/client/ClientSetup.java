package com.sappersquad.packwork.client;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.reg.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Packwork.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    // No block colour handler: the placed pack now uses per-tier baked textures/models keyed off
    // the `tier` blockstate property (studs/plates/runes carry into the world), so there is no
    // neutral leather tile to multiply a tint over.

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PACK.get(), PackScreen::new);
    }

    /**
     * The worn pack renders on the player's back (see {@link WornPackLayer}) - only when
     * Curios is here to wear it in the first place; without Curios there is no back slot,
     * no worn pack, and no layer to add.
     */
    @SubscribeEvent
    public static void addPlayerLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return;
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer) {
                renderer.addLayer(new WornPackLayer(renderer));
            }
        }
    }
}
