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
        SHOOT_1, W1, SHOOT_2, W2, SHOOT_3, W3, SHOOT_4, W4, SHOOT_5, DONE
    }

    private static Phase phase = Phase.BOOT;
    private static int ticks = 0;
    private static int wait = 0;
    private static String customTabId = "custom:0";

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

            ItemStack pack = new ItemStack(ModItems.leatherPack().get());
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

            sp.getInventory().items.set(0, pack);
            sp.getInventory().selected = 0;
            PackItem.openPack(sp, 0);
            Packwork.LOGGER.info("[autoshot] pack filled and opened");
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
