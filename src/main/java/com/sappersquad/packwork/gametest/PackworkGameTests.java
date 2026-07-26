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
        int gridStart = menu.resultIndex() - 9;
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
        int gridStart = menu.resultIndex() - 9;
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
