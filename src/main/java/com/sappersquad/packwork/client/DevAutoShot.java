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
        SHOOT_1, W1, SHOOT_2, W2, SHOOT_3, W3, SHOOT_4, W4, SHOOT_5,
        OPEN_BOOK, WB, SHOOT_BOOK, WB2, SHOOT_BOOK2,
        PLACE, WPLACE, SHOOT_WORLD, OPEN_BLOCK, WBLOCK, SHOOT_BLOCK, DONE
    }

    private static Phase phase = Phase.BOOT;
    private static int ticks = 0;
    private static int wait = 0;
    private static String customTabId = "custom:0";
    private static net.minecraft.core.BlockPos placedMiddle = null;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED || phase == Phase.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;

        switch (phase) {
            case BOOT -> {
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
                    phase = Phase.SHOOT_1;
                    wait = 0;
                }
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
                grab(mc, "packwork_placed_world"); // three tier-tinted packs on the ground
                openBlock(mc);
                phase = Phase.WBLOCK; wait = 0;
            }
            case WBLOCK -> { if (++wait > 15) phase = Phase.SHOOT_BLOCK; } // menu opens + syncs
            case SHOOT_BLOCK -> {
                grab(mc, "packwork_placed_gui"); // the SAME organizer, opened from the block
                phase = Phase.DONE;
                Packwork.LOGGER.info("[autoshot] done - screenshots written");
            }
            default -> {}
        }
    }

    private static void setupAndOpen(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayers().isEmpty()
                    ? null : server.getPlayerList().getPlayers().get(0);
            if (sp == null) return;

            // a Reinforced pack (3 trinket sockets) so the right rail is on show
            ItemStack pack = new ItemStack(ModItems.pack(com.sappersquad.packwork.pack.PackTier.REINFORCED).get());
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
            };
            for (int i = 0; i < spread.length; i++) h.insertItem(i, spread[i], false);

            // fit two trinkets so the right rail shows a filled socket + a slot for the Compass Rose
            var sockets = new com.sappersquad.packwork.pack.PackTrinketInventory(
                    () -> pack, com.sappersquad.packwork.pack.PackTier.REINFORCED);
            sockets.insertItem(0, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.WATERSKIN).get()), false);
            sockets.insertItem(1, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.SOUL_VIAL).get()), false);
            sockets.insertItem(2, new ItemStack(ModItems.trinket(
                    com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL).get()), false);
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
            PackItem.openPack(sp, 0);
            Packwork.LOGGER.info("[autoshot] pack filled and opened");
        });
    }

    /** Set three tier-tinted pack blocks on a stone-brick pad and stand the player back to view them. */
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
            for (int dx = -2; dx <= 3; dx++)
                for (int dz = -3; dz <= 6; dz++) {
                    for (int dy = 0; dy <= 4; dy++) lvl.setBlock(base.offset(dx, dy, dz), air, 2);
                    lvl.setBlock(base.offset(dx, -1, dz), floor, 2);
                }
            com.sappersquad.packwork.pack.PackTier[] tiers = {
                    com.sappersquad.packwork.pack.PackTier.LEATHER,
                    com.sappersquad.packwork.pack.PackTier.REINFORCED,
                    com.sappersquad.packwork.pack.PackTier.RUNED };
            for (int i = 0; i < 3; i++) {
                net.minecraft.core.BlockPos bp = base.offset(i, 0, 4);
                lvl.setBlock(bp, com.sappersquad.packwork.reg.ModBlocks.PACK.get().defaultBlockState()
                        .setValue(com.sappersquad.packwork.block.PackContainerBlock.FACING,
                                net.minecraft.core.Direction.NORTH), 3);
                if (lvl.getBlockEntity(bp) instanceof com.sappersquad.packwork.block.PackContainerBlockEntity be) {
                    ItemStack pk = new ItemStack(ModItems.pack(tiers[i]).get());
                    if (i == 1) { // give the middle pack a full loadout so its opened GUI shows content
                        var sockets = new com.sappersquad.packwork.pack.PackTrinketInventory(() -> pk, tiers[i]);
                        sockets.insertItem(0, new ItemStack(ModItems.trinket(
                                com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL).get()), false);
                        pk.set(com.sappersquad.packwork.reg.ModComponents.PACK_ENERGY.get(),
                                com.sappersquad.packwork.pack.PackEnergyStorage.capacityFor(pk) / 2);
                        var st = new com.sappersquad.packwork.pack.PackInventory(pk, tiers[i]);
                        st.insertItem(0, new ItemStack(Items.DIAMOND, 20), false);
                        st.insertItem(1, new ItemStack(Items.IRON_INGOT, 40), false);
                        st.insertItem(2, new ItemStack(Items.BREAD, 32), false);
                        placedMiddle = bp;
                    }
                    be.setPackStack(pk);
                }
            }
            // stand back on the pad and look at the row
            sp.connection.teleport(base.getX() + 1.5, base.getY(), base.getZ() - 2.5, 0f, 12f);
            Packwork.LOGGER.info("[autoshot] placed pack blocks");
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
