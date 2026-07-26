package com.sappersquad.packwork.client;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackMenu;
import com.sappersquad.packwork.reg.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Dev-only visual harness. With {@code -Dpackwork.autoshot=1} the client boots a
 * throwaway creative world, fills a pack with a spread across every tab, opens it,
 * and drops screenshots into {@code run/client/screenshots/} so the GUI can be
 * eyeballed headlessly (the gradle dev window can't be driven by desktop tooling).
 * Never runs in production - the whole class is gated on the system property.
 */
@EventBusSubscriber(modid = Packwork.MODID, value = Dist.CLIENT)
public final class DevAutoShot {

    private static final boolean ENABLED = System.getProperty("packwork.autoshot") != null;

    private enum Phase {
        BOOT, WAIT_LEVEL, OPEN,
        SHOOT_GRID,
        BUCKET_GIVE, BUCKET_W1, BUCKET_CLICK, BUCKET_W2, BUCKET_REPORT,
        HOVER, HW, PIN1, PW1, SHOOT_PINNED, PIN2, PW2, SHOOT_UNPINNED,
        SHOOT_1, W1, SHOOT_2, W2, SHOOT_3, W3, SHOOT_4, W4, SHOOT_5,
        OPEN_BOOK, WB, SHOOT_BOOK, WB2, SHOOT_BOOK2,
        PLACE, WPLACE, SHOOT_WORLD, OPEN_BLOCK, WBLOCK, SHOOT_BLOCK,
        WBREAK, SHOOT_BROKEN, WREPLACE, SHOOT_REPLACED,
        WLINEUP, SHOOT_LINEUP, SHOOT_INHAND,
        KIT_GIVE, KIT_W1, KIT_UNROLL, KIT_W2, SHOOT_ROLL, KIT_LOAD, KIT_W3, SHOOT_LOADED,
        KIT_CRAFT, KIT_W4, KIT_REPORT, DONE
    }

    private static Phase phase = Phase.BOOT;
    private static int ticks = 0;
    private static int wait = 0;
    private static String customTabId = "custom:0";
    private static net.minecraft.core.BlockPos placedMiddle = null;
    private static net.minecraft.core.BlockPos placedLast = null;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED || phase == Phase.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;

        switch (phase) {
            case BOOT -> {
                if (ticks == 5) {
                    // Grow the dev window and force GUI scale 3 (what players actually use) so the
                    // search text + slots can be judged at real size, not the tiny default scale.
                    try {
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1120, 900);
                        mc.options.guiScale().set(3);
                        mc.resizeDisplay();
                        Packwork.LOGGER.info("[autoshot] window 1120x900 @ guiScale 3");
                    } catch (Throwable t) {
                        Packwork.LOGGER.warn("[autoshot] resize failed: {}", t.toString());
                    }
                }
                if (ticks > 40 && mc.level == null && mc.screen != null) {
                    Packwork.LOGGER.info("[autoshot] creating throwaway world");
                    LevelSettings settings = new LevelSettings("packwork_autoshot", GameType.CREATIVE,
                            false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("packwork_autoshot", settings,
                            WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, mc.screen);
                    phase = Phase.WAIT_LEVEL;
                    wait = 0;
                }
            }
            case WAIT_LEVEL -> {
                if (mc.player != null && mc.getSingleplayerServer() != null) {
                    if (++wait > 60) {
                        setupAndOpen(mc);
                        phase = Phase.OPEN;
                        wait = 0;
                    }
                }
            }
            case OPEN -> {
                if (mc.screen instanceof PackScreen && ++wait > 12) {
                    // flatten so the whole varied spread is visible in one grid for the alignment shot
                    withMenu(mc, PackClientActions::toggleFlatten);
                    phase = Phase.SHOOT_GRID; wait = 0;
                }
            }
            case SHOOT_GRID -> {
                if (++wait > 8) {
                    grab(mc, "packwork_grid");   // BUG 3: every item type centred in its cell @ scale 3
                    withMenu(mc, PackClientActions::toggleFlatten); // back to tabbed for the pin demo
                    phase = Phase.BUCKET_GIVE; wait = 0;
                }
            }
            // ---- the reported bug: clicking the waterskin gauge with a bucket threw it on the floor ----
            case BUCKET_GIVE -> {
                giveCarried(mc, BUCKET_ROUNDS[bucketRound]);
                phase = Phase.BUCKET_W1; wait = 0;
            }
            case BUCKET_W1 -> { if (++wait > 10) { phase = Phase.BUCKET_CLICK; wait = 0; } }
            case BUCKET_CLICK -> {
                clickFluidGauge(mc);   // a REAL press AND release at the gauge - the release is where it dropped
                phase = Phase.BUCKET_W2; wait = 0;
            }
            case BUCKET_W2 -> { if (++wait > 14) { phase = Phase.BUCKET_REPORT; wait = 0; } }
            case BUCKET_REPORT -> {
                reportBucketRound(mc);
                grab(mc, "packwork_bucket_" + bucketRound);
                if (++bucketRound < BUCKET_ROUNDS.length) {
                    phase = Phase.BUCKET_GIVE;
                } else {
                    giveCarried(mc, ItemStack.EMPTY); // clear the cursor before the pin demo
                    phase = Phase.HOVER;
                }
                wait = 0;
            }
            case HOVER -> {
                if (++wait > 6) {
                    pinItemKey = hoverFirstGridItem(mc); // move the real cursor over an item + record its slot
                    phase = Phase.HW; wait = 0;
                }
            }
            case HW -> { if (++wait > 4) { phase = Phase.PIN1; wait = 0; } }
            case PIN1 -> {
                // Force the hovered slot (the unfocused dev window won't register a synthetic cursor
                // move as a hover), then fire a REAL Screen.keyPressed(P) at it - exercises the real
                // keyPressed -> pin wiring end to end.
                if (mc.screen instanceof PackScreen ps) ps.devHover(pinSlotIndex);
                pressPin(mc);                        // BUG 1: a REAL Screen.keyPressed(P)
                phase = Phase.PW1; wait = 0;
            }
            case PW1 -> { if (++wait > 14) phase = Phase.SHOOT_PINNED; }
            case SHOOT_PINNED -> {
                logPinState(mc, "after 1st P");
                grab(mc, "packwork_pinned");         // the brass pin marker should now be on that slot
                phase = Phase.PIN2; wait = 0;
            }
            case PIN2 -> {
                hoverFirstGridItem(mc);
                if (mc.screen instanceof PackScreen ps) ps.devHover(pinSlotIndex);
                pressPin(mc);                        // press again -> unpin
                phase = Phase.PW2; wait = 0;
            }
            case PW2 -> { if (++wait > 14) phase = Phase.SHOOT_UNPINNED; }
            case SHOOT_UNPINNED -> {
                logPinState(mc, "after 2nd P");
                grab(mc, "packwork_unpinned");       // marker gone again
                phase = Phase.SHOOT_1; wait = 0;
            }
            case SHOOT_1 -> {
                grab(mc, "packwork_tabs");        // default: Food tab active, rail visible
                switchTab(mc, "auto:combat");
                phase = Phase.W1; wait = 0;
            }
            case W1 -> { if (++wait > 6) phase = Phase.SHOOT_2; }
            case SHOOT_2 -> {
                grab(mc, "packwork_combat");      // a different compartment
                newTab(mc);                        // make a custom tab, selects it
                phase = Phase.W2; wait = 0;
            }
            case W2 -> { if (++wait > 8) phase = Phase.SHOOT_3; }
            case SHOOT_3 -> {
                grab(mc, "packwork_newtab");       // rail now shows the new (empty) leather tab
                // pin a diamond into it and dye it blue
                withMenu(mc, m -> {
                    customTabId = m.activeTab();
                    PackClientActions.pin(m, customTabId, "minecraft:diamond");
                    PackClientActions.tabColor(m, customTabId, 0xFF6E8BB9);
                });
                phase = Phase.W3; wait = 0;
            }
            case W3 -> { if (++wait > 8) phase = Phase.SHOOT_4; }
            case SHOOT_4 -> {
                grab(mc, "packwork_pin_dye");      // custom tab: dyed, now holds the pinned diamond
                // flatten + search to prove the search bar filters
                withMenu(mc, m -> {
                    PackClientActions.toggleFlatten(m);
                    PackClientActions.setSearch(m, "iron");
                });
                phase = Phase.W4; wait = 0;
            }
            case W4 -> { if (++wait > 8) phase = Phase.SHOOT_5; }
            case SHOOT_5 -> {
                grab(mc, "packwork_search");       // only the iron items remain
                HandbookClientHooks.open();         // swap to the Outfitter's Handbook
                phase = Phase.WB; wait = 0;
            }
            case WB -> { if (++wait > 10) phase = Phase.SHOOT_BOOK; }
            case SHOOT_BOOK -> {
                grab(mc, "packwork_handbook");     // chapter 1: "The Pack" (prose + a pack row)
                // jump to the Trinkets chapter to prove chapter switching + a second item row
                if (mc.screen instanceof OutfitterHandbookScreen book) book.devSelectChapter(2);
                phase = Phase.WB2; wait = 0;
            }
            case WB2 -> { if (++wait > 8) phase = Phase.SHOOT_BOOK2; }
            case SHOOT_BOOK2 -> {
                grab(mc, "packwork_handbook_trinkets");
                mc.setScreen(null);          // close the book
                placeBlocks(mc);             // set three packs down in the world
                phase = Phase.WPLACE; wait = 0;
            }
            case WPLACE -> { if (++wait > 30) phase = Phase.SHOOT_WORLD; } // let chunks re-render
            case SHOOT_WORLD -> {
                grab(mc, "packwork_placed_world"); // all five per-tier packs on the ground
                openBlock(mc);
                phase = Phase.WBLOCK; wait = 0;
            }
            case WBLOCK -> { if (++wait > 15) phase = Phase.SHOOT_BLOCK; } // menu opens + syncs
            case SHOOT_BLOCK -> {
                grab(mc, "packwork_placed_gui"); // the SAME organizer, opened from the block
                mc.setScreen(null);              // close the GUI, back to the world row
                breakPlacedPack(mc);             // break the Runed pack - logs the RIGHT-tier drop
                phase = Phase.WBREAK; wait = 0;
            }
            case WBREAK -> { if (++wait > 24) phase = Phase.SHOOT_BROKEN; } // let the chunk re-render
            case SHOOT_BROKEN -> {
                grab(mc, "packwork_broken");     // a gap where the Runed pack was (it was returned)
                replacePlacedPack(mc);           // set a Leather pack down in the same spot
                phase = Phase.WREPLACE; wait = 0;
            }
            case WREPLACE -> { if (++wait > 24) phase = Phase.SHOOT_REPLACED; }
            case SHOOT_REPLACED -> {
                grab(mc, "packwork_replaced");   // a Leather pack now renders there - render retracks
                giveLineup(mc);                  // hand over the whole item set for a lineup shot
                phase = Phase.WLINEUP; wait = 0;
            }
            case WLINEUP -> {
                if (++wait > 8) {
                    if (mc.player != null) mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                    phase = Phase.SHOOT_LINEUP; wait = 0;
                }
            }
            case SHOOT_LINEUP -> {
                if (++wait > 6) {
                    grab(mc, "packwork_lineup");  // every pack tier + trinket in the inventory grid
                    mc.setScreen(null);            // drop to first person, holding the Runed pack
                    phase = Phase.SHOOT_INHAND; wait = 0;
                }
            }
            case SHOOT_INHAND -> {
                if (++wait > 12) {
                    grab(mc, "packwork_inhand");  // the revamped pack sprite in hand
                    phase = Phase.KIT_GIVE; wait = 0;
                }
            }
            // ---- the Tinker's Kit: unroll the bench and craft from pack stock, for real ----
            case KIT_GIVE -> {
                giveKitPack(mc);
                phase = Phase.KIT_W1; wait = 0;
            }
            case KIT_W1 -> { if (mc.screen instanceof PackScreen && ++wait > 16) { phase = Phase.KIT_UNROLL; wait = 0; } }
            case KIT_UNROLL -> {
                switchTab(mc, "auto:nature");     // the wheat lives in Nature & Farming
                clickRollLatch(mc);               // a real press + release on the latch
                phase = Phase.KIT_W2; wait = 0;
            }
            case KIT_W2 -> { if (++wait > 14) phase = Phase.SHOOT_ROLL; }
            case SHOOT_ROLL -> {
                grab(mc, "packwork_roll_open");   // the tool roll unrolled across the lower rows
                phase = Phase.KIT_LOAD; wait = 0;
            }
            case KIT_LOAD -> {
                loadRollFromPack(mc, 3);          // three real shift-clicks off the pack grid = a row of wheat
                phase = Phase.KIT_W3; wait = 0;
            }
            case KIT_W3 -> { if (++wait > 16) phase = Phase.SHOOT_LOADED; }
            case SHOOT_LOADED -> {
                grab(mc, "packwork_roll_loaded"); // a 2x2 of planks laid out, result showing
                logKitState(mc, "before crafting");
                phase = Phase.KIT_CRAFT; wait = 0;
            }
            case KIT_CRAFT -> {
                craftFromRoll(mc);                // a real shift-click on the result
                phase = Phase.KIT_W4; wait = 0;
            }
            case KIT_W4 -> { if (++wait > 20) phase = Phase.KIT_REPORT; }
            case KIT_REPORT -> {
                logKitState(mc, "after crafting");
                grab(mc, "packwork_roll_crafted");
                phase = Phase.DONE;
                Packwork.LOGGER.info("[autoshot] done - screenshots written");
            }
            default -> {}
        }
    }

    // ---- waterskin-gauge bucket check (the bug SapperSquad hit: the bucket landed on the ground) ----

    /** One bucket, then a STACK of three, then an empty one to fill back out of the tank. */
    private static final ItemStack[] BUCKET_ROUNDS = {
            new ItemStack(Items.WATER_BUCKET),
            new ItemStack(Items.WATER_BUCKET, 3),
            new ItemStack(Items.BUCKET)
    };
    private static int bucketRound = 0;

    /** Put a stack on the player's cursor server-side; it syncs down to the open screen. */
    private static void giveCarried(Minecraft mc, ItemStack stack) {
        var server = mc.getSingleplayerServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;
        ItemStack copy = stack.copy();
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            // clear stray buckets so each round's count is unambiguous
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack s = sp.getInventory().getItem(i);
                if (s.is(Items.BUCKET) || s.is(Items.WATER_BUCKET)) sp.getInventory().setItem(i, ItemStack.EMPTY);
            }
            sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    sp.getBoundingBox().inflate(12)).forEach(net.minecraft.world.entity.Entity::discard);
            sp.containerMenu.setCarried(copy);
            Packwork.LOGGER.info("[autoshot][bucket] round {} - cursor set to {} x{}",
                    bucketRound, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(copy.getItem()), copy.getCount());
        });
    }

    /** Dispatch a genuine press + release at the waterskin gauge - the same pair MouseHandler sends. */
    private static void clickFluidGauge(Minecraft mc) {
        if (!(mc.screen instanceof PackScreen ps)) {
            Packwork.LOGGER.warn("[autoshot][bucket] pack screen not open");
            return;
        }
        int[] c = ps.devFluidGaugeCenter();
        if (c == null) {
            Packwork.LOGGER.warn("[autoshot][bucket] no waterskin gauge on the rail");
            return;
        }
        boolean pressed = ps.mouseClicked(c[0], c[1], 0);
        boolean released = ps.mouseReleased(c[0], c[1], 0);
        Packwork.LOGGER.info("[autoshot][bucket] clicked gauge at ({},{}) press={} release={}",
                c[0], c[1], pressed, released);
    }

    /** Log what the click actually did: cursor, pockets, ground, tank. This is the proof. */
    private static void reportBucketRound(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;
        int round = bucketRound;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            ItemStack cursor = sp.containerMenu.getCarried();
            int inPockets = 0, waterInPockets = 0;
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack s = sp.getInventory().getItem(i);
                if (s.is(Items.BUCKET)) inPockets += s.getCount();
                if (s.is(Items.WATER_BUCKET)) waterInPockets += s.getCount();
            }
            var ground = sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    sp.getBoundingBox().inflate(12));
            StringBuilder onFloor = new StringBuilder();
            for (var e : ground) onFloor.append(net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(e.getItem().getItem())).append(" x").append(e.getItem().getCount()).append(" ");
            ItemStack pack = sp.getInventory().getItem(0);
            int tank = new com.sappersquad.packwork.pack.PackFluidHandler(pack,
                    com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack)).getFluidInTank(0).getAmount();
            Packwork.LOGGER.info("[autoshot][bucket] round {} RESULT: cursor={} x{} | pockets: {} empty, {} water"
                            + " | ON GROUND: {} | tank={} mB",
                    round, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(cursor.getItem()),
                    cursor.getCount(), inPockets, waterInPockets,
                    ground.isEmpty() ? "NOTHING" : onFloor.toString().trim(), tank);
        });
    }

    // ---- Tinker's Kit live check ----

    /** Hand over a Runed pack with the Kit, the Sleeve and the Creel fitted, stocked to craft with. */
    private static void giveKitPack(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            sp.getInventory().clearContent();
            ItemStack pack = new ItemStack(ModItems.pack(com.sappersquad.packwork.pack.PackTier.RUNED).get());
            var sockets = new com.sappersquad.packwork.pack.PackTrinketInventory(
                    () -> pack, com.sappersquad.packwork.pack.PackTier.RUNED);
            sockets.insertItem(0, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.TINKERS_KIT).get()), false);
            sockets.insertItem(1, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.CARTOGRAPHER).get()), false);
            sockets.insertItem(2, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.ANGLERS_CREEL).get()), false);
            sockets.insertItem(3, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.FIELD_FURNACE).get()), false);
            var store = new com.sappersquad.packwork.pack.PackInventory(
                    pack, com.sappersquad.packwork.pack.PackTier.RUNED);
            store.insertItem(0, new ItemStack(Items.WHEAT, 9), false);   // 3 in a row = bread
            store.insertItem(1, new ItemStack(Items.COD, 8), false);
            store.insertItem(2, new ItemStack(Items.COMPASS, 1), false);
            store.insertItem(3, new ItemStack(Items.FILLED_MAP, 2), false);
            store.insertItem(4, new ItemStack(Items.RAW_IRON, 12), false);
            store.insertItem(5, new ItemStack(Items.COAL, 4), false);
            sp.getInventory().items.set(0, pack);
            sp.getInventory().selected = 0;
            PackItem.openPack(sp, 0);
            Packwork.LOGGER.info("[autoshot][kit] Runed pack with Tinker's Kit + Sleeve + Creel + Furnace opened");
        });
    }

    /** A genuine press + release on the tool-roll latch in the title strip. */
    private static void clickRollLatch(Minecraft mc) {
        if (!(mc.screen instanceof PackScreen ps)) {
            Packwork.LOGGER.warn("[autoshot][kit] pack screen not open");
            return;
        }
        int[] c = ps.devRollButtonCenter();
        if (c == null) {
            Packwork.LOGGER.warn("[autoshot][kit] no roll latch - is the kit fitted?");
            return;
        }
        ps.mouseClicked(c[0], c[1], 0);
        ps.mouseReleased(c[0], c[1], 0);
        Packwork.LOGGER.info("[autoshot][kit] clicked the roll latch at ({},{})", c[0], c[1]);
    }

    /** Shift-click a plank out of the pack grid onto the bench, {@code times} over - the real path. */
    private static void loadRollFromPack(Minecraft mc, int times) {
        if (!(mc.screen instanceof PackScreen ps) || mc.player == null) return;
        PackMenu menu = ps.getMenu();
        for (int n = 0; n < times; n++) {
            int slot = -1;
            for (int i = 0; i < menu.slots.size(); i++) {
                var s = menu.slots.get(i);
                if (s instanceof com.sappersquad.packwork.pack.PackViewSlot && s.isActive()
                        && s.getItem().is(Items.WHEAT)) { slot = i; break; }
            }
            if (slot < 0) { Packwork.LOGGER.warn("[autoshot][kit] no wheat in the grid to lay out"); return; }
            mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0,
                    net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
        }
        Packwork.LOGGER.info("[autoshot][kit] shift-clicked {} wheat onto the bench", times);
    }

    /** A real shift-click on the roll's result slot. */
    private static void craftFromRoll(Minecraft mc) {
        if (!(mc.screen instanceof PackScreen ps) || mc.player == null) return;
        PackMenu menu = ps.getMenu();
        mc.gameMode.handleInventoryMouseClick(menu.containerId, menu.resultIndex(), 0,
                net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
        Packwork.LOGGER.info("[autoshot][kit] shift-clicked the result slot");
    }

    /** Count what the SERVER's pack actually holds, so the craft can be checked, not assumed. */
    private static void logKitState(Minecraft mc, String when) {
        var server = mc.getSingleplayerServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            ItemStack pack = sp.getInventory().getItem(0);
            var store = new com.sappersquad.packwork.pack.PackInventory(
                    pack, com.sappersquad.packwork.pack.PackItem.tierOf(pack));
            int wheat = 0, bread = 0;
            for (int i = 0; i < store.getSlots(); i++) {
                ItemStack s = store.getStackInSlot(i);
                if (s.is(Items.WHEAT)) wheat += s.getCount();
                if (s.is(Items.BREAD)) bread += s.getCount();
            }
            StringBuilder bench = new StringBuilder();
            if (sp.containerMenu instanceof PackMenu m) {
                for (int i = m.resultIndex() - 9; i <= m.resultIndex(); i++) {
                    ItemStack s = m.slots.get(i).getItem();
                    bench.append(s.isEmpty() ? "-" : (net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(s.getItem()).getPath() + "x" + s.getCount())).append(' ');
                }
            }
            Packwork.LOGGER.info("[autoshot][kit] {}: pack holds {} wheat, {} bread | bench+result: {}",
                    when, wheat, bread, bench.toString().trim());
        });
    }

    private static void setupAndOpen(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayers().isEmpty()
                    ? null : server.getPlayerList().getPlayers().get(0);
            if (sp == null) return;

            // a Runed pack (4 trinket sockets) so all four store gauges can be on show
            ItemStack pack = new ItemStack(ModItems.pack(com.sappersquad.packwork.pack.PackTier.RUNED).get());
            IItemHandler h = pack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ITEM);
            ItemStack[] spread = {
                    new ItemStack(Items.BREAD, 32), new ItemStack(Items.COOKED_BEEF, 12),
                    new ItemStack(Items.APPLE, 6), new ItemStack(Items.GOLDEN_CARROT, 3),
                    new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.IRON_CHESTPLATE),
                    new ItemStack(Items.ARROW, 64), new ItemStack(Items.SHIELD),
                    new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_AXE),
                    new ItemStack(Items.SHEARS), new ItemStack(Items.FLINT_AND_STEEL),
                    new ItemStack(Items.IRON_INGOT, 40), new ItemStack(Items.GOLD_INGOT, 24),
                    new ItemStack(Items.DIAMOND, 9), new ItemStack(Items.RAW_IRON, 30),
                    new ItemStack(Items.NETHER_WART, 16), new ItemStack(Items.BLAZE_POWDER, 8),
                    new ItemStack(Items.OAK_SAPLING, 10), new ItemStack(Items.WHEAT_SEEDS, 12),
                    new ItemStack(Items.POPPY, 8), new ItemStack(Items.OAK_PLANKS, 64),
                    new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.BRICKS, 48),
                    new ItemStack(Items.STICK, 20), new ItemStack(Items.STRING, 14),
                    // varied silhouettes so item centring in the 16px cells can be judged
                    new ItemStack(Items.POTION), new ItemStack(Items.TORCH, 16),
                    new ItemStack(Items.ENDER_PEARL, 4), new ItemStack(Items.GLASS_BOTTLE, 6),
                    new ItemStack(Items.EGG, 8), new ItemStack(Items.BONE, 12),
            };
            for (int i = 0; i < spread.length; i++) h.insertItem(i, spread[i], false);

            // fit all four store trinkets so every gauge on the right rail shows a level
            var sockets = new com.sappersquad.packwork.pack.PackTrinketInventory(
                    () -> pack, com.sappersquad.packwork.pack.PackTier.RUNED);
            sockets.insertItem(0, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.WATERSKIN).get()), false);
            sockets.insertItem(1, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.SOUL_VIAL).get()), false);
            sockets.insertItem(2, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL).get()), false);
            sockets.insertItem(3, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.FLASK_HARNESS).get()), false);
            // half-fill the chemical tank (dist-neutral component) so the flask gauge shows a
            // level when run with Mekanism present (-Pmekanism); harmless without it.
            pack.set(com.sappersquad.packwork.reg.ModComponents.PACK_CHEMICAL.get(),
                    new com.sappersquad.packwork.pack.PackChemical("mekanism:hydrogen",
                            com.sappersquad.packwork.pack.PackChemical.capacityFor(pack) * 2 / 3));
            // stash XP + charge so both the soul-vial and charge-crystal gauges show a level
            pack.set(com.sappersquad.packwork.reg.ModComponents.PACK_XP.get(),
                    com.sappersquad.packwork.pack.PackXpStore.capacityFor(pack) * 2 / 3);
            pack.set(com.sappersquad.packwork.reg.ModComponents.PACK_ENERGY.get(),
                    com.sappersquad.packwork.pack.PackEnergyStorage.capacityFor(pack) / 2);

            // half-fill the waterskin so the gauge shows a fluid level
            var tank = new com.sappersquad.packwork.pack.PackFluidHandler(
                    pack, com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack));
            tank.fill(new net.neoforged.neoforge.fluids.FluidStack(
                            net.minecraft.world.level.material.Fluids.WATER,
                            com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack) / 2),
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

            sp.getInventory().items.set(0, pack);
            sp.getInventory().selected = 0;

            // Curios (optional): prove the pack wears in the back slot (gated; logs the result).
            if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
                com.sappersquad.packwork.compat.curios.CuriosCompat.devEquip(sp, pack.copy());
            }

            PackItem.openPack(sp, 0);
            Packwork.LOGGER.info("[autoshot] pack filled and opened");
        });
    }

    /** Set all five per-tier pack blocks on a stone-brick pad and stand the player back to view them. */
    private static void placeBlocks(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            net.minecraft.server.level.ServerLevel lvl = sp.serverLevel();
            lvl.setDayTime(6000);
            net.minecraft.core.BlockPos base = sp.blockPosition();
            var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            var floor = net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
            for (int dx = -2; dx <= 7; dx++)
                for (int dz = -3; dz <= 6; dz++) {
                    for (int dy = 0; dy <= 4; dy++) lvl.setBlock(base.offset(dx, dy, dz), air, 2);
                    lvl.setBlock(base.offset(dx, -1, dz), floor, 2);
                }
            // one of every tier, in ladder order, so the per-tier trim can be compared side by side
            com.sappersquad.packwork.pack.PackTier[] tiers =
                    com.sappersquad.packwork.pack.PackTier.values();
            for (int i = 0; i < tiers.length; i++) {
                net.minecraft.core.BlockPos bp = base.offset(i, 0, 4);
                placePackBlock(lvl, bp, tiers[i], i == 2); // give the middle (Studded) a loadout for the GUI shot
                if (i == 2) placedMiddle = bp;
                if (i == tiers.length - 1) placedLast = bp; // the Runed one, for the break/replace check
            }
            // stand back and centre the five-wide row
            sp.connection.teleport(base.getX() + 2.5, base.getY(), base.getZ() - 2.5, 0f, 14f);
            Packwork.LOGGER.info("[autoshot] placed five per-tier pack blocks");
        });
    }

    /** Set one pack block of the given tier down, optionally with a small loadout for the GUI shot. */
    private static void placePackBlock(net.minecraft.server.level.ServerLevel lvl,
                                       net.minecraft.core.BlockPos bp,
                                       com.sappersquad.packwork.pack.PackTier tier, boolean loadout) {
        lvl.setBlock(bp, com.sappersquad.packwork.reg.ModBlocks.PACK.get().defaultBlockState()
                .setValue(com.sappersquad.packwork.block.PackContainerBlock.FACING, net.minecraft.core.Direction.NORTH)
                .setValue(com.sappersquad.packwork.block.PackContainerBlock.TIER, tier), 3);
        if (!(lvl.getBlockEntity(bp) instanceof com.sappersquad.packwork.block.PackContainerBlockEntity be)) return;
        ItemStack pk = new ItemStack(ModItems.pack(tier).get());
        if (loadout) {
            var sockets = new com.sappersquad.packwork.pack.PackTrinketInventory(() -> pk, tier);
            sockets.insertItem(0, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL).get()), false);
            pk.set(com.sappersquad.packwork.reg.ModComponents.PACK_ENERGY.get(),
                    com.sappersquad.packwork.pack.PackEnergyStorage.capacityFor(pk) / 2);
            var st = new com.sappersquad.packwork.pack.PackInventory(pk, tier);
            st.insertItem(0, new ItemStack(Items.DIAMOND, 20), false);
            st.insertItem(1, new ItemStack(Items.IRON_INGOT, 40), false);
            st.insertItem(2, new ItemStack(Items.BREAD, 32), false);
        }
        be.setPackStack(pk);
    }

    /** Break the placed Runed pack the way the world does - via the block's own drops - and log the
     *  returned item's tier, proving break hands back the RIGHT tier, then clear the block. */
    private static void breakPlacedPack(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null || placedLast == null) return;
        server.execute(() -> {
            net.minecraft.server.level.ServerLevel lvl = server.getPlayerList().getPlayers().get(0).serverLevel();
            var state = lvl.getBlockState(placedLast);
            var be = lvl.getBlockEntity(placedLast);
            var drops = net.minecraft.world.level.block.Block.getDrops(state, lvl, placedLast, be);
            String dropped = drops.isEmpty() ? "NOTHING" : net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(drops.get(0).getItem()).toString();
            Packwork.LOGGER.info("[autoshot] broke the Runed placed pack -> drop is {}", dropped);
            lvl.setBlock(placedLast, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        });
    }

    /** Set a Leather pack down where the Runed one was, to show the render retracks to the new tier. */
    private static void replacePlacedPack(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null || placedLast == null) return;
        server.execute(() -> {
            net.minecraft.server.level.ServerLevel lvl = server.getPlayerList().getPlayers().get(0).serverLevel();
            placePackBlock(lvl, placedLast, com.sappersquad.packwork.pack.PackTier.LEATHER, false);
            Packwork.LOGGER.info("[autoshot] re-placed a Leather pack in the gap");
        });
    }

    /** Open the middle placed pack's organizer - the SAME menu as a carried pack. */
    private static void openBlock(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null || placedMiddle == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            if (sp.serverLevel().getBlockEntity(placedMiddle)
                    instanceof com.sappersquad.packwork.block.PackContainerBlockEntity be) {
                net.minecraft.core.BlockPos pos = placedMiddle;
                sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                                (id, inv, pl) -> PackMenu.serverForBlock(id, inv, be),
                                be.getPackStack().getHoverName()),
                        buf -> {
                            buf.writeBoolean(true);
                            buf.writeBlockPos(pos);
                            buf.writeVarInt(be.getTier().ordinal());
                        });
            }
        });
    }

    /** Fill the player inventory with every pack tier + trinket + the handbook, holding a Runed pack. */
    private static void giveLineup(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            sp.getInventory().clearContent();
            for (com.sappersquad.packwork.pack.PackTier t : com.sappersquad.packwork.pack.PackTier.values())
                sp.getInventory().add(new ItemStack(ModItems.pack(t).get()));
            for (com.sappersquad.packwork.trinket.TrinketType tt : com.sappersquad.packwork.trinket.TrinketType.values())
                sp.getInventory().add(new ItemStack(ModItems.trinket(tt).get()));
            sp.getInventory().add(new ItemStack(ModItems.HANDBOOK.get()));
            sp.getInventory().items.set(0, new ItemStack(ModItems.pack(com.sappersquad.packwork.pack.PackTier.RUNED).get()));
            sp.getInventory().selected = 0;
            Packwork.LOGGER.info("[autoshot] lineup handed over");
        });
    }

    private static void switchTab(Minecraft mc, String tab) {
        if (mc.player != null && mc.player.containerMenu instanceof PackMenu menu) {
            PackClientActions.selectTab(menu, tab);
        }
    }

    private static void toggleFlatten(Minecraft mc) {
        if (mc.player != null && mc.player.containerMenu instanceof PackMenu menu) {
            PackClientActions.toggleFlatten(menu);
        }
    }

    private static void newTab(Minecraft mc) {
        if (mc.player != null && mc.player.containerMenu instanceof PackMenu menu) {
            PackClientActions.newTab(menu);
        }
    }

    private static net.minecraft.resources.ResourceLocation pinItemKey = null;
    private static int pinSlotIndex = -1;

    /** Find the first grid item, record its menu-slot index + key, and move the real cursor over it. */
    private static net.minecraft.resources.ResourceLocation hoverFirstGridItem(Minecraft mc) {
        pinSlotIndex = -1;
        if (!(mc.screen instanceof PackScreen ps)) return null;
        var slots = ps.getMenu().slots;
        for (int i = 0; i < slots.size(); i++) {
            net.minecraft.world.inventory.Slot s = slots.get(i);
            if (s instanceof com.sappersquad.packwork.pack.PackViewSlot vs && vs.isActive() && s.hasItem()) {
                pinSlotIndex = i;
                double scale = mc.getWindow().getGuiScale();
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),
                        (ps.getGuiLeft() + s.x + 8) * scale, (ps.getGuiTop() + s.y + 8) * scale);
                var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem().getItem());
                Packwork.LOGGER.info("[autoshot] grid item {} at menu slot {} ({},{})", key, i, s.x, s.y);
                return key;
            }
        }
        Packwork.LOGGER.warn("[autoshot] no grid item to hover for the pin demo");
        return null;
    }

    /** Dispatch a genuine key press through the screen's own input handler - the same call the
     *  GLFW key callback makes - so this exercises the real keyPressed -> pin wiring, not the data action. */
    private static void pressPin(Minecraft mc) {
        if (mc.screen == null) return;
        int scan = org.lwjgl.glfw.GLFW.glfwGetKeyScancode(org.lwjgl.glfw.GLFW.GLFW_KEY_P);
        boolean handled = mc.screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_P, scan, 0);
        Packwork.LOGGER.info("[autoshot] dispatched real keyPressed(P) -> handled={}", handled);
    }

    private static void logPinState(Minecraft mc, String when) {
        if (mc.player != null && mc.player.containerMenu instanceof PackMenu m && pinItemKey != null) {
            Packwork.LOGGER.info("[autoshot] {}: {} pinnedTab={} activeTab={}",
                    when, pinItemKey, m.layout().pinnedTab(pinItemKey), m.activeTab());
        }
    }

    private static void withMenu(Minecraft mc, java.util.function.Consumer<PackMenu> action) {
        if (mc.player != null && mc.player.containerMenu instanceof PackMenu menu) {
            action.accept(menu);
        }
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                msg -> Packwork.LOGGER.info("[autoshot] {}", msg.getString()));
        Packwork.LOGGER.info("[autoshot] grabbed {}", name);
    }

    private DevAutoShot() {}
}
