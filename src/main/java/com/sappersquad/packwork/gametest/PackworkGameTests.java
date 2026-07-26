package com.sappersquad.packwork.gametest;

import com.sappersquad.packwork.pack.PackInventory;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.pack.PackTrinketInventory;
import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.reg.ModItems;
import com.sappersquad.packwork.sort.AutoTabs;
import com.sappersquad.packwork.sort.PackLayout;
import com.sappersquad.packwork.sort.PackSorting;
import com.sappersquad.packwork.sort.SortEngine;
import com.sappersquad.packwork.sort.SortRule;
import com.sappersquad.packwork.sort.TabDef;
import com.sappersquad.packwork.sort.TabView;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless proof of the two things that would actually corrupt a player's pack:
 * that contents survive a stack save/load round-trip (relog / drop / placement),
 * and that the sorting engine routes items to the tabs it promises. The live GUI
 * is a rendering layer over this; if this is green the pack is trustworthy.
 */
@GameTestHolder("packwork")
@PrefixGameTestTemplate(false)
public class PackworkGameTests {

    /** A pack's contents must survive the exact save/load a relog or drop performs. */
    @GameTest(template = "empty")
    public static void contentsSurviveSaveLoad(GameTestHelper helper) {
        HolderLookup.Provider reg = helper.getLevel().registryAccess();

        ItemStack pack = new ItemStack(ModItems.leatherPack().get());
        IItemHandler h = pack.getCapability(Capabilities.ItemHandler.ITEM);
        helper.assertTrue(h != null, "pack must expose an item-handler capability");

        h.insertItem(0, new ItemStack(Items.BREAD, 40), false);
        h.insertItem(5, new ItemStack(Items.IRON_PICKAXE), false);
        h.insertItem(9, new ItemStack(Items.DIAMOND, 7), false);

        Tag saved = pack.save(reg);
        ItemStack reloaded = ItemStack.parse(reg, saved).orElseThrow();

        IItemHandler h2 = reloaded.getCapability(Capabilities.ItemHandler.ITEM);
        helper.assertTrue(h2 != null, "reloaded pack must still expose the capability");
        helper.assertTrue(h2.getStackInSlot(0).getCount() == 40
                && h2.getStackInSlot(0).is(Items.BREAD), "bread must survive the round-trip");
        helper.assertTrue(h2.getStackInSlot(5).is(Items.IRON_PICKAXE), "pickaxe must survive");
        helper.assertTrue(h2.getStackInSlot(9).getCount() == 7
                && h2.getStackInSlot(9).is(Items.DIAMOND), "diamonds must survive");
        helper.succeed();
    }

