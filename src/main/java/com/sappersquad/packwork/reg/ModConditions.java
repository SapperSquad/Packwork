package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.config.TrinketEnabledCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;

/** The config-driven recipe condition ({@code packwork:trinket_enabled}). */
public final class ModConditions {

    public static final ResourceConditionType<TrinketEnabledCondition> TRINKET_ENABLED =
            ResourceConditionType.create(Packwork.id("trinket_enabled"), TrinketEnabledCondition.CODEC);

    /** Called from the mod initializer; Fabric conditions are registered, not deferred. */
    public static void init() {
        ResourceConditions.register(TRINKET_ENABLED);
    }

    private ModConditions() {}
}
