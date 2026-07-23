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

    /** A manual pin beats every rule; a custom tab's own rule claims matching items. */
    @GameTest(template = "empty")
    public static void pinsAndCustomTabsWin(GameTestHelper helper) {
        ResourceLocation stickId = BuiltInRegistries.ITEM.getKey(Items.STICK);
        ResourceLocation breadId = BuiltInRegistries.ITEM.getKey(Items.BREAD);

        // Custom tab that claims anything from minecraft by a name rule, plus a pin.
        TabDef custom = new TabDef("custom:0", "Bits",
                ResourceLocation.withDefaultNamespace("stick"), 0,
                List.of(SortRule.name("stick")));
        List<String> order = new ArrayList<>(AutoTabs.defaultOrder());
        order.add("custom:0");
        PackLayout layout = new PackLayout(order,
                List.of(custom),
                List.of(new PackLayout.Pin(breadId, "custom:0")),
                List.of());

        List<TabView> tabs = SortEngine.tabsFor(layout);
        // stick now claimed by the custom tab's name rule instead of Loose
        assertRoute(helper, tabs, layout, Items.STICK, "custom:0");
        // bread is pinned to custom:0, overriding the Food auto-tab
        assertRoute(helper, tabs, layout, Items.BREAD, "custom:0");
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
        ItemStack feather = new ItemStack(ModItems.trinket(
                com.sappersquad.packwork.trinket.TrinketType.FEATHER).get());

        helper.assertTrue(sockets.insertItem(0, feather.copy(), false).isEmpty(), "first feather installs");
        helper.assertTrue(!sockets.insertItem(1, feather.copy(), false).isEmpty(),
                "a duplicate feather is refused");
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

    private static void assertRoute(GameTestHelper helper, List<TabView> tabs, PackLayout layout,
                                    net.minecraft.world.item.Item item, String expectedTab) {
        String got = SortEngine.route(new ItemStack(item), tabs, layout);
        helper.assertTrue(expectedTab.equals(got),
                BuiltInRegistries.ITEM.getKey(item) + " should route to " + expectedTab + " but got " + got);
    }
}
