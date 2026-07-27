package com.sappersquad.packwork.net;

import com.sappersquad.packwork.Packwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Keybind request to open a pack. Opening a menu is server-authoritative, so the client
 * only asks; the server finds and opens the pack (a hint slot, or -1 to scan for the
 * first pack it can find - pockets first, then the worn back slot, the same order the
 * pickup routing uses). {@code worn} asks for the back-slot pack FIRST (the Shift-B
 * keybind); it still falls back to the pockets so the key always opens something.
 */
public record OpenPackPayload(int hintSlot, boolean worn) implements CustomPacketPayload {

    public static final Type<OpenPackPayload> TYPE = new Type<>(Packwork.id("open_pack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenPackPayload::hintSlot,
            ByteBufCodecs.BOOL, OpenPackPayload::worn,
            OpenPackPayload::new);

    @Override
    public Type<OpenPackPayload> type() {
        return TYPE;
    }
}
