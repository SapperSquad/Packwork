package com.sappersquad.packwork.net;

import com.sappersquad.packwork.Packwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * One serverbound packet for every GUI action. A flat {@code (action, arg, s1, s2)}
 * shape keeps the wire format and the registration trivial; each {@link PackAction}
 * documents which fields it reads.
 */
public record PackActionPayload(int action, int arg, String s1, String s2) implements CustomPacketPayload {

    public static final Type<PackActionPayload> TYPE = new Type<>(Packwork.id("pack_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PackActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PackActionPayload::action,
            ByteBufCodecs.VAR_INT, PackActionPayload::arg,
            ByteBufCodecs.STRING_UTF8, PackActionPayload::s1,
            ByteBufCodecs.STRING_UTF8, PackActionPayload::s2,
            PackActionPayload::new);

    public static PackActionPayload of(PackAction action, int arg, String s1, String s2) {
        return new PackActionPayload(action.ordinal(), arg, s1 == null ? "" : s1, s2 == null ? "" : s2);
    }

    @Override
    public Type<PackActionPayload> type() {
        return TYPE;
    }
}
