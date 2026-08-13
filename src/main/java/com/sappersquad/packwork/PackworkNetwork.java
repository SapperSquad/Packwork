package com.sappersquad.packwork;

import com.sappersquad.packwork.net.GhostSyncPayload;
import com.sappersquad.packwork.net.LedgerSyncPayload;
import com.sappersquad.packwork.net.OpenPackPayload;
import com.sappersquad.packwork.net.PackActionPayload;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The single serverbound channel the GUI talks over. Actions mutate the open
 * {@link PackMenu} on the server (the authority); the client mirrors them
 * optimistically for responsiveness. Fabric: payload TYPES register on both sides
 * here (called from common init); the clientbound RECEIVERS live in the client
 * entrypoint so nothing client-only classloads on a dedicated server.
 */
public final class PackworkNetwork {

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(PackActionPayload.TYPE, PackActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(OpenPackPayload.TYPE, OpenPackPayload.STREAM_CODEC);
        // The Recipe Ledger's data comes DOWN - recipes stopped syncing to clients in
        // 1.21.2, so the craftable list and the chalk arrangement are server-computed.
        PayloadTypeRegistry.clientboundPlay().register(LedgerSyncPayload.TYPE, LedgerSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GhostSyncPayload.TYPE, GhostSyncPayload.STREAM_CODEC);

        // Fabric play handlers already run on the server thread.
        ServerPlayNetworking.registerGlobalReceiver(PackActionPayload.TYPE, (payload, ctx) -> {
            if (ctx.player().containerMenu instanceof PackMenu menu) {
                menu.handleAction(payload.action(), payload.arg(), payload.s1(), payload.s2());
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(OpenPackPayload.TYPE,
                (payload, ctx) -> onOpen(payload, ctx.player()));
    }

    /** Gate every touch of the Trinkets compat class so its imports never classload without the mod. */
    private static final boolean TRINKETS_LOADED =
            net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets");

    private static void onOpen(OpenPackPayload payload, Player player) {
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
    }

    /** Open the pack worn in the Trinkets back slot, if Trinkets is here and one is worn. */
    private static boolean tryOpenWorn(Player player) {
        return TRINKETS_LOADED
                && player instanceof net.minecraft.server.level.ServerPlayer sp
                && com.sappersquad.packwork.compat.trinkets.TrinketsCompat.openWornPack(sp);
    }

    private PackworkNetwork() {}
}
