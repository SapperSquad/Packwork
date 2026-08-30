package com.sappersquad.packwork.net;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.config.PackworkConfig;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.trinket.TrinketType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

/**
 * The server's {@code packwork-server.toml} values, sent to each client on login so both
 * sides agree on slot counts, depth, store capacities, trinket enablement and the
 * pack-first default - the GUI then draws exactly what the server enforces. The client
 * overlays these over its own file while connected and drops them on disconnect.
 * Hand-rolled field-by-field (like the config itself) so the wire shape is identical
 * on NeoForge and Fabric.
 */
public record ConfigSyncPayload(PackworkConfig.Values values) implements CustomPacketPayload {

    public static final Type<ConfigSyncPayload> TYPE = new Type<>(Packwork.id("config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC =
            StreamCodec.of(ConfigSyncPayload::write, ConfigSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, ConfigSyncPayload payload) {
        PackworkConfig.Values v = payload.values();
        int tiers = PackTier.values().length;
        buf.writeVarInt(tiers);
        for (int i = 0; i < tiers; i++) {
            buf.writeVarInt(v.slots()[i]);
            buf.writeVarInt(v.stacksPerSlot()[i]);
            buf.writeVarInt(v.fluidMb()[i]);
            buf.writeVarInt(v.xpPoints()[i]);
            buf.writeVarInt(v.energyFe()[i]);
            buf.writeVarLong(v.vaporMb()[i]);
        }
        int trinkets = TrinketType.values().length;
        buf.writeVarInt(trinkets);
        for (int i = 0; i < trinkets; i++) {
            buf.writeBoolean(v.trinketEnabled()[i]);
        }
        buf.writeEnum(v.deathHandling());
        buf.writeDouble(v.magnetRange());
        buf.writeVarInt(v.magnetEveryTicks());
        buf.writeBoolean(v.packFirstDefault());
        buf.writeVarInt(v.neverAutoEat().size());
        for (ResourceLocation id : v.neverAutoEat()) {
            buf.writeResourceLocation(id);
        }
    }

    private static ConfigSyncPayload read(RegistryFriendlyByteBuf buf) {
        PackworkConfig.Values d = PackworkConfig.defaults();
        int tiers = buf.readVarInt();
        int myTiers = PackTier.values().length;
        int[] slots = d.slots().clone();
        int[] depth = d.stacksPerSlot().clone();
        int[] fluid = d.fluidMb().clone();
        int[] xp = d.xpPoints().clone();
        int[] fe = d.energyFe().clone();
        long[] vapor = d.vaporMb().clone();
        for (int i = 0; i < tiers; i++) {
            int s = buf.readVarInt();
            int dm = buf.readVarInt();
            int fl = buf.readVarInt();
            int x = buf.readVarInt();
            int e = buf.readVarInt();
            long vp = buf.readVarLong();
            if (i < myTiers) {
                slots[i] = s;
                depth[i] = dm;
                fluid[i] = fl;
                xp[i] = x;
                fe[i] = e;
                vapor[i] = vp;
            }
        }
        int trinkets = buf.readVarInt();
        boolean[] enabled = d.trinketEnabled().clone();
        for (int i = 0; i < trinkets; i++) {
            boolean b = buf.readBoolean();
            if (i < enabled.length) enabled[i] = b;
        }
        PackworkConfig.DeathHandling death = buf.readEnum(PackworkConfig.DeathHandling.class);
        double magnetRange = buf.readDouble();
        int magnetTicks = buf.readVarInt();
        boolean packFirst = buf.readBoolean();
        int noEatCount = buf.readVarInt();
        Set<ResourceLocation> noEat = new HashSet<>();
        for (int i = 0; i < noEatCount; i++) {
            noEat.add(buf.readResourceLocation());
        }
        return new ConfigSyncPayload(new PackworkConfig.Values(slots, depth, fluid, xp, fe, vapor,
                enabled, death, magnetRange, magnetTicks, packFirst, Set.copyOf(noEat)));
    }

    @Override
    public Type<ConfigSyncPayload> type() {
        return TYPE;
    }
}
