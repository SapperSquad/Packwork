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

        // Dev visual harness: ./gradlew runClient -Pautoshot (or -Pgallery).
        if (System.getProperty("packwork.autoshot") != null
                || System.getProperty("packwork.gallery") != null) {
            DevAutoShot.register();
        }
    }
}
