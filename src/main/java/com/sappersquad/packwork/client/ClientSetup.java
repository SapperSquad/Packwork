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

    /**
     * (1.21.8+ port) Layers see a render STATE, not the entity, so the worn pack has to be
     * carried across: this puts it on the state every frame the player is extracted. It
     * always writes a value - EMPTY when nothing is worn - because a state object is reused
     * between frames and a leftover pack would keep drawing after you took it off.
     */
    @SubscribeEvent
    public static void registerRenderStateModifiers(
            net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return;
        event.registerEntityModifier(
                net.minecraft.client.renderer.entity.player.PlayerRenderer.class,
                (net.minecraft.client.player.AbstractClientPlayer player,
                 net.minecraft.client.renderer.entity.state.PlayerRenderState state) -> {
                    var pack = com.sappersquad.packwork.compat.curios.CuriosCompat.wornPack(player);
                    state.setRenderData(WornPackLayer.WORN_PACK,
                            pack.getItem() instanceof com.sappersquad.packwork.pack.PackItem
                                    ? pack : net.minecraft.world.item.ItemStack.EMPTY);
                });
    }

}
