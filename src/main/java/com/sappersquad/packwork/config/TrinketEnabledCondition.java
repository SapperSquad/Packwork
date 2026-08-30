package com.sappersquad.packwork.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sappersquad.packwork.trinket.TrinketType;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * The recipe gate behind the config's per-trinket switches: every fitting recipe carries
 * {@code {"type": "packwork:trinket_enabled", "trinket": "<id>"}} in its
 * {@code neoforge:conditions}, so a config-disabled trinket has no recipe at all - JEI and
 * the recipe book follow suit, and no dead craftable ever reaches a player. Conditions
 * re-evaluate on datapack load, so the gate applies at server start and on {@code /reload}.
 *
 * <p>An unknown trinket id in the JSON reads as "enabled" (the recipe stays) - a datapack
 * typo should never silently delete a recipe.
 */
public record TrinketEnabledCondition(String trinket) implements ICondition {

    public static final MapCodec<TrinketEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(Codec.STRING.fieldOf("trinket").forGetter(TrinketEnabledCondition::trinket))
                    .apply(builder, TrinketEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        for (TrinketType t : TrinketType.values()) {
            if (t.id().equals(trinket)) {
                return PackworkConfig.get().enabled(t);
            }
        }
        return true;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "trinket_enabled(\"" + trinket + "\")";
    }
}
