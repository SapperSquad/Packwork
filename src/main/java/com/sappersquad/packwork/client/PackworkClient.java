package com.sappersquad.packwork.client;

import com.sappersquad.packwork.net.GhostSyncPayload;
import com.sappersquad.packwork.net.LedgerSyncPayload;
import com.sappersquad.packwork.reg.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * Client entrypoint: screen binding, keybinds, the clientbound payload receivers, and
 * (dev-only, behind its system property) the autoshot harness. Everything client-only
 * lives behind this entrypoint so nothing here ever classloads on a dedicated server.
 */
public class PackworkClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Client cosmetics only (packwork-client.toml); the server file is read in the
        // common entrypoint and is the authority for everything that affects the game.
        com.sappersquad.packwork.config.PackworkConfig.loadClient(
                net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());

        // MenuScreens.register is vanilla-private; Fabric's transitive access widener
        // (fabric-menu-api-v1) opens it - the supported registration path on 26.x.
        MenuScreens.register(ModMenus.PACK.get(), PackScreen::new);

        PackKeyMappings.register();

        // Clientbound sync: the Recipe Ledger's craftable list + the chalk arrangement.
        // Fabric play handlers already run on the client thread.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                LedgerSyncPayload.TYPE,
                (payload, ctx) -> PackClientActions.handleLedgerSync(payload));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                GhostSyncPayload.TYPE,
                (payload, ctx) -> PackClientActions.handleGhostSync(payload));

        // The server's config values, overlaid while connected and dropped on disconnect,
        // so a single-player world's own file comes straight back when you leave a server.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.sappersquad.packwork.net.ConfigSyncPayload.TYPE,
                (payload, ctx) -> com.sappersquad.packwork.config.PackworkConfig.setRemote(payload.values()));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> com.sappersquad.packwork.config.PackworkConfig.setRemote(null));

        // The worn pack renders on the player's back (see WornPackLayer) - only when
        // Trinkets is here to wear it in the first place; without Trinkets there is no back
        // slot, no worn pack, and no layer to add. Mannequins share the avatar renderer, so
        // they get the layer too and simply never have a pack to draw.
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
            net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback.EVENT
                    .register((entityType, renderer, helper, ctx) -> {
                        if (renderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer<?> avatar) {
                            helper.register(new WornPackLayer(
                                    (net.minecraft.client.renderer.entity.RenderLayerParent<
                                            net.minecraft.client.renderer.entity.state.AvatarRenderState,
                                            net.minecraft.client.model.player.PlayerModel>) avatar,
                                    ctx.getBlockModelResolver()));
                        }
                    });
        }

        // Dev visual harness: ./gradlew runClient -Pautoshot (or -Pgallery).
        if (System.getProperty("packwork.autoshot") != null
                || System.getProperty("packwork.gallery") != null
                || System.getProperty("packwork.wornshot") != null) {
            DevAutoShot.register();
        }
    }
}
