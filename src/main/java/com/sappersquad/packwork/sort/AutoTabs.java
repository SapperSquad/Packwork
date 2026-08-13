package com.sappersquad.packwork.sort;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The single source of truth for the built-in auto-tabs: their order, icons,
 * colours, and default rules. Adding a shipped compartment is one entry here plus
 * a lang key - nothing else.
 *
 * <p>Rules lean on {@link PredicateKind} (which recognises modded items by class,
 * so a modded pickaxe is a Tool for free) and on item tags. The tag <em>membership</em>
 * is datapack JSON (see {@code data/packwork/tags/item/sorting/*}), so a pack can
 * retune what counts as "ore" or "food" without touching code - which satisfies the
 * "data-driven categories" pillar.
 */
public final class AutoTabs {

    public static final String LOOSE_ID = "loose";

    private static Identifier vanilla(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    private static Identifier self(String path) {
        return Identifier.fromNamespaceAndPath("packwork", path);
    }

    /** Item tags owned by Packwork that datapacks fill in. */
    public static Identifier sortTag(String name) {
        return self("sorting/" + name);
    }

    private static SortRule sortTagRule(String name) {
        return SortRule.tag(self("sorting/" + name).toString());
    }

    /**
     * A built-in tab template. Kept package-simple: the id, its lang key, the icon
     * item, an ARGB leather tint, and the default rules.
     *
     * <p>{@code gate} is the fitting that has to be installed for the compartment to exist
     * at all - null for the compartments every pack ships with. This is how a trinket adds a
     * compartment with ONE entry in this table (plus its tag JSON and a lang key): the
     * Cartographer's Sleeve and the Angler's Creel each pay for themselves in sorting.
     */
    public record Auto(String id, String langKey, Identifier icon, int color,
                       List<SortRule> rules, com.sappersquad.packwork.trinket.TrinketType gate) {
        public Auto(String id, String langKey, Identifier icon, int color, List<SortRule> rules) {
            this(id, langKey, icon, color, rules, null);
        }
    }

    // Leather/brass-friendly tab tints. ARGB, opaque.
    public static final List<Auto> DEFAULTS = List.of(
            // Angler's Creel (fitting-gated): the catch gets first claim, ahead of Food, or
            // every cod you land would file itself under rations.
            new Auto("auto:catch", "packwork.tab.catch",
                    vanilla("cod"), 0xFF6E8BB9,
                    List.of(sortTagRule("catch"), SortRule.tag("minecraft:fishes")),
                    com.sappersquad.packwork.trinket.TrinketType.ANGLERS_CREEL),

            new Auto("auto:food", "packwork.tab.food",
                    vanilla("bread"), 0xFF8AB36B,
                    List.of(SortRule.predicate(PredicateKind.IS_FOOD), sortTagRule("food"))),

            // Combat before Tools on purpose: NeoForge tags swords under c:tools, so a
            // weapon must get first claim or it lands with the pickaxes.
            new Auto("auto:combat", "packwork.tab.combat",
                    vanilla("iron_sword"), 0xFFB4595A,
                    List.of(SortRule.predicate(PredicateKind.IS_WEAPON),
                            SortRule.predicate(PredicateKind.IS_ARMOR),
                            SortRule.tag("minecraft:arrows"),
                            sortTagRule("combat"))),

            // Cartographer's Sleeve (fitting-gated): charts, compasses, clocks and the spyglass,
            // claimed before Tools so a compass never ends up in with the pickaxes.
            new Auto("auto:charts", "packwork.tab.charts",
                    vanilla("filled_map"), 0xFFC9A24B,
                    List.of(sortTagRule("charts")),
                    com.sappersquad.packwork.trinket.TrinketType.CARTOGRAPHER),

            new Auto("auto:tools", "packwork.tab.tools",
                    vanilla("iron_pickaxe"), 0xFFB9905A,
                    List.of(SortRule.predicate(PredicateKind.IS_TOOL),
                            SortRule.tag("minecraft:shears"),
                            sortTagRule("tools"))),

            new Auto("auto:ores", "packwork.tab.ores",
                    vanilla("raw_iron"), 0xFF6E7B8B,
                    List.of(SortRule.tag("c:ores"),
                            SortRule.tag("c:ingots"),
                            SortRule.tag("c:raw_materials"),
                            SortRule.tag("c:gems"),
                            SortRule.tag("c:nuggets"),
                            SortRule.tag("c:storage_blocks"),
                            sortTagRule("ores"))),

            new Auto("auto:brewing", "packwork.tab.brewing",
                    vanilla("brewing_stand"), 0xFF7A5A9B,
                    List.of(SortRule.predicate(PredicateKind.IS_POTION),
                            SortRule.tag("c:crops/nether_wart"),
                            SortRule.name("potion"),
                            sortTagRule("brewing"))),

            new Auto("auto:nature", "packwork.tab.nature",
                    vanilla("oak_sapling"), 0xFF5FA05F,
                    List.of(SortRule.tag("minecraft:saplings"),
                            SortRule.tag("minecraft:flowers"),
                            SortRule.tag("minecraft:leaves"),
                            SortRule.tag("c:seeds"),
                            SortRule.tag("c:crops"),
                            sortTagRule("nature"))),

            // Blocks is the broad IS_BLOCK catch, so it sits LAST before Loose - the
            // specific tabs above (ores, nature, brewing) claim their blocks first.
            new Auto("auto:blocks", "packwork.tab.blocks",
                    vanilla("bricks"), 0xFF9C8265,
                    List.of(SortRule.predicate(PredicateKind.IS_BLOCK), sortTagRule("blocks")))
    );

    /** Default tab order when a pack has no custom order: all autos, then Loose. */
    public static List<String> defaultOrder() {
        return DEFAULTS.stream().map(Auto::id).toList();
    }

    /** Where this shipped compartment sits in the default priority run (-1 if it isn't one). */
    public static int priorityOf(String id) {
        for (int i = 0; i < DEFAULTS.size(); i++) if (DEFAULTS.get(i).id().equals(id)) return i;
        return -1;
    }

    public static Auto byId(String id) {
        for (Auto a : DEFAULTS) {
            if (a.id().equals(id)) return a;
        }
        return null;
    }

    public static TabView toView(Auto a) {
        return new TabView(a.id(), Component.translatable(a.langKey()), a.icon(), a.color(),
                a.rules(), false, false);
    }

    public static TabView looseView() {
        return new TabView(LOOSE_ID, Component.translatable("packwork.tab.loose"),
                vanilla("leather"), 0xFFA9946F, List.of(), false, true);
    }

    private AutoTabs() {}
}
