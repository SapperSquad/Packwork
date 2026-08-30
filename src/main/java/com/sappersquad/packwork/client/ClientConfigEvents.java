package com.sappersquad.packwork.client;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.config.PackworkConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Drops the remote server's config overlay when the client disconnects. */
@EventBusSubscriber(modid = Packwork.MODID, value = Dist.CLIENT)
public final class ClientConfigEvents {

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PackworkConfig.setRemote(null);
    }

    private ClientConfigEvents() {}
}
