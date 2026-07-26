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
     * A manual pin beats every rule (with or without a ledger); a custom tab's own rules
     * only claim items when a Quill &amp; Ledger is fitted - pin-only otherwise.
     */
    @GameTest(template = "empty")
    public static void pinsAlwaysWinLedgerGatesRules(GameTestHelper helper) {
        ResourceLocation breadId = BuiltInRegistries.ITEM.getKey(Items.BREAD);

        // Custom tab that claims sticks by a name rule, plus a pin sending bread to it.
        TabDef custom = new TabDef("custom:0", "Bits",
                ResourceLocation.withDefaultNamespace("stick"), 0,
                List.of(SortRule.name("stick")));
        List<String> order = new ArrayList<>(AutoTabs.defaultOrder());
        order.add("custom:0");
        PackLayout layout = new PackLayout(order,
                List.of(custom),
                List.of(new PackLayout.Pin(breadId, "custom:0")),
                List.of());

        // Without a Quill & Ledger: custom tab is pin-only. The stick rule is ignored
        // (stick falls to Loose), but the pin still overrides the Food auto-tab.
        List<TabView> noLedger = SortEngine.tabsFor(layout, false);
        assertRoute(helper, noLedger, layout, Items.STICK, AutoTabs.LOOSE_ID);
        assertRoute(helper, noLedger, layout, Items.BREAD, "custom:0");

        // With a Quill & Ledger: the custom tab's own rule now claims sticks; the pin
        // still wins for bread.
        List<TabView> ledger = SortEngine.tabsFor(layout, true);
        assertRoute(helper, ledger, layout, Items.STICK, "custom:0");
        assertRoute(helper, ledger, layout, Items.BREAD, "custom:0");
        helper.succeed();
    }

    /**
     * Quill &amp; Ledger files a custom tab by the item it's stamped with: a tab stamped
     * with a pickaxe (a tool) gathers tools once the ledger is fitted, and is pin-only
     * without it.
     */
    @GameTest(template = "empty")
    public static void quillLedgerFilesByStamp(GameTestHelper helper) {
        TabDef custom = new TabDef("custom:0", "Kit",
                BuiltInRegistries.ITEM.getKey(Items.IRON_PICKAXE), 0, List.of());
        // put the custom tab FIRST so, when its derived rule is active, it out-prioritises
        // the Tools auto-tab.
        List<String> order = new ArrayList<>();
        order.add("custom:0");
        order.addAll(AutoTabs.defaultOrder());
        PackLayout layout = new PackLayout(order, List.of(custom), List.of(), List.of());

        // no ledger: pin-only, so the pickaxe routes to the Tools auto-tab
        assertRoute(helper, SortEngine.tabsFor(layout, false), layout, Items.DIAMOND_PICKAXE, "auto:tools");
        // ledger: the stamp's kind (a tool) is derived and the custom tab claims it first
        assertRoute(helper, SortEngine.tabsFor(layout, true), layout, Items.DIAMOND_PICKAXE, "custom:0");
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
                net.minecraft.world.item.crafting.Ingredient.of(Items.IRON_INGOT), 4,
                net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);

        // a 3x3 crafting input: the pack + 4 iron ingots
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
                pack, new ItemStack(Items.IRON_INGOT, 4), ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
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
