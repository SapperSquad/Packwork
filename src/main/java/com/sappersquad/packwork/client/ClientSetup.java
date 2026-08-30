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
     *
     * <p>(26.x port) The player renderer is {@code AvatarRenderer} now - mannequins share it -
     * and drawing a block needs the renderer context's {@code BlockModelResolver}, which is
     * only handed out here, so the layer takes it in its constructor.
     */
    @SubscribeEvent
    public static void addPlayerLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return;
        var blockModels = event.getContext().getBlockModelResolver();
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer<
                    net.minecraft.client.player.AbstractClientPlayer> renderer =
                    event.getPlayerRenderer(skin);
            renderer.addLayer(new WornPackLayer(renderer, blockModels));
        }
    }

    /**
     * (1.21.8+ port) Layers see a render STATE, not the entity, so the worn pack has to be
     * carried across: this puts it on the state every frame the player is extracted. It
     * always writes a value - EMPTY when nothing is worn - because a state object is reused
     * between frames and a leftover pack would keep drawing after you took it off.
     *
     * <p>(26.x port) NeoForge grew a purpose-built hook for avatars, so this takes that
     * instead of the generic renderer-class one. Avatars include MANNEQUINS, which are not
     * players and have no Curios inventory, so the modifier asks before it reads a pack.
     */
    @SubscribeEvent
    public static void registerRenderStateModifiers(
            net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return;
        event.registerAvatarEntityModifier(
                new net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier() {
                    @Override
                    public <T extends net.minecraft.world.entity.Avatar
                            & net.minecraft.client.entity.ClientAvatarEntity> void accept(
                            T entity, net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
                        net.minecraft.world.item.ItemStack pack =
                                entity instanceof net.minecraft.world.entity.player.Player player
                                        ? com.sappersquad.packwork.compat.curios.CuriosCompat.wornPack(player)
                                        : net.minecraft.world.item.ItemStack.EMPTY;
                        state.setRenderData(WornPackLayer.WORN_PACK,
                                pack.getItem() instanceof com.sappersquad.packwork.pack.PackItem
                                        ? pack : net.minecraft.world.item.ItemStack.EMPTY);
                    }
                });
    }

}
