package com.sappersquad.packwork.net;

import com.sappersquad.packwork.Packwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Clientbound: everything the pack could make RIGHT NOW, computed on the server.
 *
 * <p>(1.21.11 port) 1.21.2 stopped syncing recipes to clients - the Recipe Ledger's old
 * client-side scan over the RecipeManager has no data to scan any more. So the server
 * (which still holds every recipe) runs the same craftable-from-pack-stock check it always
 * validated on lay-out, and ships the finished list: one (recipe id, result) pair per
 * craftable recipe. The client stays pure paint - search and scrolling still never touch
 * the wire, and item movement still happens only through LAY_OUT_GHOST.
 */
public record LedgerSyncPayload(List<Entry> entries) implements CustomPacketPayload {

    public record Entry(String recipeId, ItemStack result) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::recipeId,
                ItemStack.STREAM_CODEC, Entry::result,
                Entry::new);
    }

    public static final Type<LedgerSyncPayload> TYPE = new Type<>(Packwork.id("ledger_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LedgerSyncPayload> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list())
                    .map(LedgerSyncPayload::new, LedgerSyncPayload::entries);

    @Override
    public Type<LedgerSyncPayload> type() {
        return TYPE;
    }
}
