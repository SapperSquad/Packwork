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
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/**
 * Dev-only visual harness. With {@code -Dpackwork.autoshot=1} the client boots a
 * throwaway creative world, fills a pack with a spread across every tab, opens it,
 * and drops screenshots into {@code run/screenshots/} so the GUI can be
 * eyeballed headlessly (the gradle dev window can't be driven by desktop tooling).
 * Never runs in production - the client entrypoint only registers this class when
 * the system property is set.
 */
public final class DevAutoShot {

    private static final boolean AUTOSHOT = System.getProperty("packwork.autoshot") != null;
    /** The promo-gallery shoot: a separate, shorter chain that stages the store-page shots. */
    private static final boolean GALLERY = System.getProperty("packwork.gallery") != null;
    /** The worn-render shoot (-Pwornshot -Ptrinkets): third-person back shots of the worn pack. */
    private static final boolean WORNSHOT = System.getProperty("packwork.wornshot") != null;
    private static final boolean ENABLED = AUTOSHOT || GALLERY || WORNSHOT;

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
        KIT_GIVE, KIT_W1, KIT_UNROLL, KIT_W2, SHOOT_ROLL,
        LEDGER_OPEN, LEDGER_W1, SHOOT_LEDGER, LEDGER_CHALK, LEDGER_W2, SHOOT_GHOST,
        LEDGER_LAY, LEDGER_W3, SHOOT_LAID,
        KIT_CRAFT, KIT_W4, KIT_REPORT,
        // ---- wave 4: auto-pin gesture, the rule editor, keep-my-layout, JEI ----
        W4_GIVE, W4_OPEN,
        AUTOPIN_GIVE, AUTOPIN_W1, AUTOPIN_CLICK, AUTOPIN_W2, SHOOT_AUTOPIN,
        RULES_TAB, RULES_W0, RULES_OPEN, RULES_W1, SHOOT_RULES,
        RULES_WRITE, RULES_W2, SHOOT_RULES2,
        MODE_PREP, MODE_W0, MODE_ON, MODE_MOVE1, MODE_MW, MODE_MOVE2, MODE_W2, SHOOT_MODE,
        JEI_SHOW, JEI_W, SHOOT_JEI, DONE,
        // ---- the promo-gallery chain (runs INSTEAD of the above under -Pgallery) ----
        G_BOOT, G_WAIT_LEVEL,
        G_PLACE, G_WPLACE, G_HERO_W, G_SHOOT_LINEUP,
        G_OPEN, G_WOPEN, G_SHOOT_SORTING,
        G_KIT, G_KIT_W, G_UNROLL, G_UNROLL_W, G_LEDGER, G_LEDGER_W, G_CHALK, G_CHALK_W, G_SHOOT_LEDGER,
        G_PACK2, G_PACK2_W, G_TAB, G_TAB_W, G_QUILL, G_QUILL_W, G_WRITE, G_WRITE_W, G_SHOOT_RULES,
        G_KEEP_PREP, G_KEEP_W0, G_KEEP_ON, G_KEEP_MOVE1, G_KEEP_MW, G_KEEP_MOVE2, G_KEEP_W2, G_SHOOT_KEEP,
        G_PICKUP, G_PICKUP_W, G_PICKUP_TAB, G_PICKUP_TW, G_PICKUP_DROP, G_PICKUP_DW, G_SHOOT_PICKUP,
        G_NIGHT, G_NIGHT_W, G_SHOOT_NIGHT,
        G_JEI, G_JEI_W, G_SHOOT_JEI,
        // ---- the worn-render shoot (runs INSTEAD of the above under -Pwornshot) ----
        WS_BOOT, WS_WAIT_LEVEL, WS_STEP, WS_SHOOT
    }

    private static Phase phase = WORNSHOT ? Phase.WS_BOOT : (GALLERY ? Phase.G_BOOT : Phase.BOOT);
    private static int ticks = 0;
    private static int wait = 0;
    private static String customTabId = "custom:0";
    private static net.minecraft.core.BlockPos placedFirst = null;
    private static net.minecraft.core.BlockPos placedMiddle = null;
    private static net.minecraft.core.BlockPos placedLast = null;

    /** Hook the client tick (called from the client entrypoint, property-gated). */
    public static void register() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                DevAutoShot::onTick);
    }

    private static void onTick(Minecraft mcTick) {
        if (!ENABLED || phase == Phase.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;

        switch (phase) {
            case BOOT -> {
                if (ticks == 5) {
                    // Grow the dev window and force GUI scale 3 (what players actually use) so the
                    // search text + slots can be judged at real size, not the tiny default scale.
                    try {
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().handle(), 1120, 900);
                        mc.options.guiScale().set(3);
                        mc.resizeGui();
                        Packwork.LOGGER.info("[autoshot] window 1120x900 @ guiScale 3");
                    } catch (Throwable t) {
                        Packwork.LOGGER.warn("[autoshot] resize failed: {}", t.toString());
                    }
                }
                if (ticks > 40 && mc.level == null && mc.gui.screen() != null) {
                    Packwork.LOGGER.info("[autoshot] creating throwaway world");
                    // 26.1: hardcore/difficulty fold into DifficultySettings; GameRules left the ctor.
                    LevelSettings settings = new LevelSettings("packwork_autoshot", GameType.CREATIVE,
                            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("packwork_autoshot", settings,
                            WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, mc.gui.screen());
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
                if (mc.gui.screen() instanceof PackScreen && ++wait > 12) {
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
                giveCarried(mc, bucketRounds()[bucketRound]);
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
                if (++bucketRound < bucketRounds().length) {
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
                if (mc.gui.screen() instanceof PackScreen ps) ps.devHover(pinSlotIndex);
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
                if (mc.gui.screen() instanceof PackScreen ps) ps.devHover(pinSlotIndex);
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
                if (mc.gui.screen() instanceof OutfitterHandbookScreen book) book.devSelectChapter(2);
                phase = Phase.WB2; wait = 0;
            }
            case WB2 -> { if (++wait > 8) phase = Phase.SHOOT_BOOK2; }
            case SHOOT_BOOK2 -> {
                grab(mc, "packwork_handbook_trinkets");
                mc.gui.setScreen(null);          // close the book
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
                mc.gui.setScreen(null);              // close the GUI, back to the world row
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
                    if (mc.player != null) mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                    phase = Phase.SHOOT_LINEUP; wait = 0;
                }
            }
            case SHOOT_LINEUP -> {
                if (++wait > 6) {
                    grab(mc, "packwork_lineup");  // every pack tier + trinket in the inventory grid
                    mc.gui.setScreen(null);            // drop to first person, holding the Runed pack
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
            case KIT_W1 -> { if (mc.gui.screen() instanceof PackScreen && ++wait > 16) { phase = Phase.KIT_UNROLL; wait = 0; } }
            case KIT_UNROLL -> {
                switchTab(mc, "auto:nature");     // the wheat lives in Nature & Farming
                clickRollLatch(mc);               // a real press + release on the latch
                phase = Phase.KIT_W2; wait = 0;
            }
            case KIT_W2 -> { if (++wait > 14) phase = Phase.SHOOT_ROLL; }
            case SHOOT_ROLL -> {
                grab(mc, "packwork_roll_open");   // the tool roll unrolled across the lower rows
                phase = Phase.LEDGER_OPEN; wait = 0;
            }
            // ---- the Recipe Ledger: browse -> chalk -> lay out -> craft, all real clicks ----
            case LEDGER_OPEN -> {
                clickAt(mc, ps -> ps.devLedgerButtonCenter(), "ledger button");
                phase = Phase.LEDGER_W1; wait = 0;
            }
            case LEDGER_W1 -> { if (++wait > 10) phase = Phase.SHOOT_LEDGER; }
            case SHOOT_LEDGER -> {
                grab(mc, "packwork_ledger");      // the parchment sheet, craftable-from-stock
                phase = Phase.LEDGER_CHALK; wait = 0;
            }
            case LEDGER_CHALK -> {
                clickAt(mc, ps -> ps.devLedgerCellCenter("minecraft:bread"), "bread on the ledger");
                phase = Phase.LEDGER_W2; wait = 0;
            }
            case LEDGER_W2 -> { if (++wait > 10) phase = Phase.SHOOT_GHOST; }
            case SHOOT_GHOST -> {
                grab(mc, "packwork_ledger_ghost"); // the wheat row chalked onto the roll
                phase = Phase.LEDGER_LAY; wait = 0;
            }
            case LEDGER_LAY -> {
                clickAt(mc, ps -> ps.devResultWellCenter(), "the result well (lay out from stock)");
                phase = Phase.LEDGER_W3; wait = 0;
            }
            case LEDGER_W3 -> { if (++wait > 14) phase = Phase.SHOOT_LAID; }
            case SHOOT_LAID -> {
                grab(mc, "packwork_ledger_laid"); // real wheat on the roll now, bread in the well
                logKitState(mc, "after lay-out");
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
                phase = Phase.W4_GIVE;
                wait = 0;
            }
            // ---- wave 4: a fresh full pack, then the four new behaviours as pixels ----
            case W4_GIVE -> {
                setupAndOpen(mc);   // the Sculkhide spread again (now with a Quill & Ledger fitted)
                phase = Phase.W4_OPEN; wait = 0;
            }
            case W4_OPEN -> { if (mc.gui.screen() instanceof PackScreen && ++wait > 16) { phase = Phase.AUTOPIN_GIVE; wait = 0; } }
            case AUTOPIN_GIVE -> {
                switchTab(mc, "auto:ores");
                giveCarried(mc, new ItemStack(Items.BREAD, 5));  // bread does NOT belong in Ores
                phase = Phase.AUTOPIN_W1; wait = 0;
            }
            case AUTOPIN_W1 -> { if (++wait > 10) { phase = Phase.AUTOPIN_CLICK; wait = 0; } }
            case AUTOPIN_CLICK -> {
                clickViewSlot(mc, false, 0, "drop bread into an empty Ores cell (auto-pin)");
                phase = Phase.AUTOPIN_W2; wait = 0;
            }
            case AUTOPIN_W2 -> { if (++wait > 12) phase = Phase.SHOOT_AUTOPIN; }
            case SHOOT_AUTOPIN -> {
                withMenu(mc, m -> Packwork.LOGGER.info("[autoshot][autopin] bread pinnedTab={} (want auto:ores)",
                        m.layout().pinnedTab(net.minecraft.resources.Identifier.withDefaultNamespace("bread"))));
                grab(mc, "packwork_autopin");     // bread in Ores + red ribbon + the parchment note
                phase = Phase.RULES_TAB; wait = 0;
            }
            case RULES_TAB -> {
                newTab(mc);                        // a custom tab, selected
                phase = Phase.RULES_W0; wait = 0;
            }
            case RULES_W0 -> { if (++wait > 8) { phase = Phase.RULES_OPEN; wait = 0; } }
            case RULES_OPEN -> {
                clickAt(mc, PackScreen::devQuillButtonCenter, "the quill (rule editor)");
                phase = Phase.RULES_W1; wait = 0;
            }
            case RULES_W1 -> { if (++wait > 10) phase = Phase.SHOOT_RULES; }
            case SHOOT_RULES -> {
                grab(mc, "packwork_rules_empty"); // the parchment sheet: stamp line, chips, no rules yet
                phase = Phase.RULES_WRITE; wait = 0;
            }
            case RULES_WRITE -> {
                if (mc.gui.screen() instanceof PackScreen ps) ps.devSetRuleValue("ingot");
                clickAt(mc, PackScreen::devRuleAddNameCenter, "file 'ingot' by name");
                clickAt(mc, ps -> ps.devRuleChipCenter(0), "the Food category chip");
                phase = Phase.RULES_W2; wait = 0;
            }
            case RULES_W2 -> { if (++wait > 12) phase = Phase.SHOOT_RULES2; }
            case SHOOT_RULES2 -> {
                withMenu(mc, m -> {
                    var def = m.layout().customTab(m.activeTab());
                    Packwork.LOGGER.info("[autoshot][rules] tab {} rules={}", m.activeTab(),
                            def == null ? "null" : def.rules());
                });
                grab(mc, "packwork_rules_written"); // two written rules on the sheet
                phase = Phase.MODE_PREP; wait = 0;
            }
            case MODE_PREP -> {
                switchTab(mc, "auto:ores");        // the sheet folds away (not a custom tab)
                phase = Phase.MODE_W0; wait = 0;
            }
            case MODE_W0 -> { if (++wait > 8) { phase = Phase.MODE_ON; wait = 0; } }
            case MODE_ON -> {
                clickAt(mc, PackScreen::devModeButtonCenter, "the arrangement switch (keep my layout)");
                phase = Phase.MODE_MOVE1; wait = 0;
            }
            case MODE_MOVE1 -> {
                if (++wait > 8) {
                    clickViewSlot(mc, true, 0, "pick up the first Ores stack");
                    phase = Phase.MODE_MW; wait = 0;
                }
            }
            case MODE_MW -> { if (++wait > 6) { phase = Phase.MODE_MOVE2; wait = 0; } }
            case MODE_MOVE2 -> {
                clickViewSlot(mc, false, 12, "set it down two rows lower (kept cell)");
                phase = Phase.MODE_W2; wait = 0;
            }
            case MODE_W2 -> { if (++wait > 12) phase = Phase.SHOOT_MODE; }
            case SHOOT_MODE -> {
                withMenu(mc, m -> {
                    var kept = m.layout().manualFor(m.activeTab());
                    Packwork.LOGGER.info("[autoshot][mode] tab {} kept={}", m.activeTab(),
                            kept == null ? "TIDY (bad!)" : kept.cells());
                });
                grab(mc, "packwork_keep_layout"); // the moved stack holds its far cell, switch lit
                phase = Phase.JEI_SHOW; wait = 0;
            }
            case JEI_SHOW -> {
                if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
                    Packwork.LOGGER.info("[autoshot][jei] JEI absent - skipping the recipe shot");
                    phase = Phase.DONE;
                    Packwork.LOGGER.info("[autoshot] done - screenshots written");
                } else {
                    showJeiUpgrade(mc);
                    phase = Phase.JEI_W; wait = 0;
                }
            }
            case JEI_W -> { if (++wait > 25) phase = Phase.SHOOT_JEI; }
            case SHOOT_JEI -> {
                grab(mc, "packwork_jei_upgrade"); // the Studded pack's REAL recipe in JEI's own category
                phase = Phase.DONE;
                Packwork.LOGGER.info("[autoshot] done - screenshots written");
            }
            // ================= the promo-gallery chain (-Pgallery) =================
            case G_BOOT -> {
                if (ticks == 5) {
                    try {
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().handle(), 1920, 1080);
                        mc.options.guiScale().set(3);
                        mc.resizeGui();
                        Packwork.LOGGER.info("[gallery] window 1920x1080 @ guiScale 3");
                    } catch (Throwable t) {
                        Packwork.LOGGER.warn("[gallery] resize failed: {}", t.toString());
                    }
                }
                if (ticks > 40 && mc.level == null && mc.gui.screen() != null) {
                    Packwork.LOGGER.info("[gallery] creating throwaway world");
                    // 26.1: hardcore/difficulty fold into DifficultySettings; GameRules left the ctor.
                    LevelSettings settings = new LevelSettings("packwork_autoshot", GameType.CREATIVE,
                            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("packwork_autoshot", settings,
                            WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, mc.gui.screen());
                    phase = Phase.G_WAIT_LEVEL;
                    wait = 0;
                }
            }
            case G_WAIT_LEVEL -> {
                if (mc.player != null && mc.getSingleplayerServer() != null && ++wait > 60) {
                    phase = Phase.G_PLACE;
                    wait = 0;
                }
            }
            case G_PLACE -> {
                placeBlocks(mc, 64);          // the six-tier row on a SKY pad, midday - clean backdrop
                                              // (64, not 26: the angled hero camera looks OVER the row,
                                              // and one seed grew a hilltop tree into that sightline)
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();    // a clean world shot: no hotbar, no crosshair
                phase = Phase.G_WPLACE; wait = 0;
            }
            case G_WPLACE -> {
                if (++wait > 50) {
                    heroCam(mc);              // step in close - the stand-back framing read as a test shot
                    phase = Phase.G_HERO_W; wait = 0;
                }
            }
            case G_HERO_W -> { if (++wait > 30) phase = Phase.G_SHOOT_LINEUP; }
            case G_SHOOT_LINEUP -> {
                grab(mc, "gallery_lineup");   // (1) all SIX per-tier packs, daylight
                if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                phase = Phase.G_OPEN; wait = 0;
            }
            case G_OPEN -> {
                setupAndOpen(mc);             // the filled Sculkhide pack: tabs, sockets, gauges
                phase = Phase.G_WOPEN; wait = 0;
            }
            case G_WOPEN -> {
                if (wait == 1) parkCursor(mc);   // no hover highlight in the promo frame
                if (mc.gui.screen() instanceof PackScreen && ++wait > 16) { phase = Phase.G_SHOOT_SORTING; wait = 0; }
            }
            case G_SHOOT_SORTING -> {
                grab(mc, "gallery_sorting");  // (2) the sorting flagship, mid-sort
                phase = Phase.G_KIT; wait = 0;
            }
            case G_KIT -> { giveKitPack(mc); phase = Phase.G_KIT_W; wait = 0; }
            case G_KIT_W -> { if (mc.gui.screen() instanceof PackScreen && ++wait > 16) { phase = Phase.G_UNROLL; wait = 0; } }
            case G_UNROLL -> {
                switchTab(mc, "auto:nature");
                clickRollLatch(mc);
                phase = Phase.G_UNROLL_W; wait = 0;
            }
            case G_UNROLL_W -> { if (++wait > 14) { phase = Phase.G_LEDGER; wait = 0; } }
            case G_LEDGER -> { clickAt(mc, PackScreen::devLedgerButtonCenter, "ledger button"); phase = Phase.G_LEDGER_W; wait = 0; }
            case G_LEDGER_W -> { if (++wait > 10) { phase = Phase.G_CHALK; wait = 0; } }
            case G_CHALK -> {
                clickAt(mc, ps -> ps.devLedgerCellCenter("minecraft:bread"), "bread on the ledger");
                phase = Phase.G_CHALK_W; wait = 0;
            }
            case G_CHALK_W -> {
                if (wait == 1) parkCursor(mc);
                if (++wait > 10) phase = Phase.G_SHOOT_LEDGER;
            }
            case G_SHOOT_LEDGER -> {
                grab(mc, "gallery_ledger");   // (3) the Recipe Ledger with a ghost chalked
                phase = Phase.G_PACK2; wait = 0;
            }
            case G_PACK2 -> { setupAndOpen(mc); phase = Phase.G_PACK2_W; wait = 0; }
            case G_PACK2_W -> { if (mc.gui.screen() instanceof PackScreen && ++wait > 16) { phase = Phase.G_TAB; wait = 0; } }
            case G_TAB -> { newTab(mc); phase = Phase.G_TAB_W; wait = 0; }
            case G_TAB_W -> { if (++wait > 8) { phase = Phase.G_QUILL; wait = 0; } }
            case G_QUILL -> { clickAt(mc, PackScreen::devQuillButtonCenter, "the quill"); phase = Phase.G_QUILL_W; wait = 0; }
            case G_QUILL_W -> { if (++wait > 10) { phase = Phase.G_WRITE; wait = 0; } }
            case G_WRITE -> {
                if (mc.gui.screen() instanceof PackScreen ps) ps.devSetRuleValue("ingot");
                clickAt(mc, PackScreen::devRuleAddNameCenter, "file 'ingot' by name");
                clickAt(mc, ps -> ps.devRuleChipCenter(0), "the Food chip");
                phase = Phase.G_WRITE_W; wait = 0;
            }
            case G_WRITE_W -> {
                if (wait == 1) parkCursor(mc);
                if (++wait > 12) phase = Phase.G_SHOOT_RULES;
            }
            case G_SHOOT_RULES -> {
                grab(mc, "gallery_rules");    // (4) the Quill & Ledger's rule sheet, written on
                phase = Phase.G_KEEP_PREP; wait = 0;
            }
            case G_KEEP_PREP -> { switchTab(mc, "auto:ores"); phase = Phase.G_KEEP_W0; wait = 0; }
            case G_KEEP_W0 -> { if (++wait > 8) { phase = Phase.G_KEEP_ON; wait = 0; } }
            case G_KEEP_ON -> { clickAt(mc, PackScreen::devModeButtonCenter, "keep-my-layout"); phase = Phase.G_KEEP_MOVE1; wait = 0; }
            case G_KEEP_MOVE1 -> {
                if (++wait > 8) {
                    clickViewSlot(mc, true, 0, "pick up the first Ores stack");
                    phase = Phase.G_KEEP_MW; wait = 0;
                }
            }
            case G_KEEP_MW -> { if (++wait > 6) { phase = Phase.G_KEEP_MOVE2; wait = 0; } }
            case G_KEEP_MOVE2 -> { clickViewSlot(mc, false, 12, "set it down two rows lower"); phase = Phase.G_KEEP_W2; wait = 0; }
            case G_KEEP_W2 -> {
                if (wait == 1) parkCursor(mc);
                if (++wait > 12) phase = Phase.G_SHOOT_KEEP;
            }
            case G_SHOOT_KEEP -> {
                grab(mc, "gallery_keep");     // (5) keep-my-layout: the moved stack holds its cell
                phase = Phase.G_PICKUP; wait = 0;
            }
            case G_PICKUP -> { giveLodestonePackAndOpen(mc); phase = Phase.G_PICKUP_W; wait = 0; }
            case G_PICKUP_W -> { if (mc.gui.screen() instanceof PackScreen && ++wait > 16) { phase = Phase.G_PICKUP_TAB; wait = 0; } }
            case G_PICKUP_TAB -> { switchTab(mc, "auto:ores"); phase = Phase.G_PICKUP_TW; wait = 0; }
            case G_PICKUP_TW -> { if (++wait > 8) { phase = Phase.G_PICKUP_DROP; wait = 0; } }
            case G_PICKUP_DROP -> { giveCarried(mc, new ItemStack(Items.BREAD, 5)); phase = Phase.G_PICKUP_DW; wait = 0; }
            case G_PICKUP_DW -> {
                if (++wait > 12) {
                    clickViewSlot(mc, false, 0, "drop bread into Ores (auto-pin)");
                    phase = Phase.G_SHOOT_PICKUP; wait = 0;
                }
            }
            case G_SHOOT_PICKUP -> {
                if (wait == 1) parkCursor(mc);
                if (++wait > 12) {
                    grab(mc, "gallery_pickup_pin"); // (8) pickup toggle lit + the pin note + ribbon
                    phase = Phase.G_NIGHT; wait = 0;
                }
            }
            case G_NIGHT -> {
                mc.gui.setScreen(null);
                var server = mc.getSingleplayerServer();
                if (server != null) server.execute(() -> {
                    if (server.getPlayerList().getPlayers().isEmpty()) return;
                    ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                    setWorldTime(server, 18000); // 26.1: day time rides the clock manager now
                    // Step in close on the Sculkhide so its echo-gem glow carries the frame.
                    // The Sculkhide now sits at the +0 (west) end of the row, so the camera
                    // stands on its EAST side (+1.8 past the block coord = +1.3 past centre,
                    // the mirror of the old -1.3) and yaw mirrors to -20: the glowing pack
                    // rides near frame-right and the row recedes left, still Canvas->Sculkhide
                    // ascending for the viewer.
                    if (placedLast != null) {
                        sp.connection.teleport(placedLast.getX() + 1.8, placedLast.getY(),
                                placedLast.getZ() - 2.4, -20f, 16f);
                    }
                });
                if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                phase = Phase.G_NIGHT_W; wait = 0;
            }
            case G_NIGHT_W -> { if (++wait > 70) phase = Phase.G_SHOOT_NIGHT; } // sky + light re-render
            case G_SHOOT_NIGHT -> {
                grab(mc, "gallery_sculkhide_night"); // (6) the glowing tiers in the dark
                if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                phase = Phase.G_JEI; wait = 0;
            }
            case G_JEI -> {
                if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei")) {
                    Packwork.LOGGER.info("[gallery] JEI absent - skipping the ring shot");
                    phase = Phase.DONE;
                    Packwork.LOGGER.info("[gallery] done - shots written");
                } else {
                    showJeiUpgrade(mc);
                    phase = Phase.G_JEI_W; wait = 0;
                }
            }
            case G_JEI_W -> { if (++wait > 25) phase = Phase.G_SHOOT_JEI; }
            case G_SHOOT_JEI -> {
                grab(mc, "gallery_jei_ring");  // (7) the upgrade ring in JEI's crafting category
                phase = Phase.DONE;
                Packwork.LOGGER.info("[gallery] done - shots written");
            }
            // ================= the worn-render shoot (-Pwornshot -Ptrinkets) =================
            case WS_BOOT -> {
                if (ticks == 5) {
                    try {
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().handle(), 1280, 900);
                        mc.options.guiScale().set(3);
                        mc.resizeGui();
                        Packwork.LOGGER.info("[wornshot] window 1280x900");
                    } catch (Throwable t) {
                        Packwork.LOGGER.warn("[wornshot] resize failed: {}", t.toString());
                    }
                }
                if (ticks > 40 && mc.level == null && mc.gui.screen() != null) {
                    Packwork.LOGGER.info("[wornshot] creating throwaway world");
                    LevelSettings settings = new LevelSettings("packwork_autoshot", GameType.CREATIVE,
                            new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("packwork_autoshot", settings,
                            WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, mc.gui.screen());
                    phase = Phase.WS_WAIT_LEVEL;
                    wait = 0;
                }
            }
            case WS_WAIT_LEVEL -> {
                if (mc.player != null && mc.getSingleplayerServer() != null && ++wait > 60) {
                    if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
                        Packwork.LOGGER.warn("[wornshot] Trinkets absent - nothing to wear; run with -Ptrinkets");
                        phase = Phase.DONE;
                    } else {
                        wornStage(mc);
                        phase = Phase.WS_STEP;
                    }
                    wait = 0;
                }
            }
            case WS_STEP -> {
                if (++wait > 25) {
                    if (wornShot >= WORN_SHOTS.length) {
                        phase = Phase.DONE;
                        if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                        if (wornFailures > 0) {
                            Packwork.LOGGER.error("[wornshot] done - {} of {} shots REFUSED; the "
                                    + "written frames are the only ones worth looking at",
                                    wornFailures, WORN_SHOTS.length);
                        } else {
                            Packwork.LOGGER.info("[wornshot] done - {} shots written, every scene checked",
                                    WORN_SHOTS.length);
                        }
                    } else {
                        applyWornShot(mc, WORN_SHOTS[wornShot]);
                        phase = Phase.WS_SHOOT;
                        wait = 0;
                    }
                }
            }
            case WS_SHOOT -> {
                if (++wait > 30) {
                    String wrong = wornCheck(mc, WORN_SHOTS[wornShot]);
                    if (wrong != null) {
                        wornFailures++;
                        Packwork.LOGGER.error("[wornshot] {} NOT SHOT - the scene is wrong: {}",
                                WORN_SHOTS[wornShot].name(), wrong);
                    } else {
                        grab(mc, "worn_" + WORN_SHOTS[wornShot].name());
                        Packwork.LOGGER.info("[wornshot] {} -> {}", WORN_SHOTS[wornShot].name(),
                                WORN_SHOTS[wornShot].expect());
                    }
                    wornShot++;
                    phase = Phase.WS_STEP;
                    wait = 0;
                }
            }
            default -> {}
        }
    }

    // ---- waterskin-gauge bucket check (the bug SapperSquad hit: the bucket landed on the ground) ----

    /** One bucket, then a STACK of three, then an empty one to fill back out of the tank.
     *  (26.1: built lazily - constructing an ItemStack in a static initializer now throws
     *  "Components not bound yet", because item components bind to the registry holder
     *  AFTER registration; this class is classloaded during subscriber scanning.) */
    private static ItemStack[] bucketRounds;

    private static ItemStack[] bucketRounds() {
        if (bucketRounds == null) {
            bucketRounds = new ItemStack[] {
                    new ItemStack(Items.WATER_BUCKET),
                    new ItemStack(Items.WATER_BUCKET, 3),
                    new ItemStack(Items.BUCKET)
            };
        }
        return bucketRounds;
    }

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
            sp.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    sp.getBoundingBox().inflate(12)).forEach(net.minecraft.world.entity.Entity::discard);
            sp.containerMenu.setCarried(copy);
            Packwork.LOGGER.info("[autoshot][bucket] round {} - cursor set to {} x{}",
                    bucketRound, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(copy.getItem()), copy.getCount());
        });
    }

    /** Dispatch a genuine press + release at the waterskin gauge - the same pair MouseHandler sends. */
    private static void clickFluidGauge(Minecraft mc) {
        clickAt(mc, PackScreen::devFluidGaugeCenter, "the waterskin gauge");
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
            var ground = sp.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
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
            sp.getInventory().setItem(0, pack);
            sp.getInventory().setSelectedSlot(0);
            PackItem.openPack(sp, 0);
            Packwork.LOGGER.info("[autoshot][kit] Runed pack with Tinker's Kit + Sleeve + Creel + Furnace opened");
        });
    }

    /** Park the real cursor in the bottom-left corner so no hover highlight or tooltip
     *  photobombs a promo frame. {@code glfwSetCursorPos} does NOT fire the move callback,
     *  so MouseHandler's cached xpos/ypos are set reflectively too - dev-only code on a
     *  dev runtime, where the mojmap field names are live. */
    private static void parkCursor(Minecraft mc) {
        double px = 4, py = mc.getWindow().getHeight() - 4;
        try {
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(), px, py);
        } catch (Throwable ignored) {
        }
        try {
            var xf = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");
            var yf = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
            xf.setAccessible(true);
            yf.setAccessible(true);
            xf.setDouble(mc.mouseHandler, px);
            yf.setDouble(mc.mouseHandler, py);
        } catch (Throwable t) {
            Packwork.LOGGER.warn("[gallery] cursor park (reflective) failed: {}", t.toString());
        }
    }

    /** THE click helper: a genuine press + release at a dev-exposed GUI point. Every scripted
     *  click in the harness routes through here (skips politely if the target is missing). */
    private static void clickAt(Minecraft mc, java.util.function.Function<PackScreen, int[]> where, String what) {
        if (!(mc.gui.screen() instanceof PackScreen ps)) {
            Packwork.LOGGER.warn("[autoshot] pack screen not open for {}", what);
            return;
        }
        int[] c = where.apply(ps);
        if (c == null) {
            Packwork.LOGGER.warn("[autoshot] no target for {}", what);
            return;
        }
        // 1.21.9+ input events: press/release carry MouseButtonEvent records now
        var press = new net.minecraft.client.input.MouseButtonEvent(c[0], c[1],
                new net.minecraft.client.input.MouseButtonInfo(0, 0));
        ps.mouseClicked(press, false);
        ps.mouseReleased(press);
        Packwork.LOGGER.info("[autoshot] clicked {} at ({},{})", what, c[0], c[1]);
    }

    /** A genuine press + release on the tool-roll latch in the title strip. */
    private static void clickRollLatch(Minecraft mc) {
        clickAt(mc, PackScreen::devRollButtonCenter, "the roll latch");
    }

    /**
     * A REAL container click (local apply + the server packet, exactly what a mouse does)
     * on the first active grid cell at/after {@code minIndex} that is {@code wantItem}-ful.
     * This is the path that exercises setByPlayer -&gt; the post-click flush on both sides.
     */
    private static void clickViewSlot(Minecraft mc, boolean wantItem, int minIndex, String what) {
        if (!(mc.gui.screen() instanceof PackScreen ps) || mc.player == null || mc.gameMode == null) {
            Packwork.LOGGER.warn("[autoshot] pack screen not open for {}", what);
            return;
        }
        PackMenu menu = ps.getMenu();
        for (int i = minIndex; i < menu.slots.size(); i++) {
            net.minecraft.world.inventory.Slot s = menu.slots.get(i);
            if (s instanceof com.sappersquad.packwork.pack.PackViewSlot vs && vs.isActive()
                    && s.hasItem() == wantItem) {
                mc.gameMode.handleContainerInput(menu.containerId, i, 0,
                        net.minecraft.world.inventory.ContainerInput.PICKUP, mc.player);
                Packwork.LOGGER.info("[autoshot] {} -> menu slot {}", what, i);
                return;
            }
        }
        Packwork.LOGGER.warn("[autoshot] no grid cell found for {}", what);
    }

    /** JEI's recipe view on the pack ladder - reached by reflection so this class never
     *  references a JEI type (the compat rule: one class imports each mod). */
    private static void showJeiUpgrade(Minecraft mc) {
        try {
            Class.forName("com.sappersquad.packwork.compat.jei.PackworkJeiPlugin")
                    .getMethod("devShowUpgradeRecipes").invoke(null);
            Packwork.LOGGER.info("[autoshot][jei] asked JEI for the Studded pack's recipes");
        } catch (Throwable t) {
            Packwork.LOGGER.warn("[autoshot][jei] failed: {}", t.toString());
        }
    }

    /** A real shift-click on the roll's result slot. */
    private static void craftFromRoll(Minecraft mc) {
        if (!(mc.gui.screen() instanceof PackScreen ps) || mc.player == null) return;
        PackMenu menu = ps.getMenu();
        mc.gameMode.handleContainerInput(menu.containerId, menu.resultIndex(), 0,
                net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player);
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
                for (int i = m.craftStart(); i <= m.resultIndex(); i++) {
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

            // a Sculkhide pack (5 trinket sockets) so all four store gauges + the depth are on show
            ItemStack pack = new ItemStack(ModItems.pack(com.sappersquad.packwork.pack.PackTier.SCULKHIDE).get());
            // (1.21.11: the standard item cap is the transactional ResourceHandler now; the
            // harness just needs to stuff the pack, so it uses the internal store directly -
            // the cap itself is exercised by the automation gametests)
            var h = new com.sappersquad.packwork.pack.PackInventory(
                    pack, com.sappersquad.packwork.pack.PackTier.SCULKHIDE);
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
            // deepen the cobblestone slot to its full 384 (6 stacks in one slot) so the GUI's
            // true-count rendering can be judged as pixels
            for (int k = 0; k < 5; k++) h.insertItem(22, new ItemStack(Items.COBBLESTONE, 64), false);

            // fit all four store trinkets so every gauge on the right rail shows a level
            var sockets = new com.sappersquad.packwork.pack.PackTrinketInventory(
                    () -> pack, com.sappersquad.packwork.pack.PackTier.SCULKHIDE);
            sockets.insertItem(0, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.WATERSKIN).get()), false);
            sockets.insertItem(1, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.SOUL_VIAL).get()), false);
            sockets.insertItem(2, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL).get()), false);
            sockets.insertItem(3, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.FLASK_HARNESS).get()), false);
            sockets.insertItem(4, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.QUILL_LEDGER).get()), false);
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
            tank.fill(net.minecraft.world.level.material.Fluids.WATER,
                    com.sappersquad.packwork.pack.PackFluidHandler.capacityFor(pack) / 2, false);

            sp.getInventory().setItem(0, pack);
            sp.getInventory().setSelectedSlot(0);

            // Trinkets (optional): prove the pack wears in the back slot (gated; logs the result).
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets")) {
                com.sappersquad.packwork.compat.trinkets.TrinketsCompat.devEquip(sp, pack.copy());
            }

            PackItem.openPack(sp, 0);
            Packwork.LOGGER.info("[autoshot] pack filled and opened");
        });
    }

    /** Set all per-tier pack blocks on a stone-brick pad and stand the player back to view them. */
    private static void placeBlocks(Minecraft mc) {
        placeBlocks(mc, 0);
    }

    /**
     * @param rise blocks to lift the pad above the player - the gallery shoot raises it into
     *             open sky so the backdrop is guaranteed clean whatever terrain the random
     *             seed dealt (a ground-level pad once spawned walled inside a hillside).
     */
    /** 26.1 moved world time onto the clock system: set the OVERWORLD clock's total ticks
     *  (what {@code /time set} does now - verified in the 26.1.2 TimeCommand sources). */
    private static void setWorldTime(net.minecraft.server.MinecraftServer server, long ticks) {
        var clock = server.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD);
        server.clockManager().setTotalTicks(clock, ticks);
    }

    private static void placeBlocks(Minecraft mc, int rise) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            net.minecraft.server.level.ServerLevel lvl = sp.level();
            setWorldTime(server, 6000); // 26.1: day time rides the clock manager now
            net.minecraft.core.BlockPos base = sp.blockPosition().above(rise);
            var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            var floor = net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
            for (int dx = -2; dx <= 7; dx++)
                for (int dz = -3; dz <= 6; dz++) {
                    for (int dy = 0; dy <= 4; dy++) lvl.setBlock(base.offset(dx, dy, dz), air, 2);
                    lvl.setBlock(base.offset(dx, -1, dz), floor, 2);
                }
            // One of every tier, in ladder order, so the per-tier trim can be compared side by
            // side. The row is laid EAST-to-WEST descending (Canvas at +x, Sculkhide at +0)
            // because the cameras face south and east is frame-LEFT: the viewer reads
            // Canvas -> Sculkhide left-to-right, ascending — the same direction as the banner.
            com.sappersquad.packwork.pack.PackTier[] tiers =
                    com.sappersquad.packwork.pack.PackTier.values();
            for (int i = 0; i < tiers.length; i++) {
                net.minecraft.core.BlockPos bp = base.offset(tiers.length - 1 - i, 0, 4);
                placePackBlock(lvl, bp, tiers[i], i == 2); // give the middle (Studded) a loadout for the GUI shot
                if (i == 0) placedFirst = bp;              // the Canvas pack, at the +x end
                if (i == 2) placedMiddle = bp;
                if (i == tiers.length - 1) placedLast = bp; // the Sculkhide one, for the night shot + break/replace check
            }
            // stand back and centre the five-wide row
            sp.connection.teleport(base.getX() + 2.5, base.getY(), base.getZ() - 2.5, 0f, 14f);
            Packwork.LOGGER.info("[autoshot] placed {} per-tier pack blocks", tiers.length);
        });
    }

    /**
     * The hero framing for the daylight lineup: step in close at the Canvas end (+x) and look
     * up the ladder, the same slightly-low slightly-angled vantage as the night shot - the
     * near pack carries the frame and the tiers ascend left-to-right, Canvas to Sculkhide,
     * matching the banner. The dead-on stand-back position that placeBlocks leaves the
     * player at read as a test screenshot.
     */
    private static void heroCam(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null || placedFirst == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            sp.connection.teleport(placedFirst.getX() - 0.7, placedFirst.getY(),
                    placedFirst.getZ() - 2.1, 22f, 14f);
            Packwork.LOGGER.info("[gallery] hero camera set for the lineup");
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
            net.minecraft.server.level.ServerLevel lvl = server.getPlayerList().getPlayers().get(0).level();
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
            net.minecraft.server.level.ServerLevel lvl = server.getPlayerList().getPlayers().get(0).level();
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
            if (sp.level().getBlockEntity(placedMiddle)
                    instanceof com.sappersquad.packwork.block.PackContainerBlockEntity be) {
                net.minecraft.core.BlockPos pos = placedMiddle;
                sp.openMenu(new net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<com.sappersquad.packwork.net.PackOpenData>() {
                    @Override
                    public net.minecraft.network.chat.Component getDisplayName() {
                        return be.getPackStack().getHoverName();
                    }

                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                            int id, net.minecraft.world.entity.player.Inventory inv,
                            net.minecraft.world.entity.player.Player pl) {
                        return PackMenu.serverForBlock(id, inv, be);
                    }

                    @Override
                    public com.sappersquad.packwork.net.PackOpenData getScreenOpeningData(ServerPlayer p) {
                        return com.sappersquad.packwork.net.PackOpenData.block(pos, be.getTier().ordinal());
                    }
                });
            }
        });
    }

    /** A Leather pack with just a Lodestone, opened - the pack-first pickup toggle's stage. */
    private static void giveLodestonePackAndOpen(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null || server.getPlayerList().getPlayers().isEmpty()) return;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            sp.getInventory().clearContent();
            ItemStack pack = new ItemStack(ModItems.pack(com.sappersquad.packwork.pack.PackTier.LEATHER).get());
            new com.sappersquad.packwork.pack.PackTrinketInventory(
                    () -> pack, com.sappersquad.packwork.pack.PackTier.LEATHER).insertItem(0,
                    new ItemStack(ModItems.trinket(
                            com.sappersquad.packwork.trinket.TrinketType.LODESTONE).get()), false);
            var store = new com.sappersquad.packwork.pack.PackInventory(
                    pack, com.sappersquad.packwork.pack.PackTier.LEATHER);
            store.insertItem(0, new ItemStack(Items.IRON_INGOT, 24), false);
            store.insertItem(1, new ItemStack(Items.RAW_IRON, 17), false);
            sp.getInventory().setItem(0, pack);
            sp.getInventory().setSelectedSlot(0);
            PackItem.openPack(sp, 0);
            Packwork.LOGGER.info("[gallery] Lodestone pack opened for the pickup shot");
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
            sp.getInventory().setItem(0, new ItemStack(ModItems.pack(com.sappersquad.packwork.pack.PackTier.SCULKHIDE).get()));
            sp.getInventory().setSelectedSlot(0);
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

    private static net.minecraft.resources.Identifier pinItemKey = null;
    private static int pinSlotIndex = -1;

    /** Find the first grid item, record its menu-slot index + key, and move the real cursor over it. */
    private static net.minecraft.resources.Identifier hoverFirstGridItem(Minecraft mc) {
        pinSlotIndex = -1;
        if (!(mc.gui.screen() instanceof PackScreen ps)) return null;
        var slots = ps.getMenu().slots;
        for (int i = 0; i < slots.size(); i++) {
            net.minecraft.world.inventory.Slot s = slots.get(i);
            if (s instanceof com.sappersquad.packwork.pack.PackViewSlot vs && vs.isActive() && s.hasItem()) {
                pinSlotIndex = i;
                double scale = mc.getWindow().getGuiScale();
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().handle(),
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
        if (mc.gui.screen() == null) return;
        int scan = org.lwjgl.glfw.GLFW.glfwGetKeyScancode(org.lwjgl.glfw.GLFW.GLFW_KEY_P);
        boolean handled = mc.gui.screen().keyPressed(
                new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_P, scan, 0));
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

    // =====================================================================
    // the worn-render shoot (-Pwornshot -Ptrinkets)
    // =====================================================================

    /**
     * One framed check of the worn pack. The chest supplier is deliberately LAZY:
     * this table is a static field on a class the client entrypoint touches, and building
     * an ItemStack while the registries are still binding is a crash.
     */
    private record WornShot(String name, com.sappersquad.packwork.pack.PackTier tier,
                            java.util.function.Supplier<ItemStack> chest, boolean crouch,
                            boolean frontCam, boolean show, String expect) {}

    private static final WornShot[] WORN_SHOTS = {
            new WornShot("canvas_back", com.sappersquad.packwork.pack.PackTier.CANVAS,
                    () -> ItemStack.EMPTY, false, false, true,
                    "Canvas on the back: weave + twine facing out, sitting on the shoulders"),
            new WornShot("sculkhide_back", com.sappersquad.packwork.pack.PackTier.SCULKHIDE,
                    () -> ItemStack.EMPTY, false, false, true,
                    "Sculkhide: echo veins facing out, no gap at the spine"),
            new WornShot("sculkhide_chestplate", com.sappersquad.packwork.pack.PackTier.SCULKHIDE,
                    () -> new ItemStack(Items.DIAMOND_CHESTPLATE), false, false, true,
                    "rides a hair further out over plate - no z-fighting with the chestplate"),
            new WornShot("sculkhide_crouch", com.sappersquad.packwork.pack.PackTier.SCULKHIDE,
                    () -> ItemStack.EMPTY, true, false, true,
                    "tips forward with the torso, still seated on the back"),
            new WornShot("sculkhide_front", com.sappersquad.packwork.pack.PackTier.SCULKHIDE,
                    () -> ItemStack.EMPTY, false, true, true,
                    "front view: nothing pokes through the chest"),
            new WornShot("sculkhide_elytra", com.sappersquad.packwork.pack.PackTier.SCULKHIDE,
                    () -> new ItemStack(Items.ELYTRA), false, false, true,
                    "EXPECT NO PACK - wings own the back"),
            new WornShot("sculkhide_off", com.sappersquad.packwork.pack.PackTier.SCULKHIDE,
                    () -> ItemStack.EMPTY, false, false, false,
                    "EXPECT NO PACK - show_worn_pack = false"),
    };

    private static int wornShot = 0;
    private static int wornFailures = 0;
    private static net.minecraft.core.BlockPos wornPad = null;

    /** A floating stone-brick pad in open sky, so the backdrop never depends on the seed. */
    private static void wornStage(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            net.minecraft.server.level.ServerLevel lvl = sp.level();
            setWorldTime(server, 6000L);
            net.minecraft.core.BlockPos base = sp.blockPosition().above(48);
            var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            var floor = net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
            for (int dx = -5; dx <= 5; dx++)
                for (int dz = -6; dz <= 5; dz++) {
                    for (int dy = 0; dy <= 6; dy++) lvl.setBlock(base.offset(dx, dy, dz), air, 2);
                    lvl.setBlock(base.offset(dx, -1, dz), floor, 2);
                }
            wornPad = base;
            sp.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0f, 0f);
            Packwork.LOGGER.info("[wornshot] sky pad staged at {}", base);
        });
    }

    private static void applyWornShot(Minecraft mc, WornShot shot) {
        if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        mc.options.setCameraType(shot.frontCam()
                ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT
                : net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        mc.options.keyShift.setDown(shot.crouch());
        com.sappersquad.packwork.config.PackworkConfig.setShowWornPack(shot.show());
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            // Strip FIRST, dress second - and on Fabric the strip reaches FURTHER than it does
            // on NeoForge. Inventory.clearContent() empties the armor and offhand rows (so
            // clearing after equipping quietly wipes the chestplate and the elytra), and
            // Trinkets Updated additionally @Injects into clearContent's TAIL to call
            // LivingEntityTrinketAttachment.clearContents() - so on this branch it also strips
            // the BACK SLOT. Equipping before the clear equips into a slot that is about to be
            // emptied: the server logs "equipped -> Canvas Pack", the clear lands a tick later,
            // and every shot then sees a bare back that looks exactly like a broken renderer.
            // (Curios keeps its inventory off Inventory entirely, which is why the identical
            // ordering is harmless on the six NeoForge branches.)
            sp.getInventory().clearContent();
            sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, shot.chest().get());
            com.sappersquad.packwork.compat.trinkets.TrinketsCompat.devEquip(sp,
                    new ItemStack(ModItems.pack(shot.tier()).get()));
            // Re-anchor every shot: an elytra or a stray nudge drifts the player off the pad,
            // and one drifted frame ruins the shot it lands on.
            if (wornPad != null) {
                sp.connection.teleport(wornPad.getX() + 0.5, wornPad.getY(),
                        wornPad.getZ() + 0.5, 0f, 0f);
            }
        });
    }

    /**
     * Look at the scene before believing the pixels. A worn-render shot is only evidence if
     * the pack is actually worn, the armour the shot asked for is actually on, and the player
     * is actually standing on the pad - and every one of those can be knocked out from
     * OUTSIDE this harness (a stray {@code /clear} strips the back slot and the armour row, a
     * stray {@code /tp} walks the player off the pad, and both were seen happening in a
     * session where another tool was matching dev windows with a loose wildcard). A wrong
     * scene produces a bare back or an empty frame - which is exactly what a broken renderer
     * looks like, so it must never be written as a PNG and mistaken for a finding.
     *
     * @return null when the scene is what the shot asked for, else what is wrong with it
     */
    private static String wornCheck(Minecraft mc, WornShot shot) {
        if (mc.player == null) return "no client player";
        ItemStack worn = com.sappersquad.packwork.compat.trinkets.TrinketsCompat.wornPack(mc.player);
        if (!(worn.getItem() instanceof PackItem)) {
            // Name the SIDE. A bare back has two very different causes - the server never
            // holds the pack (a strip order bug, which is what it turned out to be), or the
            // server holds it and the wearer's client is never told (a sync gap). Printing
            // only the client's view cannot tell them apart, and the wrong one sends the next
            // reader into the render layer, which is fine.
            return "the back slot holds " + worn + ", not a pack (was it cleared?)"
                    + " - CLIENT sees: "
                    + com.sappersquad.packwork.compat.trinkets.TrinketsCompat.devDescribeSlots(mc.player)
                    + " - SERVER sees: " + serverSideSlots(mc);
        }
        var wornTier = PackItem.tierOf(worn);
        if (wornTier != shot.tier()) {
            return "the back slot holds a " + wornTier + " pack, not the " + shot.tier() + " this shot is of";
        }
        ItemStack want = shot.chest().get();
        ItemStack have = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!ItemStack.isSameItem(want, have)) {
            return "the chest slot holds " + have + ", but this shot needs " + want;
        }
        if (wornPad == null) return "the pad was never staged";
        double dx = mc.player.getX() - (wornPad.getX() + 0.5);
        double dz = mc.player.getZ() - (wornPad.getZ() + 0.5);
        double dy = mc.player.getY() - wornPad.getY();
        if (dx * dx + dz * dz > 4.0 || Math.abs(dy) > 3.0) {
            return String.format(java.util.Locale.ROOT,
                    "the player is at %.1f, %.1f, %.1f - off the pad at %d, %d, %d (was it teleported?)",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    wornPad.getX(), wornPad.getY(), wornPad.getZ());
        }
        return null;
    }

    /**
     * The integrated server's view of the same player's trinket slots. Read straight off the
     * server player from the client thread - a race in principle, but this is a dev harness
     * and it only runs on the failure path, where an approximate answer that names the side
     * beats a precise answer that does not.
     */
    private static String serverSideSlots(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return "no integrated server";
        if (server.getPlayerList().getPlayers().isEmpty()) return "no server player";
        return com.sappersquad.packwork.compat.trinkets.TrinketsCompat.devDescribeSlots(
                server.getPlayerList().getPlayers().get(0));
    }

    private static void grab(Minecraft mc, String name) {
        var target = mc.gameRenderer.mainRenderTarget();
        // A minimised or zero-sized window writes a 70-byte PNG that looks like a success in
        // the log and is worthless as evidence. Say so loudly instead.
        if (target.width < 64 || target.height < 64) {
            Packwork.LOGGER.error("[autoshot] render target is {}x{} - {} would be a blank file; "
                    + "is the dev window minimised?", target.width, target.height, name);
            return;
        }
        // 1.21.6+ grab signature carries a downscale factor (1 = full size)
        Screenshot.grab(mc.gameDirectory, name + ".png", target, 1,
                msg -> Packwork.LOGGER.info("[autoshot] {}", msg.getString()));
        Packwork.LOGGER.info("[autoshot] grabbed {} at {}x{}", name, target.width, target.height);
    }

    private DevAutoShot() {}
}
