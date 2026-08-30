package com.sappersquad.packwork.net;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.config.PackworkConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sends the server's config values to each client as it logs in (see {@link ConfigSyncPayload}). */
@EventBusSubscriber(modid = Packwork.MODID)
public final class ConfigSyncEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new ConfigSyncPayload(PackworkConfig.localValues()));
        }
    }

    private ConfigSyncEvents() {}
}