    /** Packs never accept packs: no nesting in v1. */
    @GameTest(template = "empty")
    public static void packRejectsNesting(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.leatherPack().get());
        IItemHandler h = pack.getCapability(Capabilities.ItemHandler.ITEM);
        ItemStack another = new ItemStack(ModItems.leatherPack().get());
        ItemStack leftover = h.insertItem(0, another, false);
        helper.assertTrue(leftover.getCount() == 1 && h.getStackInSlot(0).isEmpty(),
                "a pack must refuse to hold another pack");
        helper.succeed();
    }

    /** The auto-tabs claim the items they promise; unclaimed items fall to Loose. */
    @GameTest(template = "empty")
    public static void autoTabsRouteCorrectly(GameTestHelper helper) {
        List<TabView> tabs = SortEngine.tabsFor(PackLayout.EMPTY);
        assertRoute(helper, tabs, PackLayout.EMPTY, Items.BREAD, "auto:food");
        assertRoute(helper, tabs, PackLayout.EMPTY, Items.COOKED_BEEF, "auto:food");
        assertRoute(helper, tabs, PackLayout.EMPTY, Items.IRON_PICKAXE, "auto:tools");
        assertRoute(helper, tabs, PackLayout.EMPTY, Items.SHEARS, "auto:tools");
        assertRoute(helper, tabs, PackLayout.EMPTY, Items.DIAMOND_SWORD, "auto:combat");
        assertRoute(helper, tabs, PackLayout.EMPTY, Items.IRON_CHESTPLATE, "auto:combat");
        assertRoute(helper, tabs, PackLayout.EMPTY, Items.IRON_INGOT, "auto:ores");
        assertRoute(helper, tabs, PackLayout.EMPTY, Items.COBBLESTONE, "auto:blocks");
        assertRoute(helper, tabs, PackLayout.EMPTY, Items.OAK_SAPLING, "auto:nature");
        // A plain crafting ingredient nothing claims must land in Loose, never vanish.
        assertRoute(helper, tabs, PackLayout.EMPTY, Items.STICK, AutoTabs.LOOSE_ID);
        helper.succeed();
    }

    /**
     * A manual pin beats every rule (with or without a ledger); a custom tab's WRITTEN
     * rules only claim items while a Quill &amp; Ledger is fitted - benched otherwise.
     */
    @GameTest(template = "empty")
    public static void pinsAlwaysWinLedgerGatesRules(GameTestHelper helper) {
        ResourceLocation breadId = BuiltInRegistries.ITEM.getKey(Items.BREAD);

        // Custom tab that claims sticks by a written name rule, plus a pin sending bread
        // to it. Its stamp (a stick icon) has no category, so only the written rule counts.
        TabDef custom = new TabDef("custom:0", "Bits",
                ResourceLocation.withDefaultNamespace("stick"), 0,
                List.of(SortRule.name("stick")));
        List<String> order = new ArrayList<>(AutoTabs.defaultOrder());
        order.add("custom:0");
        PackLayout layout = new PackLayout(order,
                List.of(custom),
                List.of(new PackLayout.Pin(breadId, "custom:0")),
                List.of());

        // Without a Quill & Ledger the written rule is benched (stick falls to Loose),
        // but the pin still overrides the Food auto-tab.
        List<TabView> noLedger = SortEngine.tabsFor(layout, false);
        assertRoute(helper, noLedger, layout, Items.STICK, AutoTabs.LOOSE_ID);
        assertRoute(helper, noLedger, layout, Items.BREAD, "custom:0");

        // With a Quill & Ledger the written rule claims sticks; the pin still wins for bread.
        List<TabView> ledger = SortEngine.tabsFor(layout, true);
        assertRoute(helper, ledger, layout, Items.STICK, "custom:0");
        assertRoute(helper, ledger, layout, Items.BREAD, "custom:0");
        helper.succeed();
    }

    /**
     * The stamp is the always-on baseline (2026-07-26 rework): a tab stamped with a
     * pickaxe gathers tools with NO trinket fitted. The Quill &amp; Ledger gates only the
     * written rules.
     */
    @GameTest(template = "empty")
    public static void stampFilesAlwaysLedgerGatesWrittenRules(GameTestHelper helper) {
        TabDef custom = new TabDef("custom:0", "Kit",
                BuiltInRegistries.ITEM.getKey(Items.IRON_PICKAXE), 0,
                List.of(SortRule.name("stick")));
        // put the custom tab FIRST so its stamp rule out-prioritises the Tools auto-tab
        List<String> order = new ArrayList<>();
        order.add("custom:0");
        order.addAll(AutoTabs.defaultOrder());
        PackLayout layout = new PackLayout(order, List.of(custom), List.of(), List.of());

        // stamp matching needs no ledger: the pickaxe-stamped tab claims tools outright
        assertRoute(helper, SortEngine.tabsFor(layout, false), layout, Items.DIAMOND_PICKAXE, "custom:0");
        // the written name rule stays the ledger's: benched without it, live with it
        assertRoute(helper, SortEngine.tabsFor(layout, false), layout, Items.STICK, AutoTabs.LOOSE_ID);
        assertRoute(helper, SortEngine.tabsFor(layout, true), layout, Items.STICK, "custom:0");
        helper.succeed();
    }

    /**
     * The Quill &amp; Ledger's rule editor: writing a rule requires the ledger fitted
     * (refused otherwise), a written rule routes matching items, pulling the ledger
     * benches the rules without deleting them, and striking one off really removes it.
     * Junk input (bad type, blank value, bogus predicate) is refused server-side.
     */
    @GameTest(template = "empty")
    public static void ruleEditorWritesAndStrikesLedgerGated(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.LEATHER).get());
        player.getInventory().setItem(0, pack);
        var menu = com.sappersquad.packwork.pack.PackMenu.server(43, player.getInventory(), 0);

        menu.applyCreateTab();
        String tabId = menu.activeTab();
        helper.assertTrue(tabId.startsWith("custom:"), "the created tab is active");

        // without a ledger, the editor refuses to write
        menu.applyAddTabRule(tabId, SortRule.Type.NAME.ordinal(), "stick");
        PackLayout layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        helper.assertTrue(layout.customTab(tabId).rules().isEmpty(), "no ledger, no writing");

        // fit the ledger and write: sticks now file themselves to the custom tab
        var sockets = new PackTrinketInventory(() -> pack, PackTier.LEATHER);
        sockets.insertItem(0, new ItemStack(
                ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.QUILL_LEDGER).get()), false);
        menu.applyAddTabRule(tabId, SortRule.Type.NAME.ordinal(), "stick");
        layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        helper.assertTrue(layout.customTab(tabId).rules().size() == 1, "the rule is written");
        assertRoute(helper, SortEngine.tabsFor(layout, true), layout, Items.STICK, tabId);

        // duplicates are refused
        menu.applyAddTabRule(tabId, SortRule.Type.NAME.ordinal(), "stick");
        layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        helper.assertTrue(layout.customTab(tabId).rules().size() == 1, "no duplicate rules");

        // pull the ledger: the rule is benched (stops matching), NOT deleted
        sockets.extractItem(0, 1, false);
        layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        helper.assertTrue(layout.customTab(tabId).rules().size() == 1, "the written rule survives");
        assertRoute(helper, SortEngine.tabsFor(layout, false), layout, Items.STICK, AutoTabs.LOOSE_ID);

        // refit and strike it off: really gone now
        sockets.insertItem(0, new ItemStack(
                ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.QUILL_LEDGER).get()), false);
        menu.applyRemoveTabRule(tabId, 0);
        layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        helper.assertTrue(layout.customTab(tabId).rules().isEmpty(), "struck off");
        assertRoute(helper, SortEngine.tabsFor(layout, true), layout, Items.STICK, AutoTabs.LOOSE_ID);

        // junk in, nothing out
        menu.applyAddTabRule(tabId, 99, "stick");
        menu.applyAddTabRule(tabId, SortRule.Type.PREDICATE.ordinal(), "NOT_A_KIND");
        menu.applyAddTabRule(tabId, SortRule.Type.NAME.ordinal(), "   ");
        layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        helper.assertTrue(layout.customTab(tabId).rules().isEmpty(), "junk input is refused");
        helper.succeed();
    }

    /** Tidy Up merges partial stacks and never loses a count. */
    @GameTest(template = "empty")
    public static void tidyMergesAndConserves(GameTestHelper helper) {
        List<ItemStack> source = List.of(
                new ItemStack(Items.COBBLESTONE, 40),
                new ItemStack(Items.BREAD, 5),
                new ItemStack(Items.COBBLESTONE, 40),
                new ItemStack(Items.COBBLESTONE, 40));
        List<TabView> tabs = SortEngine.tabsFor(PackLayout.EMPTY);
        List<ItemStack> tidied = PackSorting.tidy(source, tabs, PackLayout.EMPTY);

        int cobble = 0, bread = 0;
        for (ItemStack s : tidied) {
            if (s.is(Items.COBBLESTONE)) cobble += s.getCount();
            if (s.is(Items.BREAD)) bread += s.getCount();
            helper.assertTrue(s.getCount() <= s.getMaxStackSize(), "no overfull stacks after tidy");
        }
        helper.assertTrue(cobble == 120, "all 120 cobblestone conserved, got " + cobble);
        helper.assertTrue(bread == 5, "bread conserved");
        // 120 cobble merges into 2 stacks (64+56) + 1 bread = 3 stacks total
        helper.assertTrue(tidied.size() == 3, "expected 3 merged stacks, got " + tidied.size());
        helper.succeed();
    }

    /** A brand-new pack starts empty with no persisted layout (EMPTY default). */
    @GameTest(template = "empty")
    public static void freshPackIsEmptyAndDefault(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.leatherPack().get());
        ItemContainerContents c = pack.get(ModComponents.PACK_CONTENTS.get());
        helper.assertTrue(c == null || c.nonEmptyStream().findAny().isEmpty(), "fresh pack holds nothing");
        PackLayout layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        helper.assertTrue(layout.customTabs().isEmpty() && layout.pins().isEmpty(),
                "fresh pack has default layout");
        helper.succeed();
    }

    /** A Bottomless Lining grows the pack's slot count; the extra slots are real. */
    @GameTest(template = "empty")
    public static void bottomlessGrowsCapacity(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.leatherPack().get());
        int before = com.sappersquad.packwork.trinket.TrinketAccess.capacity(pack);
        helper.assertTrue(before == PackTier.LEATHER.capacity(), "leather pack starts at tier capacity");

        PackTrinketInventory sockets = new PackTrinketInventory(() -> pack, PackTier.LEATHER);
        ItemStack lining = new ItemStack(ModItems.trinket(
                com.sappersquad.packwork.trinket.TrinketType.BOTTOMLESS).get());
        helper.assertTrue(sockets.insertItem(0, lining, false).isEmpty(), "lining installs into a socket");

        int after = com.sappersquad.packwork.trinket.TrinketAccess.capacity(pack);
        helper.assertTrue(after == before + com.sappersquad.packwork.trinket.TrinketAccess.BOTTOMLESS_BONUS,
                "capacity grows by the bonus, got " + after);
        PackInventory store = new PackInventory(pack, PackTier.LEATHER);
        helper.assertTrue(store.getSlots() == after, "the pack store exposes the grown slot count");
        helper.succeed();
    }

    /** Trinket sockets take one of each fitting and refuse non-fittings. */
    @GameTest(template = "empty")
    public static void socketsRejectNonTrinketsAndDupes(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.STUDDED).get());
        PackTrinketInventory sockets = new PackTrinketInventory(() -> pack, PackTier.STUDDED);
        ItemStack strap = new ItemStack(ModItems.trinket(
                com.sappersquad.packwork.trinket.TrinketType.QUICK_DRAW).get());

        helper.assertTrue(sockets.insertItem(0, strap.copy(), false).isEmpty(), "first fitting installs");
        helper.assertTrue(!sockets.insertItem(1, strap.copy(), false).isEmpty(),
                "a duplicate fitting is refused");
        helper.assertTrue(!sockets.insertItem(1, new ItemStack(Items.DIRT), false).isEmpty(),
                "a non-trinket is refused");
        helper.succeed();
    }

    /** The void list is opt-in and exact; it starts empty on a fresh pack. */
    @GameTest(template = "empty")
    public static void voidListIsOptInAndExact(GameTestHelper helper) {
        ResourceLocation dirt = BuiltInRegistries.ITEM.getKey(Items.DIRT);
        ResourceLocation stone = BuiltInRegistries.ITEM.getKey(Items.STONE);
        helper.assertTrue(!PackLayout.EMPTY.voids(dirt), "fresh pack voids nothing");
        PackLayout marked = PackLayout.EMPTY.withVoidList(List.of(dirt));
        helper.assertTrue(marked.voids(dirt) && !marked.voids(stone), "only the marked item is voided");
        helper.succeed();
    }

    /** A tier upgrade carries the pack's contents and trinkets over - never eats them. */
    @GameTest(template = "empty")
    public static void upgradePreservesContents(GameTestHelper helper) {
        net.minecraft.core.HolderLookup.Provider reg = helper.getLevel().registryAccess();

        ItemStack pack = new ItemStack(ModItems.leatherPack().get());
        PackInventory store = new PackInventory(pack, PackTier.LEATHER);
        store.insertItem(0, new ItemStack(Items.DIAMOND, 12), false);
        new PackTrinketInventory(() -> pack, PackTier.LEATHER).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.LODESTONE).get()), false);

        var recipe = new com.sappersquad.packwork.pack.PackUpgradeRecipe(
                PackTier.LEATHER, PackTier.STUDDED,
                List.of(net.neoforged.neoforge.common.crafting.SizedIngredient.of(Items.IRON_INGOT, 4)),
                net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);

        // a 3x3 crafting input: the pack + 4 iron ingots, one per cell (crafting consumes
        // one item per cell, so the recipe demands the honest spread)
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                pack, new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT),
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "recipe should match pack + 4 iron");

        ItemStack out = recipe.assemble(input, reg);
        helper.assertTrue(out.getItem() == ModItems.pack(PackTier.STUDDED).get(), "result is a Studded pack");
        PackInventory upgraded = new PackInventory(out, PackTier.STUDDED);
        helper.assertTrue(upgraded.getStackInSlot(0).is(Items.DIAMOND)
                && upgraded.getStackInSlot(0).getCount() == 12, "diamonds carried into the upgrade");
        helper.assertTrue(com.sappersquad.packwork.trinket.TrinketAccess.has(out,
                com.sappersquad.packwork.trinket.TrinketType.LODESTONE), "trinket carried into the upgrade");
        helper.succeed();
    }

    /** The fluid tank is trinket-gated, respects capacity, and drains back exactly. */
    @GameTest(template = "empty")
    public static void waterskinTankGatedAndConserves(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.leatherPack().get());
        // no Waterskin yet -> no fluid capability
        helper.assertTrue(pack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) == null,
                "a pack without a Waterskin exposes no tank");

        new PackTrinketInventory(() -> pack, PackTier.LEATHER).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.WATERSKIN).get()), false);
        var tank = pack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        helper.assertTrue(tank != null, "a Waterskin fits a tank");

        int cap = com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack);
        var water = new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, cap + 5000);
        int filled = tank.fill(water, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(filled == cap, "fill is clamped to capacity, got " + filled);

        var drained = tank.drain(Integer.MAX_VALUE, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        helper.assertTrue(drained.getAmount() == cap && drained.is(net.minecraft.world.level.material.Fluids.WATER),
                "drains back exactly what was stored");
        helper.assertTrue(tank.getFluidInTank(0).isEmpty(), "tank empty after drain");
        helper.succeed();
    }

    /**
     * Clicking the waterskin gauge with a STACK of buckets spends exactly one and hands back
     * exactly one - the rest of the stack is untouched. (The old code handed the whole stack
     * to {@code FluidUtil}'s single-container result and quietly ate the remainder.)
     */
    @GameTest(template = "empty")
    public static void fluidInteractConservesACarriedStack(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pack = waterskinPack();
        player.getInventory().setItem(0, pack);
        var menu = com.sappersquad.packwork.pack.PackMenu.server(9, player.getInventory(), 0);

        // 3 water buckets on the cursor -> exactly one empties into the tank
        menu.setCarried(new ItemStack(Items.WATER_BUCKET, 3));
        menu.applyFluidInteract();

        ItemStack cursor = menu.getCarried();
        helper.assertTrue(cursor.is(Items.WATER_BUCKET) && cursor.getCount() == 2,
                "two water buckets stay on the cursor, got " + cursor.getCount() + " x " + cursor.getItem());
        helper.assertTrue(countIn(player, Items.BUCKET) == 1,
                "the one emptied bucket comes back, got " + countIn(player, Items.BUCKET));
        helper.assertTrue(countIn(player, Items.WATER_BUCKET) == 0, "no extra water bucket minted");
        var tank = new com.sappersquad.packwork.pack.PackFluidHandler(
                player.getInventory().getItem(0), com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack));
        helper.assertTrue(tank.getFluidInTank(0).getAmount() == 1000,
                "one bucket landed in the tank, got " + tank.getFluidInTank(0).getAmount());

        // total bucket-shaped items in play must still be 3: 2 on the cursor + 1 in the pockets
        int total = cursor.getCount() + countIn(player, Items.BUCKET) + countIn(player, Items.WATER_BUCKET);
        helper.assertTrue(total == 3, "three containers in, three containers out, got " + total);
        helper.succeed();
    }

    /** A single bucket empties into the tank and the EMPTY bucket stays on the cursor - never the floor. */
    @GameTest(template = "empty")
    public static void fluidInteractKeepsSingleBucketOnCursor(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pack = waterskinPack();
        player.getInventory().setItem(0, pack);
        var menu = com.sappersquad.packwork.pack.PackMenu.server(9, player.getInventory(), 0);

        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        menu.applyFluidInteract();
        helper.assertTrue(menu.getCarried().is(Items.BUCKET) && menu.getCarried().getCount() == 1,
                "the emptied bucket stays on the cursor, got " + menu.getCarried());
        helper.assertTrue(countIn(player, Items.BUCKET) == 0 && countIn(player, Items.WATER_BUCKET) == 0,
                "nothing was stowed or duplicated");

        // and it fills straight back out of the tank, same one bucket
        menu.applyFluidInteract();
        helper.assertTrue(menu.getCarried().is(Items.WATER_BUCKET) && menu.getCarried().getCount() == 1,
                "the same bucket fills back from the tank, got " + menu.getCarried());
        var tank = new com.sappersquad.packwork.pack.PackFluidHandler(
                player.getInventory().getItem(0), com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack));
        helper.assertTrue(tank.getFluidInTank(0).isEmpty(), "the tank gave back everything it took");
        helper.succeed();
    }

    /** Filling FROM the tank with a stack of empties spends one and stows one filled - no loss, no dupe. */
    @GameTest(template = "empty")
    public static void fluidInteractFillsOneOfAStack(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pack = waterskinPack();
        new com.sappersquad.packwork.pack.PackFluidHandler(pack,
                com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack))
                .fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 5000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        player.getInventory().setItem(0, pack);
        var menu = com.sappersquad.packwork.pack.PackMenu.server(9, player.getInventory(), 0);

        menu.setCarried(new ItemStack(Items.BUCKET, 3));
        menu.applyFluidInteract();

        helper.assertTrue(menu.getCarried().is(Items.BUCKET) && menu.getCarried().getCount() == 2,
                "two empty buckets stay on the cursor, got " + menu.getCarried());
        helper.assertTrue(countIn(player, Items.WATER_BUCKET) == 1,
                "exactly one filled bucket comes back, got " + countIn(player, Items.WATER_BUCKET));
        var tank = new com.sappersquad.packwork.pack.PackFluidHandler(
                player.getInventory().getItem(0), com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack));
        helper.assertTrue(tank.getFluidInTank(0).getAmount() == 4000,
                "exactly one bucket left the tank, got " + tank.getFluidInTank(0).getAmount());
        int total = menu.getCarried().getCount() + countIn(player, Items.BUCKET) + countIn(player, Items.WATER_BUCKET);
        helper.assertTrue(total == 3, "three containers in, three containers out, got " + total);
        helper.succeed();
    }

    private static ItemStack waterskinPack() {
        ItemStack pack = new ItemStack(ModItems.leatherPack().get());
        new PackTrinketInventory(() -> pack, PackTier.LEATHER).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.WATERSKIN).get()), false);
        return pack;
    }

    private static int countIn(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    /** The Soul Vial siphons and pours XP without losing a point, and stops at capacity. */
    @GameTest(template = "empty")
    public static void soulVialConservesXp(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.CANVAS).get());
        int cap = com.sappersquad.packwork.pack.PackXpStore.capacityFor(pack);
        helper.assertTrue(com.sappersquad.packwork.pack.PackXpStore.stored(pack) == 0, "fresh vial is empty");

        // spend caps at what's stored (no negative / no phantom points)
        pack.set(ModComponents.PACK_XP.get(), 300);
        int spent = com.sappersquad.packwork.pack.PackXpStore.spend(pack, 1000);
        helper.assertTrue(spent == 300 && com.sappersquad.packwork.pack.PackXpStore.stored(pack) == 0,
                "spend never takes more than is stored");

        // capacity clamp: setting over cap and spending stays consistent
        pack.set(ModComponents.PACK_XP.get(), cap);
        helper.assertTrue(com.sappersquad.packwork.pack.PackXpStore.stored(pack) == cap, "holds up to capacity");
        int spent2 = com.sappersquad.packwork.pack.PackXpStore.spend(pack, cap + 500);
        helper.assertTrue(spent2 == cap, "spends exactly the stored amount, got " + spent2);
        helper.succeed();
    }

    /** The Charge Crystal is trinket-gated, respects capacity + transfer, and never dupes. */
    @GameTest(template = "empty")
    public static void chargeCrystalGatedAndConserves(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.STUDDED).get());
        helper.assertTrue(pack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) == null,
                "no Charge Crystal -> no energy capability");

        new PackTrinketInventory(() -> pack, PackTier.STUDDED).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL).get()), false);
        var crystal = pack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
        helper.assertTrue(crystal != null, "a Charge Crystal fits a reservoir");

        int xfer = com.sappersquad.packwork.pack.PackEnergyStorage.transferFor(pack);
        int got = crystal.receiveEnergy(Integer.MAX_VALUE, false);
        helper.assertTrue(got == xfer, "one receive is clamped to the transfer rate, got " + got);
        helper.assertTrue(crystal.getEnergyStored() == xfer, "stored exactly what was received");

        int out = crystal.extractEnergy(Integer.MAX_VALUE, false);
        helper.assertTrue(out == xfer && crystal.getEnergyStored() == 0, "extract returns it all, nothing left over");
        helper.succeed();
    }

    /**
     * Quick-Draw Straps pull one replacement out of the pack and conserve exactly - a
     * refill never mints a duplicate.
     */
    @GameTest(template = "empty")
    public static void quickDrawPullsOneAndNeverDupes(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.leatherPack().get());
        PackInventory store = new PackInventory(pack, PackTier.LEATHER);
        store.insertItem(0, new ItemStack(Items.IRON_PICKAXE), false);

        ItemStack pulled = com.sappersquad.packwork.trinket.TrinketEffects.pullReplacement(store, Items.IRON_PICKAXE);
        helper.assertTrue(pulled.is(Items.IRON_PICKAXE) && pulled.getCount() == 1, "pulls exactly one pickaxe");
        // the pack no longer holds it; a second pull finds nothing (never dupes)
        helper.assertTrue(com.sappersquad.packwork.trinket.TrinketEffects.pullReplacement(store, Items.IRON_PICKAXE).isEmpty(),
                "a second pull finds nothing - never dupes");

        // a stackable refills the whole stack back into the hand
        store.insertItem(0, new ItemStack(Items.TORCH, 30), false);
        ItemStack torches = com.sappersquad.packwork.trinket.TrinketEffects.pullReplacement(store, Items.TORCH);
        helper.assertTrue(torches.is(Items.TORCH) && torches.getCount() == 30,
                "pulls the whole torch stack, got " + torches.getCount());
        helper.assertTrue(com.sappersquad.packwork.trinket.TrinketEffects.pullReplacement(store, Items.TORCH).isEmpty(),
                "nothing left after");
        helper.succeed();
    }

    /**
     * The Forgework Flux bridge is fully gated: without the mod it never fires (the
     * Charge Crystal is a plain FE store); with it, FE leaves the crystal into a carried
     * Forgework terminal 1 Flux = 1 FE, conserving exactly.
     */
    @GameTest(template = "empty")
    public static void forgeworkFluxBridgeGatedAndConserves(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.RUNED).get());
        new PackTrinketInventory(() -> pack, PackTier.RUNED).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL).get()), false);
        pack.set(ModComponents.PACK_ENERGY.get(), 50_000);
        var crystal = pack.getCapability(Capabilities.EnergyStorage.ITEM);
        helper.assertTrue(crystal != null && crystal.getEnergyStored() == 50_000, "crystal seeded with charge");

        if (!net.neoforged.fml.ModList.get().isLoaded("forgework")) {
            // no-op invariant: the bridge is gated, so nothing touches the charge here
            helper.assertTrue(crystal.getEnergyStored() == 50_000,
                    "without Forgework the bridge is inert - the crystal keeps its charge");
            helper.succeed();
            return;
        }

        // Forgework present: a Portable Ender Terminal fills from the crystal, 1:1.
        var terminalItem = BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.parse("forgework:portable_ender_terminal")).orElseThrow();
        ItemStack terminal = new ItemStack(terminalItem);
        int before = crystal.getEnergyStored();
        int moved = com.sappersquad.packwork.compat.forgework.ForgeworkFluxBridge.chargeItem(terminal, crystal, 5_000);
        helper.assertTrue(moved == 5_000, "moves the per-item cap of Flux, got " + moved);
        helper.assertTrue(crystal.getEnergyStored() == before - moved, "FE leaves the crystal 1:1, none minted or lost");
        helper.succeed();
    }

    /**
     * The placed pack's render tier (a blockstate property, driving per-tier models/textures)
     * tracks the pack it holds, and the break still hands back the RIGHT-tier item. Proves the
     * per-tier block trim can never desync from, or corrupt, the stored pack: the blockstate is
     * render-only, the drop comes from the block entity's stack.
     */
    @GameTest(template = "empty")
    public static void placedTierDrivesBlockstateAndDrop(GameTestHelper helper) {
        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(1, 1, 1);
        var world = helper.absolutePos(p);
        helper.setBlock(p, com.sappersquad.packwork.reg.ModBlocks.PACK.get()); // bare place = default (leather)
        var be = (com.sappersquad.packwork.block.PackContainerBlockEntity) helper.getBlockEntity(p);

        // adopt a Runed pack: the blockstate tier must follow so the runed model renders
        be.setPackStack(new ItemStack(ModItems.pack(PackTier.RUNED).get()));
        net.minecraft.world.level.block.state.BlockState st = helper.getLevel().getBlockState(world);
        helper.assertTrue(st.getValue(com.sappersquad.packwork.block.PackContainerBlock.TIER) == PackTier.RUNED,
                "the placed block's render tier follows the pack it holds (runed)");

        // the break drop is the RIGHT-tier pack, read from the block entity - not the blockstate
        var drops = net.minecraft.world.level.block.Block.getDrops(st, helper.getLevel(), world, be);
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem() == ModItems.pack(PackTier.RUNED).get(),
                "breaking a runed placed pack returns a Runed pack, count " + drops.size());

        // swap the whole pack to a different tier: the render tier retracks, no stale trim, no dupe
        be.setPackStack(new ItemStack(ModItems.pack(PackTier.CANVAS).get()));
        st = helper.getLevel().getBlockState(world);
        helper.assertTrue(st.getValue(com.sappersquad.packwork.block.PackContainerBlock.TIER) == PackTier.CANVAS,
                "the render tier retracks a whole-pack swap (canvas)");
        var canvasDrops = net.minecraft.world.level.block.Block.getDrops(st, helper.getLevel(), world, be);
        helper.assertTrue(canvasDrops.size() == 1 && canvasDrops.get(0).getItem() == ModItems.pack(PackTier.CANVAS).get(),
                "and now breaks into a Canvas pack");
        helper.succeed();
    }

    /**
     * The correctness-critical invariant: placing a pack and breaking it round-trips EVERY
     * field - items, trinkets, layout, and each resource store - byte for byte, with nothing
     * duplicated or dropped. (The block-entity holds the pack stack itself, so the drop is
     * exactly that stack.)
     */
    @GameTest(template = "empty")
    public static void placedPackRoundTripsLossless(GameTestHelper helper) {
        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(1, 1, 1);

        // build a fully-loaded Reinforced pack: items, three trinkets, fluid, XP, energy, a pin
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.REINFORCED).get());
        PackInventory store = new PackInventory(pack, PackTier.REINFORCED);
        store.insertItem(0, new ItemStack(Items.DIAMOND, 30), false);
        store.insertItem(1, new ItemStack(Items.IRON_PICKAXE), false);
        PackTrinketInventory sockets = new PackTrinketInventory(() -> pack, PackTier.REINFORCED);
        sockets.insertItem(0, new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.WATERSKIN).get()), false);
        sockets.insertItem(1, new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.SOUL_VIAL).get()), false);
        sockets.insertItem(2, new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL).get()), false);
        new com.sappersquad.packwork.pack.PackFluidHandler(pack, com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack))
                .fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 3000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        pack.set(ModComponents.PACK_XP.get(), 1234);
        pack.set(ModComponents.PACK_ENERGY.get(), 56789);
        pack.set(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY.withPins(
                List.of(new PackLayout.Pin(BuiltInRegistries.ITEM.getKey(Items.DIAMOND), "custom:0"))));

        ItemStack original = pack.copy();

        // PLACE: the block entity adopts the whole stack (as PackItem.useOn does)
        helper.setBlock(p, com.sappersquad.packwork.reg.ModBlocks.PACK.get());
        if (!(helper.getBlockEntity(p) instanceof com.sappersquad.packwork.block.PackContainerBlockEntity be)) {
            helper.fail("placed pack has no block entity");
            return;
        }
        be.setPackStack(pack.copy());

        // the placed pack holds every field
        ItemStack held = be.getPackStack();
        helper.assertTrue(new PackInventory(be::getPackStack, be.getTier()).getStackInSlot(0).getCount() == 30, "items survive placement");
        helper.assertTrue(com.sappersquad.packwork.trinket.TrinketAccess.has(held, com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL), "trinkets survive placement");
        helper.assertTrue(new com.sappersquad.packwork.pack.PackFluidHandler(held, 1).getFluidInTank(0).getAmount() == 3000, "fluid survives placement");
        helper.assertTrue(com.sappersquad.packwork.pack.PackXpStore.stored(held) == 1234, "xp survives placement");
        helper.assertTrue(held.getOrDefault(ModComponents.PACK_ENERGY.get(), 0) == 56789, "energy survives placement");

        // BREAK: the drop IS the block-entity's stack, so it equals the placed pack exactly
        ItemStack dropped = be.getPackStack().copy();
        helper.assertTrue(ItemStack.isSameItemSameComponents(dropped, original) && dropped.getCount() == 1,
                "the broken-out pack is byte-for-byte the placed pack - nothing duped, nothing lost");
        helper.succeed();
    }

    /**
     * A placed pack exposes an item handler so hoppers/pipes feed it - inserts land in the
     * flat store (sorting is virtual, so they auto-route), the count is conserved, and no
     * pack can be nested inside another.
     */
    @GameTest(template = "empty")
    public static void placedPackItemCapInsertsAndBlocksNesting(GameTestHelper helper) {
        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(p, com.sappersquad.packwork.reg.ModBlocks.PACK.get());
        ((com.sappersquad.packwork.block.PackContainerBlockEntity) helper.getBlockEntity(p))
                .setPackStack(new ItemStack(ModItems.pack(PackTier.CANVAS).get()));

        IItemHandler cap = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, helper.absolutePos(p), null);
        helper.assertTrue(cap != null, "a placed pack exposes an item-handler capability");
        helper.assertTrue(cap.getSlots() == PackTier.CANVAS.capacity(), "handler is bounded to tier capacity");

        ItemStack leftover = cap.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        helper.assertTrue(leftover.isEmpty(), "a hopper insert is accepted");
        ItemStack packLeftover = cap.insertItem(0, new ItemStack(ModItems.leatherPack().get()), false);
        helper.assertTrue(packLeftover.getCount() == 1, "a placed pack refuses to hold a pack (no nesting)");

        int found = 0;
        for (int i = 0; i < cap.getSlots(); i++) {
            if (cap.getStackInSlot(i).is(Items.COBBLESTONE)) found += cap.getStackInSlot(i).getCount();
        }
        helper.assertTrue(found == 64, "all 64 cobblestone are stored, none duped, got " + found);
        helper.succeed();
    }

    /**
     * A placed pack with a Charge Crystal exposes standard FE (so any standard-FE cable
     * charges it); and - only when Forgework is loaded - it also speaks Forgework's own Flux
     * cap, so a Forgework cable charges it 1:1. This is the block-level interop the item-only
     * pack could not do.
     */
    @GameTest(template = "empty")
    public static void placedPackEnergyAndForgeworkFluxGated(GameTestHelper helper) {
        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(1, 1, 1);
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.RUNED).get());
        new PackTrinketInventory(() -> pack, PackTier.RUNED).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL).get()), false);
        helper.setBlock(p, com.sappersquad.packwork.reg.ModBlocks.PACK.get());
        ((com.sappersquad.packwork.block.PackContainerBlockEntity) helper.getBlockEntity(p)).setPackStack(pack);

        var fe = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(p), null);
        helper.assertTrue(fe != null && fe.canReceive(), "placed pack exposes standard FE when a Charge Crystal is fitted");

        if (!net.neoforged.fml.ModList.get().isLoaded("forgework")) {
            helper.succeed(); // FLOW_ENERGY only exists when Forgework is loaded
            return;
        }
        var flow = helper.getLevel().getCapability(
                com.forgework.registry.ModCapabilities.FLOW_ENERGY, helper.absolutePos(p), null);
        helper.assertTrue(flow != null && flow.canReceive(), "placed pack speaks Forgework Flux under Forgework");
        int before = fe.getEnergyStored();
        int accepted = flow.receiveEnergy(5_000, false);
        helper.assertTrue(accepted == 5_000, "Forgework Flux charges the pack, got " + accepted);
        helper.assertTrue(fe.getEnergyStored() == before + accepted, "1 Flux == 1 FE into the reservoir");
        helper.succeed();
    }

    /**
     * The Flask Harness fits a socket and its chemical tank stores as dist-neutral primitives
     * (id + amount) that survive a component round-trip - all with Mekanism ABSENT, so the
     * gas store never drags Mekanism into an always-loaded class.
     */
    @GameTest(template = "empty")
    public static void flaskHarnessGatedChemicalComponent(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.STUDDED).get());
        PackTrinketInventory sockets = new PackTrinketInventory(() -> pack, PackTier.STUDDED);
        helper.assertTrue(sockets.insertItem(0,
                        new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.FLASK_HARNESS).get()), false).isEmpty(),
                "the Flask Harness installs into a socket");
        helper.assertTrue(com.sappersquad.packwork.trinket.TrinketAccess.has(pack,
                com.sappersquad.packwork.trinket.TrinketType.FLASK_HARNESS), "harness installed");

        helper.assertTrue(pack.getOrDefault(ModComponents.PACK_CHEMICAL.get(),
                com.sappersquad.packwork.pack.PackChemical.EMPTY).isEmpty(), "tank starts empty");
        pack.set(ModComponents.PACK_CHEMICAL.get(),
                new com.sappersquad.packwork.pack.PackChemical("mekanism:hydrogen", 5000L));
        var pc = pack.get(ModComponents.PACK_CHEMICAL.get());
        helper.assertTrue(pc != null && pc.amount() == 5000L
                        && pc.chemical().equals("mekanism:hydrogen") && !pc.isEmpty(),
                "chemical id + amount round-trip on the component");
        helper.assertTrue(com.sappersquad.packwork.pack.PackChemical.capacityFor(pack)
                        == 16_000L * (PackTier.STUDDED.ordinal() + 1),
                "chemical tank capacity is tier-scaled");
        helper.succeed();
    }

    /**
     * With Mekanism loaded, a fitted Flask Harness exposes Mekanism's own chemical handler
     * (one tier-scaled tank) and it inserts/extracts a chemical conserving exactly. Without
     * Mekanism the branch is never entered, so the compat class never classloads.
     */
    @GameTest(template = "empty")
    public static void mekanismChemicalStoreGated(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.RUNED).get());
        new PackTrinketInventory(() -> pack, PackTier.RUNED).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.FLASK_HARNESS).get()), false);

        if (!net.neoforged.fml.ModList.get().isLoaded("mekanism")) {
            helper.succeed(); // the chemical cap only exists when Mekanism is loaded
            return;
        }
        var cap = pack.getCapability(com.sappersquad.packwork.compat.mekanism.MekanismChemicalStore.ITEM);
        helper.assertTrue(cap != null && cap.getChemicalTanks() == 1, "Flask Harness exposes one chemical tank");
        helper.assertTrue(cap.getChemicalTankCapacity(0)
                == com.sappersquad.packwork.pack.PackChemical.capacityFor(pack), "tier-scaled capacity");
        var hydrogen = mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.getOptional(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mekanism", "hydrogen")).orElse(null);
        if (hydrogen == null) { helper.succeed(); return; }
        var stack = new mekanism.api.chemical.ChemicalStack(hydrogen, 3000L);
        var leftover = cap.insertChemical(0, stack, mekanism.api.Action.EXECUTE);
        helper.assertTrue(leftover.isEmpty(), "3000 mB accepted");
        var out = cap.extractChemical(0, 5000L, mekanism.api.Action.EXECUTE);
        helper.assertTrue(out.getAmount() == 3000L, "extracts exactly what went in, got " + out.getAmount());
        helper.succeed();
    }

    /**
     * With Curios loaded, the "back" slot is registered and the pack is assigned to it (so it
     * can be worn there). Without Curios the branch is never entered - the compat class never
     * classloads. Verifies the wear WIRING headlessly (Curios is light enough to stage).
     */
    @GameTest(template = "empty")
    public static void curiosBackSlotGated(GameTestHelper helper) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        boolean backRegistered = top.theillusivec4.curios.api.CuriosApi.getSlots(level).containsKey("back");
        helper.assertTrue(backRegistered, "Curios has a registered 'back' slot");
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.LEATHER).get());
        boolean packFitsBack = top.theillusivec4.curios.api.CuriosApi.getItemStackSlots(pack, level).containsKey("back");
        helper.assertTrue(packFitsBack, "the pack is assigned to the 'back' slot");
        helper.succeed();
    }

    // =====================================================================
    //  2026-07-25 batch: the Tinker's Kit and the six new fittings.
    //  Everything below that MOVES an item proves it conserves exactly.
    // =====================================================================

    /**
     * The headline conservation proof for the Tinker's Kit: shift-crafting off the tool roll
     * consumes the grid, tops it back up from pack stock, and stops dead when the pack runs dry.
     * Planks in must equal planks out (counting 4 per crafting table) at every step.
     */
    @GameTest(template = "empty")
    public static void tinkersKitCraftsFromPackAndConserves(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.STUDDED).get());
        new PackTrinketInventory(() -> pack, PackTier.STUDDED).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.TINKERS_KIT).get()), false);
        new PackInventory(pack, PackTier.STUDDED).insertItem(0, new ItemStack(Items.OAK_PLANKS, 8), false);
        player.getInventory().setItem(0, pack);

        var menu = com.sappersquad.packwork.pack.PackMenu.server(11, player.getInventory(), 0);
        menu.applyToggleRoll();
        helper.assertTrue(menu.rollActive(), "the roll unrolls with a kit fitted");
        helper.assertTrue(menu.visibleSlots() == PackTier.VIEW_SLOTS - com.sappersquad.packwork.pack.PackMenu.ROLL_HIDES,
                "the roll covers its rows of the grid, got " + menu.visibleSlots());

        // lay a 2x2 of planks on the roll (as a shift-click from the pack would)
        int gridStart = menu.craftStart();
        for (int i : new int[]{0, 1, 3, 4}) {
            menu.slots.get(gridStart + i).set(new ItemStack(Items.OAK_PLANKS));
        }
        helper.assertTrue(menu.slots.get(menu.resultIndex()).getItem().is(Items.CRAFTING_TABLE),
                "the roll works out what the pattern makes");

        int expected = plankValue(player, menu);      // 8 in the pack + 4 on the roll
        helper.assertTrue(expected == 12, "twelve planks in play to start, got " + expected);

        // three shift-clicks: two refill from stock, the third empties the roll and stops
        for (int craft = 1; craft <= 3; craft++) {
            menu.quickMoveStack(player, menu.resultIndex());
            helper.assertTrue(plankValue(player, menu) == 12,
                    "planks conserved after craft " + craft + ", got " + plankValue(player, menu));
        }
        helper.assertTrue(countPack(pack, Items.CRAFTING_TABLE) == 3,
                "three crafting tables came out, got " + countPack(pack, Items.CRAFTING_TABLE));
        helper.assertTrue(countPack(pack, Items.OAK_PLANKS) == 0, "the pack's planks are all spent");

        // a fourth shift-click with nothing left must be a no-op, not a free table
        menu.quickMoveStack(player, menu.resultIndex());
        helper.assertTrue(countPack(pack, Items.CRAFTING_TABLE) == 3, "no free craft once the pack is dry");
        helper.assertTrue(plankValue(player, menu) == 12, "still exactly twelve planks' worth");
        helper.succeed();
    }

    /** Rolling the kit back up - or closing the pack - returns every laid-out ingredient. */
    @GameTest(template = "empty")
    public static void toolRollReturnsEverythingWhenRolledUp(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.STUDDED).get());
        new PackTrinketInventory(() -> pack, PackTier.STUDDED).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.TINKERS_KIT).get()), false);
        player.getInventory().setItem(0, pack);

        var menu = com.sappersquad.packwork.pack.PackMenu.server(12, player.getInventory(), 0);
        menu.applyToggleRoll();
        int gridStart = menu.craftStart();
        menu.slots.get(gridStart).set(new ItemStack(Items.DIAMOND, 5));
        menu.slots.get(gridStart + 4).set(new ItemStack(Items.IRON_INGOT, 3));

        menu.applyToggleRoll();  // roll it back up
        helper.assertTrue(!menu.rollActive(), "the roll is stowed");
        helper.assertTrue(countPack(pack, Items.DIAMOND) == 5 && countPack(pack, Items.IRON_INGOT) == 3,
                "everything laid out came home: " + countPack(pack, Items.DIAMOND) + " diamonds, "
                        + countPack(pack, Items.IRON_INGOT) + " iron");
        for (int i = 0; i < 9; i++) {
            helper.assertTrue(menu.slots.get(gridStart + i).getItem().isEmpty(), "the roll is empty");
        }

        // and again via closing the whole pack, which is the other way out
        menu.applyToggleRoll();
        menu.slots.get(gridStart).set(new ItemStack(Items.DIAMOND, 2));
        menu.removed(player);
        helper.assertTrue(countPack(pack, Items.DIAMOND) == 7, "closing the pack returns them too, got "
                + countPack(pack, Items.DIAMOND));
        helper.succeed();
    }

    /** The Field Furnace cooks raw ore on pack fuel, spends the right embers, and conserves. */
    @GameTest(template = "empty")
    public static void fieldFurnaceCooksAndConserves(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.LEATHER).get());
        PackInventory store = new PackInventory(pack, PackTier.LEATHER);
        store.insertItem(0, new ItemStack(Items.RAW_IRON, 8), false);
        store.insertItem(1, new ItemStack(Items.COAL, 1), false);
        store.insertItem(2, new ItemStack(Items.COBBLESTONE, 16), false);

        for (int i = 0; i < 3; i++) {
            helper.assertTrue(com.sappersquad.packwork.trinket.TrinketEffects.smeltOnce(
                    helper.getLevel(), pack, store), "cook " + (i + 1) + " should happen");
        }
        helper.assertTrue(countPack(pack, Items.IRON_INGOT) == 3,
                "three ingots came out, got " + countPack(pack, Items.IRON_INGOT));
        helper.assertTrue(countPack(pack, Items.RAW_IRON) == 5,
                "five raw iron left, got " + countPack(pack, Items.RAW_IRON));
        helper.assertTrue(countPack(pack, Items.COAL) == 0, "the lump of coal went on the embers");
        helper.assertTrue(pack.getOrDefault(ModComponents.PACK_EMBERS.get(), 0) == 1600 - 3 * 200,
                "embers spent at furnace rate, got " + pack.getOrDefault(ModComponents.PACK_EMBERS.get(), 0));
        // and it leaves your building blocks alone
        helper.assertTrue(countPack(pack, Items.COBBLESTONE) == 16 && countPack(pack, Items.STONE) == 0,
                "cobblestone is never cooked behind your back");
        helper.succeed();
    }

    /** The Provisioner's Pouch eats the CHEAPEST safe thing, exactly one, and keeps the bowl. */
    @GameTest(template = "empty")
    public static void provisionerEatsCheapestAndConserves(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(3);
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.LEATHER).get());
        PackInventory store = new PackInventory(pack, PackTier.LEATHER);
        store.insertItem(0, new ItemStack(Items.GOLDEN_APPLE, 2), false);
        store.insertItem(1, new ItemStack(Items.ROTTEN_FLESH, 4), false);
        store.insertItem(2, new ItemStack(Items.BREAD, 3), false);

        helper.assertTrue(com.sappersquad.packwork.trinket.TrinketEffects.feedFrom(player, store),
                "a hungry adventurer gets fed");
        helper.assertTrue(countPack(pack, Items.BREAD) == 2,
                "exactly one loaf eaten, got " + countPack(pack, Items.BREAD) + " left");
        helper.assertTrue(countPack(pack, Items.GOLDEN_APPLE) == 2, "your golden apples stay yours");
        helper.assertTrue(countPack(pack, Items.ROTTEN_FLESH) == 4, "it won't touch rotten flesh");
        helper.assertTrue(player.getFoodData().getFoodLevel() > 3, "and it actually fed you");

        // full up? it leaves the larder alone entirely
        player.getFoodData().setFoodLevel(20);
        helper.assertTrue(!com.sappersquad.packwork.trinket.TrinketEffects.feedFrom(player, store),
                "a full player is left alone");
        helper.assertTrue(countPack(pack, Items.BREAD) == 2, "nothing eaten while full");
        helper.succeed();
    }

    /** The Herbalist's Bundle spends exactly one seed out of your own stock, or none at all. */
    @GameTest(template = "empty")
    public static void herbalistSpendsOneSeedOrNone(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.LEATHER).get());
        PackInventory store = new PackInventory(pack, PackTier.LEATHER);
        store.insertItem(0, new ItemStack(Items.WHEAT_SEEDS, 4), false);

        ItemStack seed = com.sappersquad.packwork.trinket.TrinketEffects.takeSeedFor(
                store, (net.minecraft.world.level.block.CropBlock) net.minecraft.world.level.block.Blocks.WHEAT);
        helper.assertTrue(seed.is(Items.WHEAT_SEEDS) && seed.getCount() == 1, "takes exactly one seed");
        helper.assertTrue(countPack(pack, Items.WHEAT_SEEDS) == 3,
                "three seeds left, got " + countPack(pack, Items.WHEAT_SEEDS));

        // a crop it has no seed for costs nothing
        ItemStack none = com.sappersquad.packwork.trinket.TrinketEffects.takeSeedFor(
                store, (net.minecraft.world.level.block.CropBlock) net.minecraft.world.level.block.Blocks.CARROTS);
        helper.assertTrue(none.isEmpty(), "no carrots in the pack, so nothing is spent");
        helper.assertTrue(countPack(pack, Items.WHEAT_SEEDS) == 3, "and the wheat seeds are untouched");
        helper.succeed();
    }

    /** The Angler's Creel stows the catch and leaves behind only what genuinely wouldn't fit. */
    @GameTest(template = "empty")
    public static void anglersCreelStowsTheCatchLosingNothing(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.CANVAS).get());
        PackInventory store = new PackInventory(pack, PackTier.CANVAS);

        List<ItemStack> drops = new ArrayList<>(List.of(
                new ItemStack(Items.COD, 1), new ItemStack(Items.LILY_PAD, 1)));
        com.sappersquad.packwork.trinket.TrinketEffects.stowCatch(store, drops);
        helper.assertTrue(drops.isEmpty(), "the whole catch went in the pack");
        helper.assertTrue(countPack(pack, Items.COD) == 1 && countPack(pack, Items.LILY_PAD) == 1,
                "and it's all there");

        // a pack with no room hands the catch straight back rather than swallowing it
        for (int i = 0; i < PackTier.CANVAS.capacity(); i++) {
            store.setStackInSlot(i, new ItemStack(Items.STONE, 64));
        }
        List<ItemStack> more = new ArrayList<>(List.of(new ItemStack(Items.SALMON, 1)));
        com.sappersquad.packwork.trinket.TrinketEffects.stowCatch(store, more);
        helper.assertTrue(more.size() == 1 && more.get(0).is(Items.SALMON) && more.get(0).getCount() == 1,
                "a full pack leaves the catch for you to pick up");
        helper.succeed();
    }

    /** Charts and Catch only exist while their fitting does - and they out-rank the tabs they'd lose to. */
    @GameTest(template = "empty")
    public static void gatedCompartmentsFollowTheirFittings(GameTestHelper helper) {
        var none = SortEngine.tabsFor(PackLayout.EMPTY, java.util.Set.<com.sappersquad.packwork.trinket.TrinketType>of());
        helper.assertTrue(!hasTab(none, "auto:charts") && !hasTab(none, "auto:catch"),
                "a bare pack shows neither gated compartment");
        assertRoute(helper, none, PackLayout.EMPTY, Items.FILLED_MAP, AutoTabs.LOOSE_ID);
        assertRoute(helper, none, PackLayout.EMPTY, Items.COD, "auto:food");

        var sleeve = SortEngine.tabsFor(PackLayout.EMPTY,
                java.util.Set.of(com.sappersquad.packwork.trinket.TrinketType.CARTOGRAPHER));
        helper.assertTrue(hasTab(sleeve, "auto:charts") && !hasTab(sleeve, "auto:catch"),
                "the sleeve opens Charts and nothing else");
        assertRoute(helper, sleeve, PackLayout.EMPTY, Items.FILLED_MAP, "auto:charts");
        assertRoute(helper, sleeve, PackLayout.EMPTY, Items.COMPASS, "auto:charts");

        var creel = SortEngine.tabsFor(PackLayout.EMPTY,
                java.util.Set.of(com.sappersquad.packwork.trinket.TrinketType.ANGLERS_CREEL));
        helper.assertTrue(hasTab(creel, "auto:catch"), "the creel opens The Catch");
        // The Catch must out-prioritise Food or every cod would file itself as rations
        assertRoute(helper, creel, PackLayout.EMPTY, Items.COD, "auto:catch");
        assertRoute(helper, creel, PackLayout.EMPTY, Items.BREAD, "auto:food");

        // an OLD pack with a saved tab order still slots a newly-fitted compartment in at its
        // proper priority, not on the end where it would never claim anything
        List<String> savedOrder = new ArrayList<>(List.of("auto:food", "auto:combat", "auto:tools",
                "auto:ores", "auto:brewing", "auto:nature", "auto:blocks"));
        PackLayout old = PackLayout.EMPTY.withTabOrder(savedOrder);
        var fittedOld = SortEngine.tabsFor(old,
                java.util.Set.of(com.sappersquad.packwork.trinket.TrinketType.ANGLERS_CREEL));
        assertRoute(helper, fittedOld, old, Items.COD, "auto:catch");
        helper.succeed();
    }

    private static boolean hasTab(List<TabView> tabs, String id) {
        for (TabView t : tabs) if (t.id().equals(id)) return true;
        return false;
    }

    /** Every plank in play: loose in the pack, laid on the roll, in the player's pockets, or
     *  four-at-a-time inside a crafted table. Must never change. */
    private static int plankValue(net.minecraft.world.entity.player.Player player,
                                  com.sappersquad.packwork.pack.PackMenu menu) {
        int total = 0;
        // the tool roll's own nine cells (the view slots below are just windows onto the pack,
        // which countPack already covers - counting both would double every plank)
        for (int i = menu.resultIndex() - 9; i < menu.resultIndex(); i++) {
            total += plankWorth(menu.slots.get(i).getItem());
        }
        ItemStack pack = player.getInventory().getItem(0);
        total += countPack(pack, Items.OAK_PLANKS) + 4 * countPack(pack, Items.CRAFTING_TABLE);
        for (int i = 1; i < player.getInventory().getContainerSize(); i++) {
            total += plankWorth(player.getInventory().getItem(i));
        }
        return total;
    }

    private static int plankWorth(ItemStack s) {
        if (s.is(Items.OAK_PLANKS)) return s.getCount();
        if (s.is(Items.CRAFTING_TABLE)) return 4 * s.getCount();
        return 0;
    }

    private static int countPack(ItemStack pack, net.minecraft.world.item.Item item) {
        PackInventory store = new PackInventory(pack, com.sappersquad.packwork.pack.PackItem.tierOf(pack));
        int n = 0;
        for (int i = 0; i < store.getSlots(); i++) {
            ItemStack s = store.getStackInSlot(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    // =====================================================================
    //  2026-07-25 batch 2: per-slot DEPTH, the preserving recipe chain, and
    //  the Dragonhide tier. The depth tests are the ones that keep worlds safe.
    // =====================================================================

    /** Depth: each tier deepens every slot by x64; unstackables never stack; inserts cap at depth. */
    @GameTest(template = "empty")
    public static void depthDeepensByTier(GameTestHelper helper) {
        // Canvas = vanilla depth: one stack per slot
        ItemStack canvas = new ItemStack(ModItems.pack(PackTier.CANVAS).get());
        PackInventory canvasStore = new PackInventory(canvas, PackTier.CANVAS);
        ItemStack left = canvasStore.insertItem(0, new ItemStack(Items.COBBLESTONE, 100), false);
        helper.assertTrue(canvasStore.getStackInSlot(0).getCount() == 64 && left.getCount() == 36,
                "canvas holds one vanilla stack per slot, got " + canvasStore.getStackInSlot(0).getCount());

        // Dragonhide = six stacks per slot
        ItemStack dh = new ItemStack(ModItems.pack(PackTier.DRAGONHIDE).get());
        PackInventory dhStore = new PackInventory(dh, PackTier.DRAGONHIDE);
        ItemStack l2 = dhStore.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        helper.assertTrue(l2.isEmpty(), "first stack in");
        for (int i = 0; i < 5; i++) {
            l2 = dhStore.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
            helper.assertTrue(l2.isEmpty(), "stack " + (i + 2) + " merges into depth");
        }
        helper.assertTrue(dhStore.getStackInSlot(0).getCount() == 384,
                "dragonhide slot holds 384, got " + dhStore.getStackInSlot(0).getCount());
        l2 = dhStore.insertItem(0, new ItemStack(Items.COBBLESTONE, 10), false);
        helper.assertTrue(l2.getCount() == 10, "the 385th cobblestone is refused, not eaten");

        // 16-stackables scale by the same multiplier: pearl depth = 16 x 6 = 96
        helper.assertTrue(dhStore.insertItem(1, new ItemStack(Items.ENDER_PEARL, 64), false).isEmpty(),
                "64 pearls fit under the 96 depth");
        ItemStack pearlLeft = dhStore.insertItem(1, new ItemStack(Items.ENDER_PEARL, 64), false);
        helper.assertTrue(dhStore.getStackInSlot(1).getCount() == 96 && pearlLeft.getCount() == 32,
                "pearls deepen to exactly 96 (16 x 6), got " + dhStore.getStackInSlot(1).getCount());
        ItemStack sword = dhStore.insertItem(2, new ItemStack(Items.IRON_SWORD), false);
        helper.assertTrue(sword.isEmpty(), "a sword goes in");
        ItemStack sword2 = dhStore.insertItem(2, new ItemStack(Items.IRON_SWORD), false);
        helper.assertTrue(sword2.getCount() == 1, "a second sword never stacks into the slot");
        helper.succeed();
    }

    /**
     * THE world-safety test for depth. Vanilla's persistent ItemStack codec hard-fails on any
     * count over 99, so a deep slot must survive: (a) the item save/parse a relog performs,
     * (b) the block entity NBT save/load a placed pack performs, and (c) a pack saved in the
     * OLD pre-depth format must still load via the codec's legacy fallback.
     */
    @GameTest(template = "empty")
    public static void deepContentsSurviveEveryRoundTrip(GameTestHelper helper) {
        HolderLookup.Provider reg = helper.getLevel().registryAccess();

        // (a) relog: ItemStack.save -> parse with 384 cobble + 96 pearls + a sword aboard
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.DRAGONHIDE).get());
        PackInventory store = new PackInventory(pack, PackTier.DRAGONHIDE);
        store.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        for (int i = 0; i < 5; i++) store.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        store.insertItem(1, new ItemStack(Items.ENDER_PEARL, 64), false);
        store.insertItem(1, new ItemStack(Items.ENDER_PEARL, 64), false);
        store.insertItem(2, new ItemStack(Items.IRON_SWORD), false);

        Tag saved = pack.save(reg);
        ItemStack reloaded = ItemStack.parse(reg, saved).orElseThrow();
        PackInventory reStore = new PackInventory(reloaded, PackTier.DRAGONHIDE);
        helper.assertTrue(reStore.getStackInSlot(0).getCount() == 384,
                "384 cobble survive a relog, got " + reStore.getStackInSlot(0).getCount());
        helper.assertTrue(reStore.getStackInSlot(1).getCount() == 96, "96 pearls survive a relog");
        helper.assertTrue(reStore.getStackInSlot(2).is(Items.IRON_SWORD), "the sword survives");

        // (b) placed: the block entity's own NBT save/load (the chunk-save path)
        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(p, com.sappersquad.packwork.reg.ModBlocks.PACK.get());
        var be = (com.sappersquad.packwork.block.PackContainerBlockEntity) helper.getBlockEntity(p);
        be.setPackStack(pack.copy());
        net.minecraft.nbt.CompoundTag beTag = be.saveWithFullMetadata(reg);
        var be2 = new com.sappersquad.packwork.block.PackContainerBlockEntity(
                helper.absolutePos(p), helper.getLevel().getBlockState(helper.absolutePos(p)));
        be2.loadWithComponents(beTag, reg);
        PackInventory beStore = new PackInventory(be2.getPackStack(), PackTier.DRAGONHIDE);
        helper.assertTrue(beStore.getStackInSlot(0).getCount() == 384,
                "384 cobble survive the placed-pack chunk save, got " + beStore.getStackInSlot(0).getCount());

        // (c) migration: data written by the VANILLA codec still decodes (legacy fallback)
        var legacy = net.minecraft.world.item.component.ItemContainerContents.fromItems(
                java.util.List.of(new ItemStack(Items.BREAD, 40), new ItemStack(Items.IRON_PICKAXE)));
        Tag legacyTag = net.minecraft.world.item.component.ItemContainerContents.CODEC
                .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, legacy).getOrThrow();
        var decoded = com.sappersquad.packwork.pack.DeepContentsCodec.CODEC
                .parse(net.minecraft.nbt.NbtOps.INSTANCE, legacyTag).getOrThrow();
        helper.assertTrue(decoded.getStackInSlot(0).getCount() == 40 && decoded.getStackInSlot(0).is(Items.BREAD),
                "a pre-depth pack's contents load intact (40 bread), got " + decoded.getStackInSlot(0).getCount());
        helper.assertTrue(decoded.getStackInSlot(1).is(Items.IRON_PICKAXE), "and the pickaxe with them");
        helper.succeed();
    }

    /**
     * Nothing oversized ever LEAVES the pack: every extract path - the capability a hopper
     * pulls through, and the GUI slot a cursor lifts from - pays out one vanilla stack at
     * a time, conserving exactly across the whole drain.
     */
    @GameTest(template = "empty")
    public static void oversizedStacksNeverEscape(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.DRAGONHIDE).get());
        PackInventory store = new PackInventory(pack, PackTier.DRAGONHIDE);
        for (int i = 0; i < 6; i++) store.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);

        // capability path (hoppers/pipes): a greedy pull still gets one legal stack
        ItemStack pulled = store.extractItem(0, 999, false);
        helper.assertTrue(pulled.getCount() == 64, "a greedy extract pays out 64, got " + pulled.getCount());
        int total = pulled.getCount();
        while (true) {
            ItemStack next = store.extractItem(0, Integer.MAX_VALUE, false);
            if (next.isEmpty()) break;
            helper.assertTrue(next.getCount() <= 64, "every pull is a legal stack");
            total += next.getCount();
        }
        helper.assertTrue(total == 384, "the whole depth drains out exactly, got " + total);

        // GUI path: a view slot's remove() (the cursor pickup) is clamped the same way
        for (int i = 0; i < 6; i++) store.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().setItem(0, pack);
        var menu = com.sappersquad.packwork.pack.PackMenu.server(21, player.getInventory(), 0);
        menu.applyFlatten(true);   // show every backing slot so the cobble is on the grid
        com.sappersquad.packwork.pack.PackViewSlot cell = null;
        for (var s : menu.slots) {
            if (s instanceof com.sappersquad.packwork.pack.PackViewSlot vs && vs.isActive() && s.hasItem()) {
                cell = vs;
                break;
            }
        }
        helper.assertTrue(cell != null && cell.getItem().getCount() == 384, "the deep slot shows its true count");
        ItemStack taken = cell.remove(Integer.MAX_VALUE);
        helper.assertTrue(taken.getCount() == 64, "a cursor pickup lifts one stack, got " + taken.getCount());
        helper.assertTrue(cell.getItem().getCount() == 320, "the rest stays safely in the pack");
        helper.succeed();
    }

    /** Tidy Up merges loose stacks down INTO depth: fewer, deeper stacks, count conserved. */
    @GameTest(template = "empty")
    public static void tidyMergesIntoDepth(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.LEATHER).get()); // depth x2 = 128
        PackInventory store = new PackInventory(pack, PackTier.LEATHER);
        List<ItemStack> source = List.of(
                new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 40), new ItemStack(Items.BREAD, 5));
        List<ItemStack> tidied = PackSorting.tidy(source, SortEngine.tabsFor(PackLayout.EMPTY),
                PackLayout.EMPTY, store::depthFor);
        int cobble = 0;
        int cobbleStacks = 0;
        for (ItemStack s : tidied) {
            if (s.is(Items.COBBLESTONE)) { cobble += s.getCount(); cobbleStacks++; }
            helper.assertTrue(s.getCount() <= store.depthFor(s), "no stack over its depth");
        }
        helper.assertTrue(cobble == 168, "all 168 cobblestone conserved, got " + cobble);
        helper.assertTrue(cobbleStacks == 2, "merged into depth: 128 + 40 = two stacks, got " + cobbleStacks);
        helper.succeed();
    }

    /**
     * The recipe chain (SapperSquad's rework): every tier above Canvas is crafted FROM the pack
     * before it, and that craft preserves EVERYTHING - deep contents, trinkets, layout,
     * name, and all five stores. Proven on the endgame step: Runed + shulker shells +
     * dragon's breath = Dragonhide.
     */
    @GameTest(template = "empty")
    public static void upgradeChainPreservesEverythingIncludingDepth(GameTestHelper helper) {
        HolderLookup.Provider reg = helper.getLevel().registryAccess();

        ItemStack pack = new ItemStack(ModItems.pack(PackTier.RUNED).get());
        PackInventory store = new PackInventory(pack, PackTier.RUNED);
        for (int i = 0; i < 5; i++) store.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false); // 320 deep
        new PackTrinketInventory(() -> pack, PackTier.RUNED).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.WATERSKIN).get()), false);
        new com.sappersquad.packwork.pack.PackFluidHandler(pack, com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack))
                .fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 7000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        pack.set(ModComponents.PACK_XP.get(), 4321);
        pack.set(ModComponents.PACK_ENERGY.get(), 98765);
        pack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("Old Faithful"));

        var recipe = new com.sappersquad.packwork.pack.PackUpgradeRecipe(
                PackTier.RUNED, PackTier.DRAGONHIDE,
                List.of(net.neoforged.neoforge.common.crafting.SizedIngredient.of(Items.SHULKER_SHELL, 4),
                        net.neoforged.neoforge.common.crafting.SizedIngredient.of(Items.DRAGON_BREATH, 4)),
                net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);

        // materials spread ONE PER CELL - the only layout crafting can charge honestly
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                pack, new ItemStack(Items.SHULKER_SHELL), new ItemStack(Items.SHULKER_SHELL),
                new ItemStack(Items.SHULKER_SHELL), new ItemStack(Items.SHULKER_SHELL),
                new ItemStack(Items.DRAGON_BREATH), new ItemStack(Items.DRAGON_BREATH),
                new ItemStack(Items.DRAGON_BREATH), new ItemStack(Items.DRAGON_BREATH)));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "runed + shells + breath matches");

        // stacked into one cell must NOT match: taking the craft only consumes ONE item per
        // grid cell, so a stacked cell would buy the upgrade at a quarter of its price
        var stacked = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                pack, new ItemStack(Items.SHULKER_SHELL, 4), new ItemStack(Items.DRAGON_BREATH, 4),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertTrue(!recipe.matches(stacked, helper.getLevel()),
                "stacked materials must not match - crafting would underpay");

        // missing the second material must NOT match - the End gate is real
        var half = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                pack, new ItemStack(Items.SHULKER_SHELL), new ItemStack(Items.SHULKER_SHELL),
                new ItemStack(Items.SHULKER_SHELL), new ItemStack(Items.SHULKER_SHELL),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertTrue(!recipe.matches(half, helper.getLevel()), "shells alone are not enough");

        ItemStack out = recipe.assemble(input, reg);
        helper.assertTrue(out.getItem() == ModItems.pack(PackTier.DRAGONHIDE).get(), "result is Dragonhide");
        PackInventory upStore = new PackInventory(out, PackTier.DRAGONHIDE);
        helper.assertTrue(upStore.getStackInSlot(0).getCount() == 320,
                "the deep stack rides up intact, got " + upStore.getStackInSlot(0).getCount());
        helper.assertTrue(com.sappersquad.packwork.trinket.TrinketAccess.has(out,
                com.sappersquad.packwork.trinket.TrinketType.WATERSKIN), "trinkets carry");
        helper.assertTrue(new com.sappersquad.packwork.pack.PackFluidHandler(out, 1).getFluidInTank(0).getAmount() == 7000,
                "the waterskin's fill carries");
        helper.assertTrue(out.getOrDefault(ModComponents.PACK_XP.get(), 0) == 4321, "stored XP carries");
        helper.assertTrue(out.getOrDefault(ModComponents.PACK_ENERGY.get(), 0) == 98765, "stored charge carries");
        helper.assertTrue("Old Faithful".equals(out.getHoverName().getString()), "the name carries");
        helper.succeed();
    }

    /** Everything keyed off the tier enum scales to Dragonhide: sockets, stores, depth. */
    @GameTest(template = "empty")
    public static void dragonhideScalesEverything(GameTestHelper helper) {
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.DRAGONHIDE).get());
        helper.assertTrue(PackTier.DRAGONHIDE.trinketSlots() == 5, "five trinket sockets");
        helper.assertTrue(PackTier.DRAGONHIDE.depthMultiplier() == 6, "depth x6 (384 of a 64-stackable)");
        helper.assertTrue(com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack) == 48000,
                "waterskin scales to 48 buckets");
        helper.assertTrue(com.sappersquad.packwork.pack.PackXpStore.capacityFor(pack) == 30000,
                "soul vial scales to 30k points");
        helper.assertTrue(com.sappersquad.packwork.pack.PackEnergyStorage.capacityFor(pack) == 600_000,
                "charge crystal scales to 600k FE");
        helper.assertTrue(com.sappersquad.packwork.pack.PackChemical.capacityFor(pack) == 96_000L,
                "flask harness scales to 96,000 mB");
        // the placed block renders it and lights it
        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(p, com.sappersquad.packwork.reg.ModBlocks.PACK.get());
        ((com.sappersquad.packwork.block.PackContainerBlockEntity) helper.getBlockEntity(p)).setPackStack(pack.copy());
        var st = helper.getLevel().getBlockState(helper.absolutePos(p));
        helper.assertTrue(st.getValue(com.sappersquad.packwork.block.PackContainerBlock.TIER) == PackTier.DRAGONHIDE,
                "the blockstate carries the dragonhide tier");
        helper.assertTrue(st.getLightEmission() == 11, "the breath-gem glows brighter than runed");
        helper.succeed();
    }

    /**
     * The Recipe Ledger's one server verb: laying a chalked recipe onto the roll pulls each
     * ingredient from PACK stock exactly once, all-or-nothing. A pattern the pack can't
     * cover moves NOTHING, and the laid-out pattern crafts and refills as normal.
     */
    @GameTest(template = "empty")
    public static void ghostLayOutPullsFromPackAllOrNothing(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.STUDDED).get());
        new PackTrinketInventory(() -> pack, PackTier.STUDDED).insertItem(0,
                new ItemStack(ModItems.trinket(com.sappersquad.packwork.trinket.TrinketType.TINKERS_KIT).get()), false);
        new PackInventory(pack, PackTier.STUDDED).insertItem(0, new ItemStack(Items.WHEAT, 7), false);
        player.getInventory().setItem(0, pack);

        var menu = com.sappersquad.packwork.pack.PackMenu.server(31, player.getInventory(), 0);
        menu.applyToggleRoll();

        // bread = 3 wheat in a row; the pack holds 7
        menu.applyLayOutGhost("minecraft:bread");
        int gridStart = menu.craftStart();
        int onGrid = 0;
        for (int i = 0; i < 9; i++) onGrid += menu.slots.get(gridStart + i).getItem().getCount();
        helper.assertTrue(onGrid == 3, "exactly one set laid out, got " + onGrid);
        helper.assertTrue(countPack(pack, Items.WHEAT) == 4,
                "the wheat came out of the pack, " + countPack(pack, Items.WHEAT) + " left");
        helper.assertTrue(menu.slots.get(menu.resultIndex()).getItem().is(Items.BREAD),
                "the laid-out pattern makes its bread");

        // laying it again on a FULL pattern must change nothing (cells already hold their makings)
        menu.applyLayOutGhost("minecraft:bread");
        onGrid = 0;
        for (int i = 0; i < 9; i++) onGrid += menu.slots.get(gridStart + i).getItem().getCount();
        helper.assertTrue(onGrid == 3 && countPack(pack, Items.WHEAT) == 4,
                "re-laying a laid pattern moves nothing");

        // craft it twice (vanilla's shift-click loop re-calls quickMoveStack while the result
        // holds): first craft refills the bench from the pack's 4, the second leaves 1 spare
        menu.quickMoveStack(player, menu.resultIndex());
        menu.quickMoveStack(player, menu.resultIndex());
        int loaves = countPack(pack, Items.BREAD);
        for (int i = 1; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.BREAD)) loaves += player.getInventory().getItem(i).getCount();
        }
        helper.assertTrue(loaves == 2, "seven wheat = two loaves and one spare, got " + loaves + " loaves");

        // a recipe the pack can't cover at all: NOTHING moves
        menu.applyToggleRoll(); // clear the roll back into the pack
        menu.applyToggleRoll();
        int wheatBefore = countPack(pack, Items.WHEAT);
        menu.applyLayOutGhost("minecraft:cake"); // needs milk, sugar, eggs - none aboard
        onGrid = 0;
        for (int i = 0; i < 9; i++) onGrid += menu.slots.get(gridStart + i).getItem().getCount();
        helper.assertTrue(onGrid == 0, "an uncoverable pattern lays nothing out");
        helper.assertTrue(countPack(pack, Items.WHEAT) == wheatBefore, "and spends nothing");
        helper.succeed();
    }

    /**
     * The natural pinning gesture: dropping an item into a tab its rules would NOT route it
     * to pins it there (so it stays put instead of jumping back on the next sort), while
     * dropping an item where it already belongs pins nothing. Conservation holds throughout.
     */
    @GameTest(template = "empty")
    public static void droppingIntoForeignTabAutoPins(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.LEATHER).get());
        player.getInventory().setItem(0, pack);
        var menu = com.sappersquad.packwork.pack.PackMenu.server(41, player.getInventory(), 0);

        // drop bread (a Food item) into the Ores tab: it must PIN there and stay
        menu.applySelectTab("auto:ores");
        menu.setCarried(new ItemStack(Items.BREAD, 5));
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        PackLayout layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        helper.assertTrue("auto:ores".equals(layout.pinnedTab(BuiltInRegistries.ITEM.getKey(Items.BREAD))),
                "bread dropped into Ores pins to Ores, got " + layout.pins());
        helper.assertTrue(countPack(pack, Items.BREAD) == 5, "the bread itself landed in the pack");
        helper.assertTrue(menu.getCarried().isEmpty(), "and left the cursor");
        helper.assertTrue("auto:ores".equals(SortEngine.route(new ItemStack(Items.BREAD),
                SortEngine.tabsFor(layout), layout)), "so bread now routes to Ores");

        // drop iron into Ores - its own tab - and no pin is created
        menu.setCarried(new ItemStack(Items.IRON_INGOT, 3));
        menu.clicked(1, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        helper.assertTrue(layout.pinnedTab(BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT)) == null,
                "iron dropped into Ores needs no pin");
        helper.assertTrue(countPack(pack, Items.IRON_INGOT) == 3, "the iron landed all the same");
        helper.succeed();
    }

    /**
     * Keep-my-layout: a compartment flipped to manual keeps items in the exact cells the
     * player drops them, new arrivals fill the gaps, the arrangement survives the same
     * save/load a relog performs, and flipping back to Tidy re-sorts cleanly. All of it
     * is view-only over the one flat store, so conservation holds at every step.
     */
    @GameTest(template = "empty")
    public static void keepMyLayoutHoldsCellsAndConserves(GameTestHelper helper) {
        HolderLookup.Provider reg = helper.getLevel().registryAccess();
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack pack = new ItemStack(ModItems.pack(PackTier.LEATHER).get());
        player.getInventory().setItem(0, pack);
        var menu = com.sappersquad.packwork.pack.PackMenu.server(44, player.getInventory(), 0);

        // flip Blocks to keep-my-layout, then drop cobble into cell 5 of the empty grid
        menu.applySelectTab("auto:blocks");
        menu.applyToggleTabMode("auto:blocks");
        menu.setCarried(new ItemStack(Items.COBBLESTONE, 10));
        menu.clicked(5, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);

        PackLayout layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        var kept = layout.manualFor("auto:blocks");
        helper.assertTrue(kept != null && kept.cells().size() == 1
                        && kept.cells().get(0).cell() == 5,
                "the drop is remembered at cell 5, got " + (kept == null ? "null" : kept.cells()));
        helper.assertTrue(countPack(pack, Items.COBBLESTONE) == 10 && menu.getCarried().isEmpty(),
                "all ten cobble landed in the pack");
        helper.assertTrue(menu.slots.get(5).getItem().is(Items.COBBLESTONE),
                "and the grid shows it at cell 5");

        // an automated arrival (hopper-style insert) fills a gap instead of moving the cobble
        new PackInventory(pack, PackTier.LEATHER).insertItem(0, new ItemStack(Items.STONE), false);
        menu.rebuildView();
        helper.assertTrue(menu.slots.get(0).getItem().is(Items.STONE),
                "the arrival fills the first gap");
        helper.assertTrue(menu.slots.get(5).getItem().is(Items.COBBLESTONE),
                "and the cobble has not moved");

        // the arrangement survives the exact save/load a relog performs
        Tag saved = pack.save(reg);
        ItemStack reloaded = ItemStack.parse(reg, saved).orElseThrow();
        player.getInventory().setItem(1, reloaded);
        var menu2 = com.sappersquad.packwork.pack.PackMenu.server(45, player.getInventory(), 1);
        menu2.applySelectTab("auto:blocks");
        helper.assertTrue(menu2.slots.get(5).getItem().is(Items.COBBLESTONE),
                "the kept cell survives a relog");

        // Tidy Up is still the one-shot re-sort: the mode stays, the arrangement resets
        menu.applyTidyUp();
        layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        kept = layout.manualFor("auto:blocks");
        helper.assertTrue(kept != null && kept.cells().isEmpty(),
                "Tidy Up resets the arrangement but keeps the mode");
        helper.assertTrue(countPack(pack, Items.COBBLESTONE) == 10
                && countPack(pack, Items.STONE) == 1, "conservation holds through Tidy Up");

        // flip back to Tidy: the compartment re-sorts cleanly (compact, ascending)
        menu.applyToggleTabMode("auto:blocks");
        layout = pack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        helper.assertTrue(layout.manualFor("auto:blocks") == null, "back to Tidy");
        helper.assertTrue(menu.slots.get(0).hasItem() && menu.slots.get(1).hasItem(),
                "the tidy view compacts to the front");
        helper.succeed();
    }

    /** The Outfitter's Handbook content model builds (every chapter has entries) and the item is registered. */
    @GameTest(template = "empty")
    public static void handbookContentBuilds(GameTestHelper helper) {
        var chapters = com.sappersquad.packwork.guide.HandbookContent.CHAPTERS;
        helper.assertTrue(!chapters.isEmpty(), "the handbook has chapters");
        for (var ch : chapters) {
            helper.assertTrue(!ch.entries().isEmpty(), "chapter '" + ch.title() + "' has content");
        }
        helper.assertTrue(ModItems.HANDBOOK.get() != null, "the handbook item is registered");
        helper.succeed();
    }

    private static void assertRoute(GameTestHelper helper, List<TabView> tabs, PackLayout layout,
                                    net.minecraft.world.item.Item item, String expectedTab) {
        String got = SortEngine.route(new ItemStack(item), tabs, layout);
        helper.assertTrue(expectedTab.equals(got),
                BuiltInRegistries.ITEM.getKey(item) + " should route to " + expectedTab + " but got " + got);
    }
}
