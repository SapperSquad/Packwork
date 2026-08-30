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

    private static final boolean AUTOSHOT = System.getProperty("packwork.autoshot") != null;
    /** The promo-gallery shoot: a separate, shorter chain that stages the store-page shots. */
    private static final boolean GALLERY = System.getProperty("packwork.gallery") != null;
    /** The worn-render shoot (-Pwornshot -Pcurios): third-person back shots of the worn pack. */
    private static final boolean WORNSHOT = System.getProperty("packwork.wornshot") != null;
    /** The sorting-GIF capture (-Pgifshot): one framebuffer PNG per tick, scripted. */
    private static final boolean GIFSHOT = System.getProperty("packwork.gifshot") != null;
    /** The worn-pack HERO shoot (-Pwornhero -Pcurios): store frames + the turntable clip. */
    private static final boolean WORNHERO = System.getProperty("packwork.wornhero") != null;
    /** The place-at-death micro-clip (-Pdeathclip): you fall, the pack stands where you fell. */
    private static final boolean DEATHCLIP = System.getProperty("packwork.deathclip") != null;
    private static final boolean ENABLED =
            AUTOSHOT || GALLERY || WORNSHOT || GIFSHOT || WORNHERO || DEATHCLIP;

    private enum Phase {
        BOOT, WAIT_LEVEL, OPEN,
        SHOOT_GRID,
        BUCKET_GIVE, BUCKET_W1, BUCKET_CLICK, BUCKET_W2, BUCKET_REPORT,
        HOVER, HW, PIN1, PW1, SHOOT_PINNED, PIN2, PW2, SHOOT_UNPINNED,
        SHOOT_1, W1, SHOOT_2, W2, SHOOT_3, W3, SHOOT_4, W4, SHOOT_5,
        OPEN_BOOK, WB, SHOOT_BOOK, WB2, SHOOT_BOOK2, WB3, SHOOT_BOOK3,
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
        // ---- 1.2.0: the Overflow Valve + Compacting Press, socketed and used ----
        FIT_GIVE, FIT_OPEN, SHOOT_FIT, FIT_MARK, FIT_MW, SHOOT_FIT_MARK,
        FIT_DIAL, FIT_DW, SHOOT_FIT_DIAL,
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
        WS_BOOT, WS_WAIT_LEVEL, WS_STEP, WS_SHOOT,
        // ---- the sorting-GIF capture (runs INSTEAD of the above under -Pgifshot) ----
        GIF_BOOT, GIF_WAIT_LEVEL, GIF_STAGE, GIF_ROLL,
        // ---- the worn-pack HERO shoot (runs INSTEAD of the above under -Pwornhero) ----
        WH_BOOT, WH_WAIT_LEVEL, WH_STEP, WH_SHOOT, WH_SPIN_PREP, WH_SPIN,
        // ---- the place-at-death micro-clip (runs INSTEAD of the above under -Pdeathclip) ----
        DC_BOOT, DC_WAIT_LEVEL, DC_STAGE, DC_ROLL
    }

    private static Phase phase = DEATHCLIP ? Phase.DC_BOOT
            : WORNHERO ? Phase.WH_BOOT
            : GIFSHOT ? Phase.GIF_BOOT
            : WORNSHOT ? Phase.WS_BOOT : (GALLERY ? Phase.G_BOOT : Phase.BOOT);
    private static int ticks = 0;
    private static int wait = 0;
    private static String customTabId = "custom:0";
    private static net.minecraft.core.BlockPos placedFirst = null;
    private static net.minecraft.core.BlockPos placedMiddle = null;
    private static net.minecraft.core.BlockPos placedLast = null;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED || phase == Phase.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        // Never let the window pause itself. Single-player pauses on lost focus, and a
        // capture run that loses focus - to another dev client, to anything - records the
        // Game Menu over a frozen world instead of the scene. It cost a take on the death
        // clip and it would have cost one on any of the others eventually.
        mc.options.pauseOnLostFocus = false;
        // Re-assert the hero shoot's turntable angle every tick, after vanilla's own head/body
        // turn has run - it drags the body back toward the head yaw, so setting it once is not
        // enough. No-op unless the hero shoot is running.
        holdHeroYaw(mc);

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
                // the last chapter: Field Reports, where the clickable links live
                if (mc.screen instanceof OutfitterHandbookScreen book) {
                    book.devSelectChapter(com.sappersquad.packwork.guide.HandbookContent.CHAPTERS.size() - 1);
                }
                phase = Phase.WB3; wait = 0;
            }
            case WB3 -> {
                if (++wait > 8) {
                    // Park the real cursor on the first link so the shot shows its hover state.
                    if (mc.screen instanceof OutfitterHandbookScreen book) {
                        int[] p = book.devFirstLinkPoint();
                        if (p != null) hoverGui(mc, p[0], p[1]);
                        else Packwork.LOGGER.warn("[autoshot] no link found on the Field Reports page");
                    }
                    phase = Phase.SHOOT_BOOK3; wait = 0;
                }
            }
            case SHOOT_BOOK3 -> {
                if (++wait > 6) {
                    grab(mc, "packwork_handbook_field_reports");
                    mc.setScreen(null);          // close the book
                    placeBlocks(mc);             // set three packs down in the world
                    phase = Phase.WPLACE; wait = 0;
                }
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
            case W4_OPEN -> { if (mc.screen instanceof PackScreen && ++wait > 16) { phase = Phase.AUTOPIN_GIVE; wait = 0; } }
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
                        m.layout().pinnedTab(net.minecraft.resources.ResourceLocation.withDefaultNamespace("bread"))));
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
                if (mc.screen instanceof PackScreen ps) ps.devSetRuleValue("ingot");
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
                phase = Phase.FIT_GIVE; wait = 0;
            }
            // ---- 1.2.0: the two new fittings, in their sockets and actually used ----
            case FIT_GIVE -> {
                setupFittings(mc);
                phase = Phase.FIT_OPEN; wait = 0;
            }
            case FIT_OPEN -> {
                if (mc.screen instanceof PackScreen && ++wait > 16) {
                    // flatten: the marking gesture needs the cobblestone ON the visible grid,
                    // and the pack files it under Blocks by default (the first run found no
                    // cobblestone cell and refused both presses, which is the harness working)
                    withMenu(mc, PackClientActions::toggleFlatten);
                    phase = Phase.SHOOT_FIT; wait = 0;
                }
            }
            case SHOOT_FIT -> {
                if (++wait < 8) break;   // let the flatten land before the shot
                // the two new sprites in real trinket sockets on the rail, at GUI size
                grab(mc, "packwork_fittings_1_2");
                phase = Phase.FIT_MARK; wait = 0;
            }
            case FIT_MARK -> {
                pressOverCobble(mc, 0, "O over cobblestone - mark it on the discard list");
                phase = Phase.FIT_MW; wait = 0;
            }
            case FIT_MW -> { if (++wait > 10) phase = Phase.SHOOT_FIT_MARK; }
            case SHOOT_FIT_MARK -> {
                grab(mc, "packwork_valve_mark");   // the stitched note: keeping N stacks
                phase = Phase.FIT_DIAL; wait = 0;
            }
            case FIT_DIAL -> {
                pressOverCobble(mc, org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT,
                        "Shift+O over cobblestone - dial the keep level up a rung");
                phase = Phase.FIT_DW; wait = 0;
            }
            case FIT_DW -> { if (++wait > 10) phase = Phase.SHOOT_FIT_DIAL; }
            case SHOOT_FIT_DIAL -> {
                withMenu(mc, m -> {
                    var id = net.minecraft.resources.ResourceLocation.withDefaultNamespace("cobblestone");
                    Packwork.LOGGER.info("[autoshot][valve] cobblestone listed={} keep={} binnedAtDoor={}",
                            m.layout().listed(id), m.layout().keepStacks(id), m.layout().voids(id));
                });
                grab(mc, "packwork_valve_dial");
                phase = Phase.JEI_SHOW; wait = 0;
            }
            case JEI_SHOW -> {
                if (!net.neoforged.fml.ModList.get().isLoaded("jei")) {
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
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1920, 1080);
                        mc.options.guiScale().set(3);
                        mc.resizeDisplay();
                        Packwork.LOGGER.info("[gallery] window 1920x1080 @ guiScale 3");
                    } catch (Throwable t) {
                        Packwork.LOGGER.warn("[gallery] resize failed: {}", t.toString());
                    }
                }
                if (ticks > 40 && mc.level == null && mc.screen != null) {
                    Packwork.LOGGER.info("[gallery] creating throwaway world");
                    LevelSettings settings = new LevelSettings("packwork_autoshot", GameType.CREATIVE,
                            false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("packwork_autoshot", settings,
                            WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, mc.screen);
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
                mc.options.hideGui = true;    // a clean world shot: no hotbar, no crosshair
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
                mc.options.hideGui = false;
                phase = Phase.G_OPEN; wait = 0;
            }
            case G_OPEN -> {
                setupAndOpen(mc);             // the filled Sculkhide pack: tabs, sockets, gauges
                phase = Phase.G_WOPEN; wait = 0;
            }
            case G_WOPEN -> {
                if (wait == 1) parkCursor(mc);   // no hover highlight in the promo frame
                if (mc.screen instanceof PackScreen && ++wait > 16) { phase = Phase.G_SHOOT_SORTING; wait = 0; }
            }
            case G_SHOOT_SORTING -> {
                grab(mc, "gallery_sorting");  // (2) the sorting flagship, mid-sort
                phase = Phase.G_KIT; wait = 0;
            }
            case G_KIT -> { giveKitPack(mc); phase = Phase.G_KIT_W; wait = 0; }
            case G_KIT_W -> { if (mc.screen instanceof PackScreen && ++wait > 16) { phase = Phase.G_UNROLL; wait = 0; } }
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
            case G_PACK2_W -> { if (mc.screen instanceof PackScreen && ++wait > 16) { phase = Phase.G_TAB; wait = 0; } }
            case G_TAB -> { newTab(mc); phase = Phase.G_TAB_W; wait = 0; }
            case G_TAB_W -> { if (++wait > 8) { phase = Phase.G_QUILL; wait = 0; } }
            case G_QUILL -> { clickAt(mc, PackScreen::devQuillButtonCenter, "the quill"); phase = Phase.G_QUILL_W; wait = 0; }
            case G_QUILL_W -> { if (++wait > 10) { phase = Phase.G_WRITE; wait = 0; } }
            case G_WRITE -> {
                if (mc.screen instanceof PackScreen ps) ps.devSetRuleValue("ingot");
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
            case G_PICKUP_W -> { if (mc.screen instanceof PackScreen && ++wait > 16) { phase = Phase.G_PICKUP_TAB; wait = 0; } }
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
                mc.setScreen(null);
                var server = mc.getSingleplayerServer();
                if (server != null) server.execute(() -> {
                    if (server.getPlayerList().getPlayers().isEmpty()) return;
                    ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                    sp.serverLevel().setDayTime(18000);
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
                mc.options.hideGui = true;
                phase = Phase.G_NIGHT_W; wait = 0;
            }
            case G_NIGHT_W -> { if (++wait > 70) phase = Phase.G_SHOOT_NIGHT; } // sky + light re-render
            case G_SHOOT_NIGHT -> {
                grab(mc, "gallery_sculkhide_night"); // (6) the glowing tiers in the dark
                mc.options.hideGui = false;
                phase = Phase.G_JEI; wait = 0;
            }
            case G_JEI -> {
                if (!net.neoforged.fml.ModList.get().isLoaded("jei")) {
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
            // ================= the worn-render shoot (-Pwornshot -Pcurios) =================
            case WS_BOOT -> {
                if (ticks == 5) {
                    try {
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1920, 1080);
                        mc.options.guiScale().set(3);
                        // The third-person camera sits a fixed 4 blocks back, so the only way to
                        // fill the frame with the player is a long lens. FOV 30 turns a test
                        // screenshot into a shot you can actually judge trim on - and post.
                        mc.options.fov().set(38);
                        mc.resizeDisplay();
                        Packwork.LOGGER.info("[wornshot] window 1920x1080 @ fov 38");
                    } catch (Throwable t) {
                        Packwork.LOGGER.warn("[wornshot] resize failed: {}", t.toString());
                    }
                }
                if (ticks > 40 && mc.level == null && mc.screen != null) {
                    Packwork.LOGGER.info("[wornshot] creating throwaway world");
                    LevelSettings settings = new LevelSettings("packwork_autoshot", GameType.CREATIVE,
                            false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("packwork_autoshot", settings,
                            WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, mc.screen);
                    phase = Phase.WS_WAIT_LEVEL;
                    wait = 0;
                }
            }
            case WS_WAIT_LEVEL -> {
                if (mc.player != null && mc.getSingleplayerServer() != null && ++wait > 60) {
                    if (!net.neoforged.fml.ModList.get().isLoaded("curios")) {
                        Packwork.LOGGER.warn("[wornshot] Curios absent - nothing to wear; run with -Pcurios");
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
                        mc.options.hideGui = false;
                        Packwork.LOGGER.info("[wornshot] done - {} shots written", WORN_SHOTS.length);
                    } else {
                        applyWornShot(mc, WORN_SHOTS[wornShot]);
                        phase = Phase.WS_SHOOT;
                        wait = 0;
                    }
                }
            }
            case WS_SHOOT -> {
                if (++wait > 30) {
                    grab(mc, "worn_" + WORN_SHOTS[wornShot].name());
                    Packwork.LOGGER.info("[wornshot] {} -> {}", WORN_SHOTS[wornShot].name(),
                            WORN_SHOTS[wornShot].expect());
                    wornShot++;
                    phase = Phase.WS_STEP;
                    wait = 0;
                }
            }
            // ================= the place-at-death micro-clip (-Pdeathclip) =================
            case DC_BOOT -> {
                if (ticks == 5) {
                    try {
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
                        mc.options.guiScale().set(2);
                        mc.options.fov().set(50);
                        mc.resizeDisplay();
                        Packwork.LOGGER.info("[deathclip] window 1280x720 @ fov 50");
                    } catch (Throwable t) {
                        Packwork.LOGGER.warn("[deathclip] resize failed: {}", t.toString());
                    }
                }
                if (ticks > 40 && mc.level == null && mc.screen != null) {
                    LevelSettings settings = new LevelSettings("packwork_autoshot", GameType.CREATIVE,
                            false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("packwork_autoshot", settings,
                            WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, mc.screen);
                    phase = Phase.DC_WAIT_LEVEL;
                    wait = 0;
                }
            }
            case DC_WAIT_LEVEL -> {
                if (mc.player != null && mc.getSingleplayerServer() != null && ++wait > 60) {
                    deathStage(mc);
                    phase = Phase.DC_STAGE;
                    wait = 0;
                }
            }
            case DC_STAGE -> { if (++wait > 30) { phase = Phase.DC_ROLL; deathFrame = 0; wait = 0; } }
            case DC_ROLL -> {
                if (deathFrame >= DEATH_FRAMES) {
                    phase = Phase.DONE;
                    mc.options.hideGui = false;
                    Packwork.LOGGER.info("[deathclip] done - {} frames in {}", deathFrame, spinDir);
                    break;
                }
                // Respawning in single-player makes the client sit on "Loading terrain..."
                // for a couple of real seconds while the integrated server hands the chunk
                // back. Those frames are a blurred panorama and a caption - useless, and
                // they landed square in the middle of the first cut. Freeze the counter (so
                // the SCRIPT waits too) and let the loading screen pass unrecorded: the clip
                // then cuts straight from the death screen to standing up, which is what a
                // promo clip wants anyway.
                if (mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen
                        || mc.level == null) {
                    break;
                }
                deathScript(mc, deathFrame);
                deathCapture(mc);
                deathFrame++;
            }
            // ================= the worn-pack HERO shoot (-Pwornhero -Pcurios) =================
            case WH_BOOT -> {
                if (ticks == 5) {
                    try {
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1920, 1080);
                        mc.options.guiScale().set(3);
                        // FOV 30 is the LONGEST lens vanilla has: the option is an
                        // OptionInstance.IntRange(30, 110) (checked in Options.java, not
                        // remembered), and anything below 30 is refused and falls back to the
                        // DEFAULT 70 - silently. A take shot at "26" came out wider than one
                        // shot at 34, which is the only way that failure ever announces itself.
                        // The other half of the framing is the backstop wall in heroStage.
                        mc.options.fov().set(30);
                        mc.resizeDisplay();
                        Packwork.LOGGER.info("[wornhero] window 1920x1080 @ fov 30 (the longest lens vanilla allows)");
                    } catch (Throwable t) {
                        Packwork.LOGGER.warn("[wornhero] resize failed: {}", t.toString());
                    }
                }
                if (ticks > 40 && mc.level == null && mc.screen != null) {
                    Packwork.LOGGER.info("[wornhero] creating throwaway world");
                    LevelSettings settings = new LevelSettings("packwork_autoshot", GameType.CREATIVE,
                            false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("packwork_autoshot", settings,
                            WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, mc.screen);
                    phase = Phase.WH_WAIT_LEVEL;
                    wait = 0;
                }
            }
            case WH_WAIT_LEVEL -> {
                if (mc.player != null && mc.getSingleplayerServer() != null && ++wait > 60) {
                    if (!net.neoforged.fml.ModList.get().isLoaded("curios")) {
                        Packwork.LOGGER.warn("[wornhero] Curios absent - nothing to wear; run with -Pcurios");
                        phase = Phase.DONE;
                    } else {
                        heroStage(mc);
                        phase = Phase.WH_STEP;
                    }
                    wait = 0;
                }
            }
            case WH_STEP -> {
                if (++wait > 25) {
                    if (heroShot >= HERO_SHOTS.length) {
                        phase = Phase.WH_SPIN_PREP;
                        wait = 0;
                    } else {
                        applyHeroShot(mc, HERO_SHOTS[heroShot]);
                        phase = Phase.WH_SHOOT;
                        wait = 0;
                    }
                }
            }
            case WH_SHOOT -> {
                if (++wait > 30) {
                    HeroShot shot = HERO_SHOTS[heroShot];
                    String wrong = heroCheck(mc, shot);
                    if (wrong != null) {
                        heroFailures++;
                        Packwork.LOGGER.error("[wornhero] {} NOT SHOT - the scene is wrong: {}",
                                shot.name(), wrong);
                    } else {
                        grab(mc, "hero_" + shot.name());
                        Packwork.LOGGER.info("[wornhero] {} -> {}", shot.name(), shot.expect());
                    }
                    heroShot++;
                    phase = Phase.WH_STEP;
                    wait = 0;
                }
            }
            case WH_SPIN_PREP -> {
                // The turntable clip wants a smaller frame and a rounder rate than the stills
                if (ticks % 1 == 0 && wait == 0) {
                    try {
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
                        mc.resizeDisplay();
                    } catch (Throwable t) {
                        Packwork.LOGGER.warn("[wornhero] spin resize failed: {}", t.toString());
                    }
                    applyHeroShot(mc, HERO_SHOTS[0]);   // Sculkhide, no armour
                }
                if (++wait > 30) { phase = Phase.WH_SPIN; spinFrame = 0; wait = 0; }
            }
            case WH_SPIN -> {
                if (spinFrame >= SPIN_FRAMES) {
                    phase = Phase.DONE;
                    if (heroFailures > 0) {
                        Packwork.LOGGER.error("[wornhero] done - {} of {} stills REFUSED",
                                heroFailures, HERO_SHOTS.length);
                    } else {
                        Packwork.LOGGER.info("[wornhero] done - {} stills, {} spin frames",
                                HERO_SHOTS.length, spinFrame);
                    }
                    break;
                }
                // 0 -> 360 over the clip: a full turntable that loops seamlessly
                heroBodyYaw = 360f * spinFrame / SPIN_FRAMES;
                spinCapture(mc);
                spinFrame++;
            }
            // ================= the sorting-GIF capture (-Pgifshot) =================
            case GIF_BOOT -> {
                if (ticks == 5) {
                    try {
                        // 1280x720 at GUI scale 2 is the whole point: every GUI texel is
                        // exactly 2x2 device pixels, so the encoder's 2x nearest downscale
                        // to 640x360 is LOSSLESS for the GUI. Any other pairing blurs it.
                        org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
                        mc.options.guiScale().set(2);
                        mc.resizeDisplay();
                        Packwork.LOGGER.info("[gifshot] window 1280x720 at GUI scale 2");
                    } catch (Throwable t) {
                        Packwork.LOGGER.warn("[gifshot] resize failed: {}", t.toString());
                    }
                }
                if (ticks > 40 && mc.level == null && mc.screen != null) {
                    Packwork.LOGGER.info("[gifshot] creating throwaway world");
                    LevelSettings settings = new LevelSettings("packwork_autoshot", GameType.CREATIVE,
                            false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("packwork_autoshot", settings,
                            WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, mc.screen);
                    phase = Phase.GIF_WAIT_LEVEL;
                    wait = 0;
                }
            }
            case GIF_WAIT_LEVEL -> {
                if (mc.player != null && mc.getSingleplayerServer() != null && ++wait > 60) {
                    if (!mc.options.hideGui) mc.options.hideGui = true;   // no HUD over the GUI
                    gifStage(mc);
                    phase = Phase.GIF_STAGE;
                    wait = 0;
                }
            }
            case GIF_STAGE -> {
                // let the open land and the first sort settle before frame 0
                if (mc.screen instanceof PackScreen && ++wait > 20) {
                    phase = Phase.GIF_ROLL;
                    gifFrame = 0;
                    wait = 0;
                }
            }
            case GIF_ROLL -> {
                if (gifFrame >= GIF_FRAMES) {
                    phase = Phase.DONE;
                    mc.options.hideGui = false;
                    Packwork.LOGGER.info("[gifshot] done - {} frames in {}", gifFrame, gifDir);
                    break;
                }
                gifScript(mc, gifFrame);
                gifFrame(mc);
                gifFrame++;
                if (gifFrame % 40 == 0) Packwork.LOGGER.info("[gifshot] {} frames", gifFrame);
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

    /** Park the real cursor in the bottom-left corner so no hover highlight or tooltip
     *  photobombs a promo frame. {@code glfwSetCursorPos} does NOT fire the move callback,
     *  so MouseHandler's cached xpos/ypos are set reflectively too - dev-only code on a
     *  dev runtime, where the mojmap field names are live. */
    private static void parkCursor(Minecraft mc) {
        double px = 4, py = mc.getWindow().getHeight() - 4;
        try {
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), px, py);
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

    /**
     * Put the real cursor on a GUI point so hover states show up in a screenshot. GLFW works
     * in window pixels and the GUI in scaled units, and MouseHandler caches its own position -
     * glfwSetCursorPos alone does NOT update it (the lesson behind parkCursor).
     */
    private static void hoverGui(Minecraft mc, int guiX, int guiY) {
        var w = mc.getWindow();
        double px = guiX * (double) w.getScreenWidth() / Math.max(1, w.getGuiScaledWidth());
        double py = guiY * (double) w.getScreenHeight() / Math.max(1, w.getGuiScaledHeight());
        try {
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(w.getWindow(), px, py);
        } catch (Throwable ignored) {
        }
        try {
            var xf = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");
            var yf = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
            xf.setAccessible(true);
            yf.setAccessible(true);
            xf.setDouble(mc.mouseHandler, px);
            yf.setDouble(mc.mouseHandler, py);
            Packwork.LOGGER.info("[autoshot] hovering gui ({},{}) -> window ({},{})",
                    guiX, guiY, (int) px, (int) py);
        } catch (Throwable t) {
            Packwork.LOGGER.warn("[autoshot] hover (reflective) failed: {}", t.toString());
        }
    }

    /** THE click helper: a genuine press + release at a dev-exposed GUI point. Every scripted
     *  click in the harness routes through here (skips politely if the target is missing). */
    private static void clickAt(Minecraft mc, java.util.function.Function<PackScreen, int[]> where, String what) {
        if (!(mc.screen instanceof PackScreen ps)) {
            Packwork.LOGGER.warn("[autoshot] pack screen not open for {}", what);
            return;
        }
        int[] c = where.apply(ps);
        if (c == null) {
            Packwork.LOGGER.warn("[autoshot] no target for {}", what);
            return;
        }
        ps.mouseClicked(c[0], c[1], 0);
        ps.mouseReleased(c[0], c[1], 0);
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
        if (!(mc.screen instanceof PackScreen ps) || mc.player == null || mc.gameMode == null) {
            Packwork.LOGGER.warn("[autoshot] pack screen not open for {}", what);
            return;
        }
        PackMenu menu = ps.getMenu();
        for (int i = minIndex; i < menu.slots.size(); i++) {
            net.minecraft.world.inventory.Slot s = menu.slots.get(i);
            if (s instanceof com.sappersquad.packwork.pack.PackViewSlot vs && vs.isActive()
                    && s.hasItem() == wantItem) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0,
                        net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
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

    /**
     * A Runed pack wearing the two 1.2.0 fittings, holding enough cobblestone and iron for
     * both of them to have something to do. Opened so the sockets are on the rail and the
     * new sprites can be judged at GUI size rather than only in the montage.
     */
    private static void setupFittings(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            var tier = com.sappersquad.packwork.pack.PackTier.RUNED;
            ItemStack pack = new ItemStack(ModItems.pack(tier).get());
            // the pack's own store rather than the capability: the capability spelling drifts
            // across the version line and this harness reads the same on all eight branches
            var h = new com.sappersquad.packwork.pack.PackInventory(pack, tier);
            h.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
            h.insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
            h.insertItem(1, new ItemStack(Items.IRON_INGOT, 64), false);
            h.insertItem(2, new ItemStack(Items.GOLD_NUGGET, 27), false);
            h.insertItem(3, new ItemStack(Items.BREAD, 12), false);

            var sockets = new com.sappersquad.packwork.pack.PackTrinketInventory(() -> pack, tier);
            sockets.insertItem(0, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.OVERFLOW_VALVE).get()), false);
            sockets.insertItem(1, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.COMPACTING_PRESS).get()), false);
            sockets.insertItem(2, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.COMPASS_ROSE).get()), false);

            sp.getInventory().items.set(0, pack);
            sp.getInventory().selected = 0;
            PackItem.openPack(sp, 0);
            Packwork.LOGGER.info("[autoshot] 1.2.0 fittings pack opened");
        });
    }

    /**
     * Drive the REAL key handler over the first cobblestone cell, so the shot proves the
     * gesture and the note the player actually gets - not a synthetic call past the screen.
     */
    private static void pressOverCobble(Minecraft mc, int mods, String what) {
        if (!(mc.screen instanceof PackScreen ps)) {
            Packwork.LOGGER.warn("[autoshot] pack screen not open for {}", what);
            return;
        }
        PackMenu menu = ps.getMenu();
        for (int i = 0; i < menu.slots.size(); i++) {
            var s = menu.slots.get(i);
            if (s instanceof com.sappersquad.packwork.pack.PackViewSlot && s.hasItem()
                    && s.getItem().is(Items.COBBLESTONE)) {
                ps.devHover(i);
                ps.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_O, 0, mods);
                Packwork.LOGGER.info("[autoshot] {} -> menu slot {}", what, i);
                return;
            }
        }
        Packwork.LOGGER.warn("[autoshot] no cobblestone cell found for {}", what);
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
    private static void placeBlocks(Minecraft mc, int rise) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            net.minecraft.server.level.ServerLevel lvl = sp.serverLevel();
            lvl.setDayTime(6000);
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
                        buf -> PackItem.writeBlockHost(buf, pos, be.getTier()));
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
            sp.getInventory().items.set(0, pack);
            sp.getInventory().selected = 0;
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
            sp.getInventory().items.set(0, new ItemStack(ModItems.pack(com.sappersquad.packwork.pack.PackTier.SCULKHIDE).get()));
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

    // ---- the worn-render shoot: is the pack on your back worth looking at? ----

    /**
     * One framed check of {@link WornPackLayer}. {@code chest} is a supplier, not a stack -
     * this table is a static field on an {@code @EventBusSubscriber} class, and building an
     * ItemStack while the registries are still binding is a crash on the 26.x branches.
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
    private static net.minecraft.core.BlockPos wornPad = null;

    /** A floating stone-brick pad in open sky, so the backdrop never depends on the seed. */
    private static void wornStage(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            net.minecraft.server.level.ServerLevel lvl = sp.serverLevel();
            lvl.setDayTime(6000);
            net.minecraft.core.BlockPos base = sp.blockPosition().above(48);
            var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            var floor = net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
            for (int dx = -5; dx <= 5; dx++)
                for (int dz = -6; dz <= 5; dz++) {
                    for (int dy = 0; dy <= 6; dy++) lvl.setBlock(base.offset(dx, dy, dz), air, 2);
                    lvl.setBlock(base.offset(dx, -1, dz), floor, 2);
                }
            wornPad = base;
            sp.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0f, 14f);
            Packwork.LOGGER.info("[wornshot] sky pad staged at {}", base);
        });
    }

    private static void applyWornShot(Minecraft mc, WornShot shot) {
        mc.options.hideGui = true;
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
            com.sappersquad.packwork.compat.curios.CuriosCompat.devEquip(sp,
                    new ItemStack(ModItems.pack(shot.tier()).get()));
            // Strip FIRST, dress second: Inventory.clearContent() empties the armor and
            // offhand rows too, so clearing after equipping quietly wiped the chestplate and
            // the elytra - two shots that looked fine and proved nothing.
            Packwork.LOGGER.info("[wornshot] hands were main={} off={}",
                    sp.getMainHandItem(), sp.getOffhandItem());
            sp.getInventory().clearContent();
            sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, shot.chest().get());
            // Re-anchor every shot: an elytra or a stray nudge drifts the player off the pad,
            // and one drifted frame ruins the shot it lands on.
            if (wornPad != null) {
                sp.connection.teleport(wornPad.getX() + 0.5, wornPad.getY(),
                        wornPad.getZ() + 0.5, 0f, 14f);
            }
        });
    }

    private static void withMenu(Minecraft mc, java.util.function.Consumer<PackMenu> action) {
        if (mc.player != null && mc.player.containerMenu instanceof PackMenu menu) {
            action.accept(menu);
        }
    }

    // =====================================================================
    //  the place-at-death micro-clip (-Pdeathclip)
    // =====================================================================
    //
    //  "Your stuff doesn't scatter" in one beat: you take a fatal hit, the screen goes red,
    //  and when you come back the pack is STANDING where you fell with everything in it.
    //  It is the only visual 1.1.0's death-handling config has.

    private static final int DEATH_FRAMES = 100;    // 5s at 20fps
    private static int deathFrame = 0;
    private static java.io.File deathDir = null;
    private static net.minecraft.core.BlockPos deathSpot = null;

    /**
     * A camp, a survival player carrying a loaded pack, and the config flipped to PLACE.
     *
     * <p>{@code setRemote} rather than {@code setLocalForTesting}: on an integrated server the
     * client receives the config-sync payload on login and {@code get()} then prefers the
     * REMOTE overlay, so a local-only override is read by nobody. Setting the overlay covers
     * both sides at once - and the death handler runs on the server in this same JVM.
     */
    private static void deathStage(Minecraft mc) {
        var v = com.sappersquad.packwork.config.PackworkConfig.defaults();
        var place = new com.sappersquad.packwork.config.PackworkConfig.Values(
                v.slots(), v.stacksPerSlot(), v.fluidMb(), v.xpPoints(), v.energyFe(), v.vaporMb(),
                v.trinketEnabled(), com.sappersquad.packwork.config.PackworkConfig.DeathHandling.PLACE,
                v.magnetRange(), v.magnetEveryTicks(), v.packFirstDefault(), v.neverAutoEat(),
                v.valveDefaultKeepStacks(), v.pressKeepLoose(), v.pressIncludes2x2());
        com.sappersquad.packwork.config.PackworkConfig.setRemote(place);

        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            net.minecraft.server.level.ServerLevel lvl = sp.serverLevel();
            lvl.setDayTime(1200);
            net.minecraft.core.BlockPos base = sp.blockPosition().above(48);
            var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            var grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
            for (int dx = -8; dx <= 8; dx++)
                for (int dz = -8; dz <= 8; dz++) {
                    for (int dy = 0; dy <= 8; dy++) lvl.setBlock(base.offset(dx, dy, dz), air, 2);
                    lvl.setBlock(base.offset(dx, -1, dz), grass, 2);
                }
            put(lvl, base.offset(-3, 0, 5), net.minecraft.world.level.block.Blocks.CAMPFIRE);
            littleTree(lvl, base.offset(-5, 0, 6));
            littleTree(lvl, base.offset(6, 0, 7));
            for (int dx = -6; dx <= 6; dx += 3) {
                put(lvl, base.offset(dx, 0, 4), net.minecraft.world.level.block.Blocks.SHORT_GRASS);
            }
            // NO camera backstop here, unlike the hero shoot: this clip has to stand the
            // player BACK from the death spot afterwards, and a wall four blocks behind is
            // exactly where they would land. The first take put the camera inside it.
            deathSpot = base;

            sp.setGameMode(GameType.SURVIVAL);
            sp.getInventory().clearContent();
            var tier = com.sappersquad.packwork.pack.PackTier.RUNED;
            ItemStack pack = new ItemStack(ModItems.pack(tier).get());
            var store = new com.sappersquad.packwork.pack.PackInventory(pack, tier);
            store.insertItem(0, new ItemStack(Items.DIAMOND, 34), false);
            store.insertItem(1, new ItemStack(Items.IRON_INGOT, 64), false);
            store.insertItem(2, new ItemStack(Items.BREAD, 21), false);
            sp.getInventory().items.set(0, pack);
            sp.getInventory().selected = 0;

            // No setRespawnPosition: a fresh world has no bed, the call is refused with a
            // "no home bed or charged respawn anchor" line IN THE CHAT (which then sits in
            // frame), and the respawn lands at world spawn - underground, on the first take.
            // The script teleports the player back instead, several ticks running, because a
            // single teleport races the respawn packet that follows it.
            sp.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0f, 8f);
            Packwork.LOGGER.info("[deathclip] camp staged at {}, death.handling = PLACE", base);
        });
    }

    private static void deathScript(Minecraft mc, int t) {
        if (t == 2) {
            mc.options.hideGui = false;             // the HUD sells that this is real survival
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
        // 0.0-1.0s: standing at the camp, pack on the hotbar
        if (t == 20) {                              // the fatal hit
            var server = mc.getSingleplayerServer();
            if (server != null) server.execute(() -> {
                if (server.getPlayerList().getPlayers().isEmpty()) return;
                ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                sp.hurt(sp.damageSources().fall(), 100f);   // 1.21.1: plain hurt(); hurtServer arrives later
                Packwork.LOGGER.info("[deathclip] fatal hit; dead={}", sp.isDeadOrDying());
            });
        }
        // 1.0-1.6s: the death screen
        if (t == 32 && mc.player != null) {
            mc.player.respawn();
            Packwork.LOGGER.info("[deathclip] respawn requested");
        }
        if (t == 40) {
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            // wipe the respawn chatter ("you have no home bed...") before the good frames
            mc.gui.getChat().clearMessages(true);
        }
        // Three teleports, not one: the client is still settling the respawn and a lone
        // teleport gets overwritten by the position that follows it.
        if (t == 42 || t == 46 || t == 50) {
            var server = mc.getSingleplayerServer();
            if (server != null) server.execute(() -> {
                if (server.getPlayerList().getPlayers().isEmpty() || deathSpot == null) return;
                ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                // Stand OFF the pack's line, not on it. Straight behind, the player's own body
                // hides the thing the clip is about - which is exactly what the last take
                // produced: an empty meadow with the pack directly behind the avatar.
                // Facing +z, +x is frame-LEFT, so standing 2.5 east puts the pack right of centre.
                sp.connection.teleport(deathSpot.getX() + 2.5, deathSpot.getY(),
                        deathSpot.getZ() - 4.0, 0f, 10f);
                if (t == 50) {
                    Packwork.LOGGER.info("[deathclip] block at the death spot: {}; walked back to {}",
                            sp.serverLevel().getBlockState(deathSpot), sp.blockPosition());
                }
            });
        }
        // 2.5-5.0s: back on your feet, the pack standing exactly where you fell
    }

    private static void deathCapture(Minecraft mc) {
        var target = mc.getMainRenderTarget();
        if (target.width < 64 || target.height < 64) return;
        if (deathDir == null) {
            deathDir = new java.io.File(new java.io.File(mc.gameDirectory, "screenshots"), "deathclip");
            if (!deathDir.exists() && !deathDir.mkdirs()) {
                Packwork.LOGGER.error("[deathclip] could not create {}", deathDir);
                return;
            }
            spinDir = deathDir;   // shared for the done-message
        }
        try (var img = Screenshot.takeScreenshot(target)) {
            img.writeToFile(new java.io.File(deathDir,
                    String.format(java.util.Locale.ROOT, "frame_%04d.png", deathFrame)));
        } catch (Exception e) {
            Packwork.LOGGER.error("[deathclip] frame {} failed", deathFrame, e);
        }
    }

    // =====================================================================
    //  the worn-pack HERO shoot (-Pwornhero -Pcurios)
    // =====================================================================
    //
    //  The proof shoot (-Pwornshot) answers "does it render". This one answers "is it a
    //  store frame", and the difference is entirely composition: closer lens, a camp to
    //  stand in instead of a bare slab, and a THREE-QUARTER angle.
    //
    //  That angle needed a trick, because vanilla's third-person camera always sits directly
    //  behind you - you cannot orbit your own back. What you CAN do is turn the avatar under
    //  a fixed camera: for a Player the camera reads `yRot`, while the renderer reads
    //  `yBodyRot` and `yHeadRot`. Overriding the latter two every client tick spins the body
    //  in place and leaves the camera where it is. Same trick drives the turntable clip.

    private record HeroShot(String name, com.sappersquad.packwork.pack.PackTier tier,
                            java.util.function.Supplier<ItemStack> chest, float bodyYaw,
                            String expect) {}

    private static final HeroShot[] HERO_SHOTS = {
            new HeroShot("worn_sculkhide", com.sappersquad.packwork.pack.PackTier.SCULKHIDE,
                    () -> ItemStack.EMPTY, 34f,
                    "Sculkhide three-quarter: echo veins catching the light, camp behind"),
            new HeroShot("worn_canvas", com.sappersquad.packwork.pack.PackTier.CANVAS,
                    () -> ItemStack.EMPTY, 34f,
                    "Canvas three-quarter: weave and twine, the other end of the ladder"),
            new HeroShot("worn_over_armor", com.sappersquad.packwork.pack.PackTier.RUNED,
                    () -> new ItemStack(Items.NETHERITE_CHESTPLATE), -30f,
                    "Runed over netherite plate, turned the other way - it rides proud of armour"),
    };

    private static int heroShot = 0;
    private static int heroFailures = 0;
    private static net.minecraft.core.BlockPos heroPad = null;
    /** Live override for the rendered body/head yaw; NaN leaves the avatar alone. */
    private static float heroBodyYaw = Float.NaN;
    private static final int SPIN_FRAMES = 100;   // 5s at 20fps
    private static int spinFrame = 0;
    private static java.io.File spinDir = null;

    /**
     * A camp on a sky pad, not a slab in the sky. Grass underfoot, a lit campfire and a
     * barrel off to one side, a low bank of oak leaves behind - enough that the frame has
     * depth and a warm light source, without anything crossing the pack's silhouette.
     */
    private static void heroStage(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            net.minecraft.server.level.ServerLevel lvl = sp.serverLevel();
            lvl.setDayTime(1200);                       // low morning sun: long light, warm
            net.minecraft.core.BlockPos base = sp.blockPosition().above(48);
            var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            var grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
            for (int dx = -8; dx <= 8; dx++)
                for (int dz = -8; dz <= 8; dz++) {
                    for (int dy = 0; dy <= 8; dy++) lvl.setBlock(base.offset(dx, dy, dz), air, 2);
                    lvl.setBlock(base.offset(dx, -1, dz), grass, 2);
                }
            // The camp goes at +z - the player faces +z (yaw 0) and the third-person camera
            // sits at -z, so +z is what ends up BEHIND the subject in frame. The first cut put
            // it at -z, "behind the camera line", which is another way of saying invisible.
            // Everything is pushed wide so nothing crosses the pack's own outline.
            put(lvl, base.offset(4, 0, 7), net.minecraft.world.level.block.Blocks.CAMPFIRE);
            put(lvl, base.offset(5, 0, 6), net.minecraft.world.level.block.Blocks.BARREL);
            littleTree(lvl, base.offset(6, 0, 8));
            littleTree(lvl, base.offset(-6, 0, 7));
            for (int dx = -7; dx <= 7; dx += 2) {
                if (Math.abs(dx) < 3) continue;                       // keep the centre clear
                put(lvl, base.offset(dx, 0, 4), net.minecraft.world.level.block.Blocks.SHORT_GRASS);
                put(lvl, base.offset(dx + 1, 0, 6), net.minecraft.world.level.block.Blocks.POPPY);
            }
            // The camera BACKSTOP, and it is the real framing control. Vanilla's third-person
            // camera wants to sit 4 blocks back but collision-checks its way in, so a wall
            // four blocks behind the player pulls it to about 3.2 and the subject grows by
            // half again. At three blocks it came in too far and cropped the pack. It sits behind the camera, so it is never in shot. Without
            // it the longest legal lens still leaves a store frame two-thirds empty sky.
            for (int dx = -4; dx <= 4; dx++)
                for (int dy = 0; dy <= 4; dy++) {
                    put(lvl, base.offset(dx, dy, -4), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
                }
            heroPad = base;
            // pitch 10: the camera rises just above the shoulders and looks slightly DOWN -
            // the lineup hero's vantage. At the first cut's -4 it looked up instead, and two
            // thirds of a 1920x1080 frame came out empty sky.
            sp.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0f, 14f);
            Packwork.LOGGER.info("[wornhero] camp staged at {}", base);
        });
    }

    private static void put(net.minecraft.server.level.ServerLevel lvl,
                            net.minecraft.core.BlockPos bp,
                            net.minecraft.world.level.block.Block block) {
        lvl.setBlock(bp, block.defaultBlockState(), 2);
    }

    /** A two-block trunk with a leaf cap: scenery that reads as a tree, not floating foliage. */
    private static void littleTree(net.minecraft.server.level.ServerLevel lvl,
                                   net.minecraft.core.BlockPos foot) {
        var log = net.minecraft.world.level.block.Blocks.OAK_LOG;
        var leaves = net.minecraft.world.level.block.Blocks.OAK_LEAVES;
        put(lvl, foot, log);
        put(lvl, foot.above(), log);
        put(lvl, foot.above(2), leaves);
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            put(lvl, foot.above(2).relative(d), leaves);
            put(lvl, foot.above(1).relative(d), leaves);
        }
        put(lvl, foot.above(3), leaves);
    }

    private static void applyHeroShot(Minecraft mc, HeroShot shot) {
        if (!mc.options.hideGui) mc.options.hideGui = true;
        mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        com.sappersquad.packwork.config.PackworkConfig.setShowWornPack(true);
        heroBodyYaw = shot.bodyYaw();
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            // strip first, dress second (and on Fabric the strip reaches the trinket slot too)
            sp.getInventory().clearContent();
            sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, shot.chest().get());
            com.sappersquad.packwork.compat.curios.CuriosCompat.devEquip(sp,
                    new ItemStack(ModItems.pack(shot.tier()).get()));
            if (heroPad != null) {
                sp.connection.teleport(heroPad.getX() + 0.5, heroPad.getY(),
                        heroPad.getZ() + 0.5, 0f, 14f);
            }
        });
    }

    /** Same contract as the proof shoot's check: never write a frame the scene doesn't back. */
    private static String heroCheck(Minecraft mc, HeroShot shot) {
        if (mc.player == null) return "no client player";
        ItemStack worn = com.sappersquad.packwork.compat.curios.CuriosCompat.wornPack(mc.player);
        if (!(worn.getItem() instanceof PackItem)) {
            return "the back slot holds " + worn + ", not a pack";
        }
        if (PackItem.tierOf(worn) != shot.tier()) {
            return "the back slot holds a " + PackItem.tierOf(worn) + ", not the " + shot.tier();
        }
        ItemStack want = shot.chest().get();
        ItemStack have = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!ItemStack.isSameItem(want, have)) {
            return "the chest slot holds " + have + ", but this shot needs " + want;
        }
        if (heroPad == null) return "the pad was never staged";
        double dx = mc.player.getX() - (heroPad.getX() + 0.5);
        double dz = mc.player.getZ() - (heroPad.getZ() + 0.5);
        if (dx * dx + dz * dz > 4.0) return "the player has drifted off the camp";
        return null;
    }

    /** One turntable frame, written synchronously (same reasoning as the GIF capture). */
    private static void spinCapture(Minecraft mc) {
        var target = mc.getMainRenderTarget();
        if (target.width < 64 || target.height < 64) {
            Packwork.LOGGER.error("[wornhero] render target is {}x{} - refusing a blank frame",
                    target.width, target.height);
            return;
        }
        if (spinDir == null) {
            spinDir = new java.io.File(new java.io.File(mc.gameDirectory, "screenshots"), "wornspin");
            if (!spinDir.exists() && !spinDir.mkdirs()) {
                Packwork.LOGGER.error("[wornhero] could not create {}", spinDir);
                return;
            }
        }
        try (var img = Screenshot.takeScreenshot(target)) {
            img.writeToFile(new java.io.File(spinDir,
                    String.format(java.util.Locale.ROOT, "frame_%04d.png", spinFrame)));
        } catch (Exception e) {
            Packwork.LOGGER.error("[wornhero] spin frame {} failed", spinFrame, e);
        }
    }

    /**
     * Hold the avatar at the shoot's angle. Runs at the END of every client tick, after
     * vanilla has had its say - {@code tickHeadTurn} drags the body back toward the head
     * yaw, so this has to be re-asserted rather than set once. Both the "O" (previous) and
     * current fields are written, or the renderer interpolates between the old angle and
     * the new one and the avatar shivers.
     */
    private static void holdHeroYaw(Minecraft mc) {
        if (Float.isNaN(heroBodyYaw) || mc.player == null) return;
        mc.player.yBodyRot = heroBodyYaw;
        mc.player.yBodyRotO = heroBodyYaw;
        mc.player.yHeadRot = heroBodyYaw;
        mc.player.yHeadRotO = heroBodyYaw;
    }

    // =====================================================================
    //  the sorting-GIF capture (-Pgifshot)
    // =====================================================================
    //
    //  One framebuffer PNG per client tick, driven by a tick-counted script, into
    //  run/client/screenshots/gifshot/. Because the script is counted in TICKS and not in
    //  wall-clock, the readback stalling the render thread only makes the capture take
    //  longer - it never changes the timing of the finished GIF.
    //
    //  Deliberately NO synthetic mouse cursor. Minecraft never draws one (the OS does), so
    //  a framebuffer capture has none, and a hand-drawn stand-in would be a UI element the
    //  mod does not have. Vanilla's slot HIGHLIGHT is in the framebuffer, so the script
    //  moves the hover instead: the viewer reads a pointer that isn't there.

    /** 20 tps in, so one frame per tick is a 20fps capture; the encoder drops to 15. */
    private static final int GIF_FRAMES = 400;          // 20 seconds
    private static int gifFrame = 0;
    private static java.io.File gifDir = null;
    private static int gifClickedRow = 0;

    /**
     * The messy inventory the GIF opens on: interleaved junk, duplicates scattered, in no
     * order at all - the viewer's own inventory, basically. Deliberately spanning every
     * auto-tab so the rail lights up across the board as it fills.
     */
    private static ItemStack[] gifMess() {
        // NO duplicates: two stacks of the same thing merge on the way in, and a grid that
        // fills more slowly than the pockets empty makes the pack look like it is eating
        // things. Twenty-seven distinct items fill three full rows.
        return new ItemStack[]{
            new ItemStack(Items.COBBLESTONE, 45), new ItemStack(Items.BREAD, 7),
            new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.OAK_SAPLING, 12),
            new ItemStack(Items.RAW_GOLD, 9), new ItemStack(Items.TORCH, 33),
            new ItemStack(Items.WHEAT_SEEDS, 18), new ItemStack(Items.ANDESITE, 61),
            new ItemStack(Items.IRON_SWORD), new ItemStack(Items.COOKED_BEEF, 5),
            new ItemStack(Items.NETHER_WART, 14), new ItemStack(Items.RAW_IRON, 22),
            new ItemStack(Items.OAK_PLANKS, 40), new ItemStack(Items.ARROW, 27),
            new ItemStack(Items.APPLE, 3), new ItemStack(Items.DIRT, 52),
            new ItemStack(Items.BLAZE_POWDER, 6), new ItemStack(Items.SHEARS),
            new ItemStack(Items.DIAMOND, 4), new ItemStack(Items.POPPY, 11),
            new ItemStack(Items.GRAVEL, 23), new ItemStack(Items.GLASS_BOTTLE, 5),
            new ItemStack(Items.BONE, 8), new ItemStack(Items.STICK, 31),
            new ItemStack(Items.RAW_COPPER, 17), new ItemStack(Items.MUSHROOM_STEW),
            new ItemStack(Items.BRICKS, 28),
        };
    }

    /**
     * Hand the player a Sculkhide pack plus the mess, on a stone-brick pad in open sky, and
     * open the pack over it. The pad is the gallery shoot's trick: the backdrop is then pure
     * daylight sky on every seed, instead of whatever village or ravine the world rolled -
     * and behind a GUI, a busy backdrop is just noise the palette has to pay for.
     */
    private static void gifStage(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            if (server.getPlayerList().getPlayers().isEmpty()) return;
            ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
            var tier = com.sappersquad.packwork.pack.PackTier.SCULKHIDE;
            net.minecraft.server.level.ServerLevel lvl = sp.serverLevel();
            lvl.setDayTime(6000);                       // noon: the brightest, flattest sky
            net.minecraft.core.BlockPos base = sp.blockPosition().above(48);
            var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            var floor = net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState();
            for (int dx = -5; dx <= 5; dx++)
                for (int dz = -5; dz <= 6; dz++) {
                    for (int dy = 0; dy <= 6; dy++) lvl.setBlock(base.offset(dx, dy, dz), air, 2);
                    lvl.setBlock(base.offset(dx, -1, dz), floor, 2);
                }
            // The pack the GIF closes on: standing in the world three blocks ahead, so the
            // last beat is the object itself and the loop back to the open GUI reads as
            // re-opening it. Without this the final second was a bare stone pad.
            placePackBlock(lvl, base.offset(0, 0, 3), tier, false);
            sp.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0f, 12f);

            sp.getInventory().clearContent();
            ItemStack pack = new ItemStack(ModItems.pack(tier).get());
            sp.getInventory().items.set(0, pack);
            sp.getInventory().selected = 0;
            ItemStack[] mess = gifMess();
            // fill the three inventory rows (slots 9..35), leaving the hotbar for the pack
            for (int i = 0; i < mess.length && i < 27; i++) {
                sp.getInventory().items.set(9 + i, mess[i]);
            }
            PackItem.openPack(sp, 0);
            Packwork.LOGGER.info("[gifshot] messy inventory staged on the sky pad, pack open");
        });
    }

    /**
     * One tick of the script. Times are in ticks at 20 tps; the shot table in the outreach
     * storyboard is in seconds, so every boundary here is that number x20.
     */
    private static void gifScript(Minecraft mc, int t) {
        if (!(mc.screen instanceof PackScreen ps)) return;
        PackMenu menu = ps.getMenu();

        // The dump runs FLATTENED. Filed into compartments, most of what you shift-click
        // lands on a tab you are not looking at - the first cut showed items vanish from the
        // pockets and never appear, which reads as "it ate them", the exact opposite of the
        // point. Flat, they pile up in front of you; shot 3 then turns it off and shows the
        // compartments were being filled the whole time.
        if (t == 4) PackClientActions.toggleFlatten(menu);

        // shot 1 (0.0-3.0s): the mess, held still. Nothing moves for the first beat.
        if (t < 60) return;

        // shot 2 (3.0-8.0s): the dump. One shift-click every 3 ticks empties all 27 pockets.
        if (t < 160) {
            if ((t - 60) % 3 == 0) gifQuickMoveNext(mc, menu);
            return;
        }

        // shot 3 (8.0-13.0s): the proof. Un-flatten, then Food, then Ores & Valuables.
        // The cursor steps OFF the rail a beat after each click: a tab tooltip is two lines
        // wide and lies straight across the compartment the shot exists to show.
        if (t == 162) PackClientActions.toggleFlatten(menu);
        if (t == 170) gifClickTab(mc, ps, menu, "auto:food");
        if (t == 178) gifPointBlank(mc, ps);
        if (t == 212) gifClickTab(mc, ps, menu, "auto:ores");
        if (t == 220) gifPointBlank(mc, ps);
        if (t < 255) return;

        // shot 4 (13.0-17.0s): the obedience. Carry bread OUT of Food and set it down in
        // Ores & Valuables - dropping into a compartment it would never sort to is the pin
        // gesture, and the stitched note says so.
        if (t == 256) gifClickTab(mc, ps, menu, "auto:food");
        if (t == 266) gifPickUpFrom(mc, ps, menu, Items.BREAD, true);
        if (t == 278) gifClickTab(mc, ps, menu, "auto:ores");
        if (t == 290) gifDropIntoFirstFreeCell(mc, ps, menu);
        if (t == 298) gifPointBlank(mc, ps);
        if (t < 340) return;

        // shot 5 (17.0-20.0s): Tidy Up, then close on the pack standing in the world.
        if (t == 342) PackClientActions.tidyUp(menu);
        if (t == 378) mc.setScreen(null);
    }

    /** Park the REAL cursor on a tab, then select it - the highlight is the GIF's pointer. */
    private static void gifClickTab(Minecraft mc, PackScreen ps, PackMenu menu, String tabId) {
        int[] c = ps.devTabCenter(tabId);
        if (c != null) hoverGui(mc, c[0], c[1]);
        PackClientActions.selectTab(menu, tabId);
    }

    /** Park the cursor on an EMPTY grid cell: the highlight stays, the tooltip goes. */
    private static void gifPointBlank(Minecraft mc, PackScreen ps) {
        PackMenu menu = ps.getMenu();
        for (int i = menu.slots.size() - 1; i >= 0; i--) {
            net.minecraft.world.inventory.Slot s = menu.slots.get(i);
            if (s instanceof com.sappersquad.packwork.pack.PackViewSlot && s.isActive() && !s.hasItem()) {
                gifPointAt(mc, i);
                return;
            }
        }
    }

    /** Shift-click the next non-empty player-inventory slot into the pack. */
    private static void gifQuickMoveNext(Minecraft mc, PackMenu menu) {
        if (mc.player == null || mc.gameMode == null) return;
        for (int i = gifClickedRow; i < menu.slots.size(); i++) {
            net.minecraft.world.inventory.Slot s = menu.slots.get(i);
            if (!(s.container instanceof net.minecraft.world.entity.player.Inventory)) continue;
            if (!s.hasItem() || s.getItem().getItem() instanceof PackItem) continue;
            gifPointAt(mc, i);
            mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0,
                    net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
            gifClickedRow = i + 1;
            return;
        }
    }

    /** Pick up the first cell holding this item (so the next click can place it elsewhere). */
    private static void gifPickUpFrom(Minecraft mc, PackScreen ps, PackMenu menu,
                                      net.minecraft.world.item.Item want, boolean fromPack) {
        if (mc.player == null || mc.gameMode == null) return;
        for (int i = 0; i < menu.slots.size(); i++) {
            net.minecraft.world.inventory.Slot s = menu.slots.get(i);
            boolean isPackCell = s instanceof com.sappersquad.packwork.pack.PackViewSlot;
            if (isPackCell != fromPack || !s.hasItem() || !s.getItem().is(want)) continue;
            gifPointAt(mc, i);
            mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0,
                    net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
            return;
        }
    }

    /** Drop whatever is on the cursor into the first empty cell of the open compartment. */
    private static void gifDropIntoFirstFreeCell(Minecraft mc, PackScreen ps, PackMenu menu) {
        if (mc.player == null || mc.gameMode == null) return;
        for (int i = 0; i < menu.slots.size(); i++) {
            net.minecraft.world.inventory.Slot s = menu.slots.get(i);
            if (!(s instanceof com.sappersquad.packwork.pack.PackViewSlot) || !s.isActive()) continue;
            if (s.hasItem()) continue;
            gifPointAt(mc, i);
            mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0,
                    net.minecraft.world.inventory.ClickType.PICKUP, mc.player);
            return;
        }
    }

    /**
     * Move the REAL cursor onto a menu slot. {@code devHover} is no use here - the screen
     * recomputes the hovered slot from the mouse every frame, so a forced value is gone by
     * the time the frame is captured. Moving the actual pointer is what makes the highlight
     * follow the clicks, and the highlight is all the cursor this GIF gets.
     */
    private static void gifPointAt(Minecraft mc, int menuIndex) {
        if (!(mc.screen instanceof PackScreen ps)) return;
        int[] c = ps.devSlotCenter(menuIndex);
        if (c != null) hoverGui(mc, c[0], c[1]);
    }

    /**
     * Write one frame SYNCHRONOUSLY. {@code Screenshot.grab} hands the encode to the IO pool,
     * which is right for a handful of shots and wrong for four hundred: the render thread
     * would run ahead and queue hundreds of ~3.7MB NativeImages. Blocking here keeps memory
     * flat and costs only wall-clock, which the tick-counted script does not care about.
     */
    private static void gifFrame(Minecraft mc) {
        var target = mc.getMainRenderTarget();
        if (target.width < 64 || target.height < 64) {
            Packwork.LOGGER.error("[gifshot] render target is {}x{} - refusing to write a blank "
                    + "frame; is the dev window minimised?", target.width, target.height);
            return;
        }
        if (gifDir == null) {
            gifDir = new java.io.File(new java.io.File(mc.gameDirectory, "screenshots"), "gifshot");
            if (!gifDir.exists() && !gifDir.mkdirs()) {
                Packwork.LOGGER.error("[gifshot] could not create {}", gifDir);
                return;
            }
        }
        try (var img = Screenshot.takeScreenshot(target)) {
            img.writeToFile(new java.io.File(gifDir,
                    String.format(java.util.Locale.ROOT, "frame_%04d.png", gifFrame)));
        } catch (Exception e) {
            Packwork.LOGGER.error("[gifshot] frame {} failed", gifFrame, e);
        }
    }

    private static void grab(Minecraft mc, String name) {
        var target = mc.getMainRenderTarget();
        // A minimised or zero-sized window writes a 70-byte PNG that looks like a success in
        // the log and is worthless as evidence. Say so loudly instead.
        if (target.width < 64 || target.height < 64) {
            Packwork.LOGGER.error("[autoshot] render target is {}x{} - {} would be a blank file; "
                    + "is the dev window minimised?", target.width, target.height, name);
            return;
        }
        Screenshot.grab(mc.gameDirectory, name + ".png", target,
                msg -> Packwork.LOGGER.info("[autoshot] {}", msg.getString()));
        Packwork.LOGGER.info("[autoshot] grabbed {} at {}x{}", name, target.width, target.height);
    }

    private DevAutoShot() {}
}
