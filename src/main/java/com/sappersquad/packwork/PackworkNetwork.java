package com.sappersquad.packwork;

import com.sappersquad.packwork.net.PackActionPayload;
import com.sappersquad.packwork.pack.PackMenu;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The single serverbound channel the GUI talks over. Actions mutate the open
 * {@link PackMenu} on the server (the authority); the client mirrors them
 * optimistically for responsiveness.
 */
public final class PackworkNetwork {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(PackActionPayload.TYPE, PackActionPayload.STREAM_CODEC, PackworkNetwork::onAction);
    }

    private static void onAction(PackActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().containerMenu instanceof PackMenu menu) {
                menu.handleAction(payload.action(), payload.arg(), payload.s1(), payload.s2());
            }
        });
    }

    private PackworkNetwork() {}
}
