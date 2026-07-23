package com.sappersquad.packwork.sort;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * The durable sorting configuration carried on a pack stack: the tab order the
 * player has set, any custom tabs they built, and manual pins that always win.
 *
 * <p>Pure view state (which tab is showing, the search text, the flatten toggle,
 * the page) deliberately lives only in the open menu, not here - so flipping tabs
 * never rewrites the item component.
 *
 * <p>An {@link #EMPTY} layout means "use the built-in auto-tab order, no custom
 * tabs, no pins" - a brand-new pack.
 *
 * @param tabOrder   ordered tab ids (mix of {@code auto:*} and {@code custom:*}); empty = default
 * @param customTabs player-made tab definitions
 * @param pins       item-to-tab overrides that beat every rule
 */
public record PackLayout(List<String> tabOrder, List<TabDef> customTabs, List<Pin> pins) {

    public static final PackLayout EMPTY = new PackLayout(List.of(), List.of(), List.of());

    /** A manual override: this exact item always lands in this tab. */
    public record Pin(ResourceLocation item, String tabId) {
        public static final Codec<Pin> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Pin::item),
                Codec.STRING.fieldOf("tab").forGetter(Pin::tabId)
        ).apply(inst, Pin::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Pin> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Pin::item,
                ByteBufCodecs.STRING_UTF8, Pin::tabId,
                Pin::new);
    }

    public static final Codec<PackLayout> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.listOf().optionalFieldOf("tab_order", List.of()).forGetter(PackLayout::tabOrder),
            TabDef.CODEC.listOf().optionalFieldOf("custom_tabs", List.of()).forGetter(PackLayout::customTabs),
            Pin.CODEC.listOf().optionalFieldOf("pins", List.of()).forGetter(PackLayout::pins)
    ).apply(inst, PackLayout::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PackLayout> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), PackLayout::tabOrder,
            TabDef.STREAM_CODEC.apply(ByteBufCodecs.list()), PackLayout::customTabs,
            Pin.STREAM_CODEC.apply(ByteBufCodecs.list()), PackLayout::pins,
            PackLayout::new);

    /** The tab id a manual pin sends this item to, or null if unpinned. */
    public String pinnedTab(ResourceLocation itemId) {
        for (Pin p : pins) {
            if (p.item().equals(itemId)) return p.tabId();
        }
        return null;
    }

    public PackLayout withPins(List<Pin> newPins) {
        return new PackLayout(tabOrder, customTabs, newPins);
    }

    public PackLayout withCustomTabs(List<TabDef> newTabs) {
        return new PackLayout(tabOrder, newTabs, pins);
    }

    public PackLayout withTabOrder(List<String> newOrder) {
        return new PackLayout(newOrder, customTabs, pins);
    }

    /** Custom tab matching an id, or null. */
    public TabDef customTab(String id) {
        for (TabDef t : customTabs) {
            if (t.id().equals(id)) return t;
        }
        return null;
    }

    /** Next free {@code custom:<n>} id given the current custom tabs. */
    public String nextCustomId() {
        int max = -1;
        for (TabDef t : customTabs) {
            if (t.id().startsWith("custom:")) {
                try {
                    max = Math.max(max, Integer.parseInt(t.id().substring("custom:".length())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return "custom:" + (max + 1);
    }

    public List<String> mutableTabOrder() {
        return new ArrayList<>(tabOrder);
    }
}
