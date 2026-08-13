package com.sappersquad.packwork.net;

import com.sappersquad.packwork.Packwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Clientbound: one chalked recipe's 3x3 arrangement for the tool roll's ghost overlay.
 *
 * <p>(1.21.11 port) The client can no longer resolve a recipe id into ingredients (recipes
 * stopped syncing in 1.21.2), so chalking asks the server: REQUEST_GHOST goes up with the
 * id, and this comes back with the SAME row-major arrangement the server's LAY_OUT_GHOST
 * uses ({@code PackMenu.arrangeOn3x3} - still one shared helper, so chalk and lay-out
 * cannot drift), each cell as its display stacks (capped, cycled client-side), plus the
 * promised result for the empty result well. An empty recipeId clears the chalk.
 */
public record GhostSyncPayload(String recipeId, ItemStack result,
                               List<List<ItemStack>> cells) implements CustomPacketPayload {

    public static final Type<GhostSyncPayload> TYPE = new Type<>(Packwork.id("ghost_sync"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<List<ItemStack>>> CELLS_CODEC =
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, GhostSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GhostSyncPayload::recipeId,
            ItemStack.OPTIONAL_STREAM_CODEC, GhostSyncPayload::result,
            CELLS_CODEC, GhostSyncPayload::cells,
            GhostSyncPayload::new);

    @Override
    public Type<GhostSyncPayload> type() {
        return TYPE;
    }
}
