package com.sappersquad.packwork.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * What the Waterskin Rack holds: one fluid and an amount in millibuckets. The Fabric
 * branch's stand-in for NeoForge's {@code SimpleFluidContent} component value - same
 * serialized shape ({@code id} + {@code amount} + optional {@code components}), and the
 * amount stays in mB so the GUI math, tier capacities, and store copy read identically on
 * both loaders. Conversion to Fabric's droplets (81 per mB) happens only at the transfer
 * face in {@link PackFluidHandler}.
 */
public record PackFluidContent(Fluid fluid, int amount, DataComponentPatch components) {

    public static final PackFluidContent EMPTY = new PackFluidContent(Fluids.EMPTY, 0, DataComponentPatch.EMPTY);

    public static final Codec<PackFluidContent> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("id").forGetter(PackFluidContent::fluid),
            Codec.INT.optionalFieldOf("amount", 0).forGetter(PackFluidContent::amount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(PackFluidContent::components)
    ).apply(i, PackFluidContent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PackFluidContent> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.registry(net.minecraft.core.registries.Registries.FLUID),
                    PackFluidContent::fluid,
                    ByteBufCodecs.VAR_INT, PackFluidContent::amount,
                    DataComponentPatch.STREAM_CODEC, PackFluidContent::components,
                    PackFluidContent::new);

    public static PackFluidContent of(Fluid fluid, int amountMb) {
        return amountMb <= 0 || fluid == Fluids.EMPTY ? EMPTY
                : new PackFluidContent(fluid, amountMb, DataComponentPatch.EMPTY);
    }

    public boolean isEmpty() {
        return amount <= 0 || fluid == Fluids.EMPTY;
    }

    /** Legacy-shaped accessors so the menu/screen/test vocabulary stays close to FluidStack's. */
    public Fluid getFluid() {
        return fluid;
    }

    public boolean is(Fluid other) {
        return !isEmpty() && fluid == other;
    }

    public int getAmount() {
        return isEmpty() ? 0 : amount;
    }

    public PackFluidContent copy() {
        return this; // immutable record - the copy() call sites keep reading naturally
    }

    public PackFluidContent withAmount(int newAmount) {
        return newAmount <= 0 ? EMPTY : new PackFluidContent(fluid, newAmount, components);
    }
}
