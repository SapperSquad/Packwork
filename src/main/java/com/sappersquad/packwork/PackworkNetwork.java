package com.sappersquad.packwork;

import com.sappersquad.packwork.net.OpenPackPayload;
import com.sappersquad.packwork.net.PackActionPayload;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
        registrar.playToServer(OpenPackPayload.TYPE, OpenPackPayload.STREAM_CODEC, PackworkNetwork::onOpen);
        // (1.21.11 port) the Recipe Ledger's data comes DOWN now - recipes stopped syncing
        // to clients in 1.21.2, so the craftable list and the chalk arrangement are
        // server-computed. Handlers defer into the client hooks class inside enqueueWork,
        // so nothing client-only classloads on a dedicated server.
        registrar.playToClient(com.sappersquad.packwork.net.LedgerSyncPayload.TYPE,
                com.sappersquad.packwork.net.LedgerSyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.sappersquad.packwork.client.PackClientActions.handleLedgerSync(payload)));
        registrar.playToClient(com.sappersquad.packwork.net.GhostSyncPayload.TYPE,
                com.sappersquad.packwork.net.GhostSyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.sappersquad.packwork.client.PackClientActions.handleGhostSync(payload)));
        // Server -> client on login: the server's packwork-server.toml values, so gauges,
        // slot counts and trinket gates draw exactly what the server enforces.
        registrar.playToClient(com.sappersquad.packwork.net.ConfigSyncPayload.TYPE,
                com.sappersquad.packwork.net.ConfigSyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        com.sappersquad.packwork.config.PackworkConfig.setRemote(payload.values())));
    }

    private static void onAction(PackActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player().containerMenu instanceof PackMenu menu) {
                menu.handleAction(payload.action(), payload.arg(), payload.s1(), payload.s2());
            }
        });
    }

    /** Gate every touch of the Curios compat class so its imports never classload without the mod. */
    private static final boolean CURIOS_LOADED = net.neoforged.fml.ModList.get().isLoaded("curios");

    private static void onOpen(OpenPackPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            // Shift-B: the worn back-slot pack first, explicitly.
            if (payload.worn() && tryOpenWorn(player)) {
                return;
            }
            int hint = payload.hintSlot();
            if (hint >= 0 && hint < player.getInventory().getContainerSize()
                    && player.getInventory().getItem(hint).getItem() instanceof PackItem) {
                PackItem.openPack(player, hint);
                return;
            }
            // scan for the first pack the player is carrying - pockets first, then the worn
            // back slot, the same order the pack-first pickup routing scans
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s.getItem() instanceof PackItem) {
                    PackItem.openPack(player, i);
                    return;
                }
            }
            tryOpenWorn(player);
        });
    }

    /** Open the pack worn in the Curios back slot, if Curios is here and one is worn. */
    private static boolean tryOpenWorn(Player player) {
        return CURIOS_LOADED
                && player instanceof net.minecraft.server.level.ServerPlayer sp
                && com.sappersquad.packwork.compat.curios.CuriosCompat.openWornPack(sp);
    }

    private PackworkNetwork() {}
}
