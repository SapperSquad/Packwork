package com.sappersquad.packwork.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The Flask Harness's chemical tank, stored as DIST-NEUTRAL primitives - a chemical registry
 * id and an amount in mB - so this component (and {@code ModComponents}) never imports
 * Mekanism. Only {@code compat/mekanism/MekanismChemicalStore} translates it to and from a
 * Mekanism {@code ChemicalStack}. Empty = no chemical or zero amount. One chemical at a time,
 * like the Waterskin tank.
 */
public record PackChemical(String chemical, long amount) {

    public static final PackChemical EMPTY = new PackChemical("", 0L);

    public static final Codec<PackChemical> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("chemical", "").forGetter(PackChemical::chemical),
            Codec.LONG.optionalFieldOf("amount", 0L).forGetter(PackChemical::amount)
    ).apply(inst, PackChemical::new));

    public static final StreamCodec<ByteBuf, PackChemical> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PackChemical::chemical,
            ByteBufCodecs.VAR_LONG, PackChemical::amount,
            PackChemical::new);

    public boolean isEmpty() {
        return chemical.isEmpty() || amount <= 0;
    }

    /** Tank size in mB: per-tier, packmaker-tunable ({@code tiers.<name>.vapor_mb}).
     *  Dist-neutral so the gauge can read it without Mekanism. */
    public static long capacityFor(ItemStack pack) {
        return com.sappersquad.packwork.config.PackworkConfig.get()
                .vaporMbFor(PackItem.tierOf(pack)); // default: Canvas 16k mB .. Sculkhide 96k
    }
}
