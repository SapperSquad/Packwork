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
     * <p>(1.21.10+ port) The player renderer is {@code AvatarRenderer} now - mannequins
     * share it - so the cast names that class instead of {@code PlayerRenderer}, and only
     * the PLAYER renderers come out of {@code getPlayerRenderer}.
     */
    @SubscribeEvent
    public static void addPlayerLayers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return;
        for (var skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.AvatarRenderer<
                    net.minecraft.client.player.AbstractClientPlayer> renderer =
                    event.getPlayerRenderer(skin);
            renderer.addLayer(new WornPackLayer(renderer));
        }
    }

    /**
     * (1.21.8+ port) Layers see a render STATE, not the entity, so the worn pack has to be
     * carried across: this puts it on the state every frame the player is extracted. It
     * always writes a value - EMPTY when nothing is worn - because a state object is reused
     * between frames and a leftover pack would keep drawing after you took it off.
     *
     * <p>(1.21.10+ port) {@code AvatarRenderer} is generic now, so its class literal is a RAW
     * {@code Class<AvatarRenderer>} that will not fit the event's
     * {@code Class<? extends EntityRenderer<? extends E, ? extends S>>} parameter - hence the
     * one unchecked cast. That same renderer class also draws MANNEQUINS, which are Avatars
     * but not players, so the modifier takes a plain Entity and asks before it reads a pack.
     */
    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerRenderStateModifiers(
            net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) return;
        Class<? extends net.minecraft.client.renderer.entity.EntityRenderer<
                ? extends net.minecraft.world.entity.Entity,
                ? extends net.minecraft.client.renderer.entity.state.AvatarRenderState>> avatarRenderer =
                (Class) net.minecraft.client.renderer.entity.player.AvatarRenderer.class;
        event.registerEntityModifier(avatarRenderer,
                (net.minecraft.world.entity.Entity entity,
                 net.minecraft.client.renderer.entity.state.AvatarRenderState state) -> {
                    net.minecraft.world.item.ItemStack pack =
                            entity instanceof net.minecraft.world.entity.player.Player player
                                    ? com.sappersquad.packwork.compat.curios.CuriosCompat.wornPack(player)
                                    : net.minecraft.world.item.ItemStack.EMPTY;
                    state.setRenderData(WornPackLayer.WORN_PACK,
                            pack.getItem() instanceof com.sappersquad.packwork.pack.PackItem
                                    ? pack : net.minecraft.world.item.ItemStack.EMPTY);
                });
    }

}
