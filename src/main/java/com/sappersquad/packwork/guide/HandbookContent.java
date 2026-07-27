package com.sappersquad.packwork.guide;

import com.sappersquad.packwork.pack.PackEnergyStorage;
import com.sappersquad.packwork.pack.PackFluidHandler;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.pack.PackXpStore;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.trinket.TrinketType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The Outfitter's Handbook contents: text paragraphs interleaved with rows of rendered
 * item icons, laid out by {@code OutfitterHandbookScreen}. Modelled on PhytoForge's
 * {@code ManualContent} (client screen + content class, zero deps).
 *
 * <p>Numbers are interpolated from the real implementation - {@link PackTier}, the store
 * capacity helpers, and the {@link TrinketType} table - so the book can't drift out of
 * step with the code. Prose still needs a human when a mechanic changes; the numbers no
 * longer do.
 *
 * <p>This class is deliberately dist-neutral (no client-only imports) so a gametest can
 * assert the content builds, and so it is safe on a dedicated server. Entries construct
 * ItemStacks, so {@link #CHAPTERS} must not be touched before registries freeze - safe in
 * practice because the book only opens in-world.
 */
public final class HandbookContent {

    public sealed interface Entry permits TextEntry, ItemsEntry {}

    public record TextEntry(String text) implements Entry {}

    /** A row of rendered item icons with an optional caption to its right. */
    public record ItemsEntry(String caption, List<ItemStack> items) implements Entry {}

    public record Chapter(String title, List<Entry> entries) {}

    private HandbookContent() {}

    private static TextEntry text(String s) { return new TextEntry(s); }

    private static ItemsEntry row(String caption, ItemStack... stacks) {
        return new ItemsEntry(caption, List.of(stacks));
    }

    private static ItemStack pack(PackTier tier) { return new ItemStack(ModItems.pack(tier).get()); }

    /** Every tier's pack, ladder order - a new tier appears in the book by existing. */
    private static ItemStack[] allPacks() {
        return java.util.Arrays.stream(PackTier.values()).map(HandbookContent::pack).toArray(ItemStack[]::new);
    }

    private static ItemStack trinket(TrinketType type) { return new ItemStack(ModItems.trinket(type).get()); }

    private static String buckets(PackTier tier) {
        return (PackFluidHandler.capacityFor(pack(tier)) / 1000) + " buckets";
    }

    /** One tier's numbers, straight off the SSOT: "NAME slots/stacksPerSlot/sockets." */
    private static String tierLine(PackTier t) {
        return t.getSerializedName().toUpperCase(java.util.Locale.ROOT) + " " + t.capacity()
                + " / x" + t.depthMultiplier() + " / " + t.trinketSlots() + ".";
    }

    /** The top tier's display name for "up to ..." sentences - never goes stale on a new tier. */
    private static String topName() {
        return PackTier.top().getSerializedName().toUpperCase(java.util.Locale.ROOT);
    }

    public static final List<Chapter> CHAPTERS = List.of(

        // =================================================================
        new Chapter("The Pack", List.of(
            text("A humble adventurer's pack that holds far more than it should - and quietly "
                + "organizes itself. Right-click one in hand to open it, or press the Open Pack key "
                + "(default B) to open the first pack you're carrying without digging it out. "
                + "Wearing one on your back (a Curios back slot)? B finds it when your pockets hold "
                + "no pack, and Shift-B opens the worn one straight off your shoulders."),
            row("one ladder, every pack", allPacks()),
            text("Drop an item in and the pack decides where it goes. Stamped leather tabs run down "
                + "the left; a rules engine claims each item for the right tab the instant it lands. "
                + "A Loose tab catches anything no rule wanted, so nothing you drop in ever vanishes."),
            text("Everything here is leather, brass, canvas and glass. Later fittings let one pack "
                + "carry fluids, arcane charge and soul-vialed XP - each a trinket you craft and "
                + "slot, never a tank-and-cable panel."))),

        // =================================================================
        new Chapter("Sorting", List.of(
            text("The tabs down the left ARE the compartments. Click one to see what it holds; the "
                + "top-to-bottom order is priority, and the first tab whose rules want an item claims "
                + "it. Reorder tabs and you re-decide who gets first pick."),
            text("Auto-tabs, shipped and ready: Food, Combat, Tools & Utility, Ores & Valuables, "
                + "Brewing & Alchemy, Nature & Farming, Blocks & Building, and Loose. They sort by "
                + "what an item IS - any mod's pickaxe is a Tool, any mod's food is Food - so they "
                + "cover modded items with no per-mod support."),
            text("A Loose tab always sits last and claims anything no rule wanted. Nothing dropped "
                + "into a pack is ever lost; at worst it lands in Loose."),
            text("PIN an item to keep it where you put it. Drop it into any tab it wouldn't sort "
                + "to and the pack pins it there on the spot - watch for the stitched note - or "
                + "hover it and press P. A pinned item wears a red ribbon, beats every rule, and "
                + "never gets moved by the sort. Press P over a pinned item to release it."),
            text("Every compartment has an ARRANGEMENT switch, under the grid by the page count. "
                + "TIDY (the default) lets the pack arrange the compartment; flip it to KEEP MY "
                + "LAYOUT and items stay in the exact cells you drop them, with new arrivals "
                + "filling the gaps. Tidy Up still works as a one-shot re-sort - the sorted order "
                + "just becomes your new starting layout. Flip back and the pack takes over again."),
            text("Three tools across the title bar: TIDY UP merges partial stacks and re-sorts the "
                + "whole pack; SEARCH finds anything across every tab; FLATTEN drops it all into one "
                + "grid when you just want to rummage."),
            text("Make your own compartments. The + button adds one; then hover an item and press I "
                + "to stamp it as the tab's icon, R to rename, middle-click the tab to dye its "
                + "leather, and [ or ] to slide it up or down the rail. Right-click removes it."),
            text("The STAMP is a live filter, not just a picture: stamp a tab with a pickaxe and "
                + "it gathers tools, stamp it with bread and it gathers food - no fitting needed. "
                + "Want sharper filters than that? That's the Quill & Ledger's desk - see "
                + "Trinkets."))),

        // =================================================================
        new Chapter("Trinkets", List.of(
            text("Brass sockets run down the right rail - the number of them is your pack's tier. "
                + "A trinket is a fitting you craft and drop into a socket; pull it back out any time. "
                + "Each grants the pack one trick."),
            row("carry & keep",
                trinket(TrinketType.LODESTONE), trinket(TrinketType.RESTOCK),
                trinket(TrinketType.REPAIR), trinket(TrinketType.BOTTOMLESS)),
            text("LODESTONE CHARM draws loose items nearby into the pack - and it files what you "
                + "mine. With one fitted, anything you pick up that the pack knows where to put "
                + "(it sorts to a compartment, it's pinned, or the pack already holds some) goes "
                + "STRAIGHT IN; new, unknown finds still go to your pockets, so nothing vanishes "
                + "into the bag unseen. A small switch in the title strip turns pack-first pickup "
                + "off per pack. RESTOCK STRAP tops up your hotbar stacks from pack stock. REPAIR "
                + "KIT slowly mends the gear you're wearing and holding. BOTTOMLESS LINING adds "
                + "another run of slots - and never voids them if you pull it back out; the extra "
                + "items just hide until it's refitted."),
            row("sort & swap",
                trinket(TrinketType.COMPASS_ROSE), trinket(TrinketType.QUILL_LEDGER),
                trinket(TrinketType.QUICK_DRAW)),
            text("COMPASS ROSE is the only way a pack throws anything away, and it's opt-in: hover an "
                + "item and press O to mark it, and only marked items are voided on the way in."),
            text("QUILL & LEDGER is the rule editor. Fit it, open a custom compartment, and click "
                + "the quill under the grid: a parchment sheet where you write your own filters - "
                + "by name ('ingot'), by mod, or by category chip - and strike them off again. "
                + "Written rules sort only while the ledger is fitted; pull it and the tab falls "
                + "back to its stamp and pins, with the rules kept safe in the leather for its "
                + "return. Pins still win over everything."),
            text("QUICK-DRAW STRAPS keep you swinging: when a tool breaks in your hand, the pack "
                + "passes you an identical one from stock without missing a beat. It only ever hands "
                + "back what the pack actually holds, so it can't conjure a duplicate."),
            row("the working kit",
                trinket(TrinketType.TINKERS_KIT), trinket(TrinketType.FIELD_FURNACE),
                trinket(TrinketType.PROVISIONER), trinket(TrinketType.TORCHBEARER)),
            text("TINKER'S KIT unrolls a leather tool roll across the pack's lower rows: a 3x3 bench "
                + "you can work at anywhere. Shift-click from the pack to lay ingredients out, and "
                + "after each craft the bench tops itself back up from your stores, so one shift-click "
                + "on the result runs the batch until the pack is out of makings. Roll it back up and "
                + "everything on it goes home."),
            text("FIELD FURNACE banks a few campfire embers in the pack and cooks as you walk - raw "
                + "ore and raw food only, so it never turns your cobblestone to stone behind your "
                + "back. It burns fuel out of the pack, a lump of coal for eight things cooked, and "
                + "if the finished piece won't fit, the raw one goes straight back."),
            text("PROVISIONER'S POUCH feeds you before hunger bites, and it eats the CHEAPEST safe "
                + "thing in the pack first - your golden apples stay yours. It leaves anything with a "
                + "nasty effect well alone, and the bowl comes back in the pack."),
            text("TORCHBEARER'S LOOP sets a torch down from pack stock whenever you're standing "
                + "somewhere genuinely dark. It stops the moment the light comes up, and if the torch "
                + "can't stand there it goes back in the pack."),
            row("the field trades",
                trinket(TrinketType.CARTOGRAPHER), trinket(TrinketType.ANGLERS_CREEL),
                trinket(TrinketType.HERBALIST)),
            text("CARTOGRAPHER'S SLEEVE adds a Charts & Bearings compartment - maps, compasses, "
                + "clocks and the spyglass file themselves there instead of scattering through Tools. "
                + "ANGLER'S CREEL adds The Catch, and your catch drops straight into the pack instead "
                + "of bouncing off your chest. Pull either fitting and the compartment simply closes; "
                + "its items re-route, because a tab was only ever a filter."),
            text("HERBALIST'S BUNDLE replants a grown crop the instant you pull it, spending one seed "
                + "out of your own stock. If the ground is taken by the time it gets there, the seed "
                + "comes home."))),

        // =================================================================
        new Chapter("Tiers & Upgrades", List.of(
            row("Canvas to " + PackTier.top().getSerializedName(), allPacks()),
            text("The pack grows three ways: WIDER (more slots), DEEPER (each slot holds more), and "
                + "another trinket socket every step. Slots / stacks-per-slot / sockets by tier: "
                + java.util.Arrays.stream(PackTier.values()).map(HandbookContent::tierLine)
                    .collect(java.util.stream.Collectors.joining(" "))),
            text("DEPTH is the top of the ladder's real gift: a "
                + PackTier.top().getSerializedName().toUpperCase(java.util.Locale.ROOT) + " slot holds "
                + PackTier.top().slotDepth(64) + " of a common item - "
                + PackTier.top().step() + " whole stacks in one slot, sorted under one tab. Depth "
                + "lives entirely inside the pack: anything that LEAVES - your cursor, a hopper, a "
                + "fitting drawing from stock - always comes out one vanilla stack at a time, so the "
                + "world outside never sees an impossible stack."),
            text("Depth belongs to the TIER; extra slots belong to the BOTTOMLESS LINING. One axis "
                + "each: craft the material to hold more of everything, fit the lining to hold more "
                + "kinds of thing. They never overlap, so there's no double-dipping to puzzle over."),
            text("The ladder itself: sew a CANVAS pack - a chest wrapped in wool, its corners tied "
                + "with string. Every later tier is the same picture: set your pack in the MIDDLE "
                + "of the bench and build its ring around it - the tier's metal or stone on the "
                + "four edges, its fittings on the four corners. LEATHER ringed in leather with "
                + "copper buckles; STUDDED copper set with iron studs; REINFORCED gold cornered "
                + "with diamonds; RUNED diamond bound in netherite; and SCULKHIDE amethyst with "
                + "echo shards from the Deep Dark, where the hide was cured. All nine cells, "
                + "always - and turn the ring any way you like, the pack doesn't mind."),
            text("Upgrading is safe, always. The upgrade craft IS the recipe - a full pack plus "
                + "materials - and the result carries contents, layout, trinkets, name, and every "
                + "store (water, XP, charge, embers, vapors) straight up. A craft never eats "
                + "what's inside - pause, never punish."))),

        // =================================================================
        new Chapter("The Stores", List.of(
            text("One pack can carry more than items. Each extra store is a physical fitting you "
                + "craft and slot - never on by default - and shows as a glass gauge on the right rail."),
            row("fluids, XP, charge",
                trinket(TrinketType.WATERSKIN), trinket(TrinketType.SOUL_VIAL),
                trinket(TrinketType.CHARGE_CRYSTAL)),
            text("WATERSKIN RACK fits a fluid tank, from " + buckets(PackTier.CANVAS) + " on Canvas up "
                + "to " + buckets(PackTier.top()) + " on " + topName() + ". Click the glass gauge with "
                + "a bucket or flask on the cursor to fill or empty it. It speaks NeoForge's own fluid "
                + "capability, so any mod's pipes work against a placed pack."),
            text("SOUL VIAL stores experience - " + PackXpStore.capacityFor(pack(PackTier.CANVAS))
                + " points on Canvas up to " + PackXpStore.capacityFor(pack(PackTier.top()))
                + " on " + topName() + ". Click the green gauge to siphon your XP in, Shift-click to "
                + "pour it back, and it quietly mends your Mending-enchanted gear from the reservoir."),
            text("CHARGE CRYSTAL holds an arcane charge - standard FE in a copper-wound crystal, never "
                + "a battery - from " + PackEnergyStorage.capacityFor(pack(PackTier.CANVAS)) + " FE on "
                + "Canvas up to " + PackEnergyStorage.capacityFor(pack(PackTier.top())) + " on "
                + topName() + ". Any charger fills it, and it tops up the powered tools in your hands."),
            text("Running Forgework? The Charge Crystal also feeds any Forgework portable terminal "
                + "you're carrying, 1 Flux = 1 FE - the same arcane charge, poured into your "
                + "ender-gear. It does nothing without Forgework installed."),
            text("ALCHEMIST'S FLASK HARNESS racks bottled vapors - Mekanism chemicals in glass, never "
                + "a plasma tank. It only shows up with Mekanism installed."))));
}
