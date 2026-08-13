package com.sappersquad.packwork.sort;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * A player-made compartment: a stamped leather tab with a name, an item icon,
 * a dyed colour, and its own ordered list of match rules. Auto-tabs are defined
 * in code ({@link AutoTabs}); only custom tabs are serialised here on the pack.
 *
 * @param id     stable id, always of the form {@code custom:<n>}
 * @param name   display text shown on the tab
 * @param icon   item id whose sprite stamps the tab
 * @param color  ARGB tint for the leather (0 = undyed)
 * @param rules  ordered rules; first match in tab-priority order claims an item
 */
public record TabDef(String id, String name, ResourceLocation icon, int color, List<SortRule> rules) {

    public static final Codec<TabDef> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("id").forGetter(TabDef::id),
            Codec.STRING.fieldOf("name").forGetter(TabDef::name),
            ResourceLocation.CODEC.fieldOf("icon").forGetter(TabDef::icon),
            Codec.INT.fieldOf("color").forGetter(TabDef::color),
            SortRule.CODEC.listOf().fieldOf("rules").forGetter(TabDef::rules)
    ).apply(inst, TabDef::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TabDef> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TabDef::id,
            ByteBufCodecs.STRING_UTF8, TabDef::name,
            ResourceLocation.STREAM_CODEC, TabDef::icon,
            ByteBufCodecs.INT, TabDef::color,
            SortRule.STREAM_CODEC.apply(ByteBufCodecs.list()), TabDef::rules,
            TabDef::new);

    public TabDef withName(String newName) {
        return new TabDef(id, newName, icon, color, rules);
    }

    public TabDef withIcon(ResourceLocation newIcon) {
        return new TabDef(id, name, newIcon, color, rules);
    }

    public TabDef withColor(int newColor) {
        return new TabDef(id, name, icon, newColor, rules);
    }

    public TabDef withRules(List<SortRule> newRules) {
        return new TabDef(id, name, icon, color, newRules);
    }
}
