package com.sappersquad.packwork.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * The menu-open data the server sends alongside a pack menu: which of the three hosts
 * the client should bind (carried / placed block / worn), where, and the pack's tier.
 * The tier rides so the client builds the SAME trinket-socket count as the server even
 * before its host stack syncs - a mismatch there overruns the container-content packet
 * and drops the player. (Fabric's {@code ExtendedMenuType} carries this where the
 * NeoForge branches hand-write the same fields onto the open packet's buffer.)
 */
public record PackOpenData(int kind, BlockPos pos, int slot, int tier) {

    // The host kinds, mirrored by PackItem's HOST_* constants.
    public static final int HOST_CARRIED = 0;
    public static final int HOST_BLOCK = 1;
    public static final int HOST_WORN = 2;

    public static final StreamCodec<FriendlyByteBuf, PackOpenData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PackOpenData::kind,
                    BlockPos.STREAM_CODEC, PackOpenData::pos,
                    ByteBufCodecs.VAR_INT, PackOpenData::slot,
                    ByteBufCodecs.VAR_INT, PackOpenData::tier,
                    PackOpenData::new);

    public static PackOpenData carried(int slot, int tier) {
        return new PackOpenData(HOST_CARRIED, BlockPos.ZERO, slot, tier);
    }

    public static PackOpenData block(BlockPos pos, int tier) {
        return new PackOpenData(HOST_BLOCK, pos, -1, tier);
    }

    public static PackOpenData worn(int tier) {
        return new PackOpenData(HOST_WORN, BlockPos.ZERO, -1, tier);
    }
}
