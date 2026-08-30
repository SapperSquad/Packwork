package com.sappersquad.packwork.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sappersquad.packwork.reg.ModConditions;
import com.sappersquad.packwork.trinket.TrinketType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.RegistryOps;

/**
 * The recipe gate behind the config's per-trinket switches: every fitting recipe carries
 * {@code {"condition": "packwork:trinket_enabled", "trinket": "<id>"}} in its
 * {@code fabric:load_conditions}, so a config-disabled trinket has no recipe at all - JEI
 * and the recipe book follow suit, and no dead craftable ever reaches a player. Conditions
 * re-evaluate on datapack load, so the gate applies at server start and on {@code /reload}.
 *
 * <p>An unknown trinket id in the JSON reads as "enabled" (the recipe stays) - a datapack
 * typo should never silently delete a recipe.
 *
 * <p>(Fabric) Same idea and the same TOML key as the NeoForge branches; only the interface
 * differs. Fabric's {@link ResourceCondition} asks for the type back and takes the registry
 * lookup as its argument, where NeoForge's ICondition takes its own context object - the
 * config is the authority in both, so neither is consulted here.
 */
public record TrinketEnabledCondition(String trinket) implements ResourceCondition {

    public static final MapCodec<TrinketEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(Codec.STRING.fieldOf("trinket").forGetter(TrinketEnabledCondition::trinket))
                    .apply(builder, TrinketEnabledCondition::new));

    @Override
    public ResourceConditionType<?> getType() {
        return ModConditions.TRINKET_ENABLED;
    }

    @Override
    public boolean test(RegistryOps.RegistryInfoLookup lookup) {
        for (TrinketType t : TrinketType.values()) {
            if (t.id().equals(trinket)) {
                return PackworkConfig.get().enabled(t);
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "trinket_enabled(\"" + trinket + "\")";
    }
}
