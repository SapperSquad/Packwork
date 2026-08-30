package com.sappersquad.packwork.reg;

import com.mojang.serialization.MapCodec;
import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.config.TrinketEnabledCondition;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** The config-driven recipe condition ({@code packwork:trinket_enabled}). */
public class ModConditions {

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Packwork.MODID);

    public static final Supplier<MapCodec<TrinketEnabledCondition>> TRINKET_ENABLED =
            CONDITIONS.register("trinket_enabled", () -> TrinketEnabledCondition.CODEC);
}
