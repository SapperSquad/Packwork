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
