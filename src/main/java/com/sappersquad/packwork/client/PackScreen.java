package com.sappersquad.packwork.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.PackMenu;
import com.sappersquad.packwork.pack.PackViewSlot;
import com.sappersquad.packwork.sort.AutoTabs;
import com.sappersquad.packwork.sort.TabView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The pack, opened: a stamped-leather tab rail down the left, a stitched search
 * bar, the item grid, and the player's own pockets below. Clicking a tab shows its
 * compartment; the pack routes new items into tabs the moment they land.
 */
public class PackScreen extends AbstractContainerScreen<PackMenu> {

    private static final ResourceLocation BG = Packwork.id("textures/gui/pack.png");
    private static final ResourceLocation TAB = Packwork.id("textures/gui/tab.png");

    // rail geometry
    private static final int TAB_W = 26;
    private static final int RAIL_TOP = 22;
    private static final int TAB_TUCK = 4; // pixels of an inactive tab hidden under the frame

    // dye palette cycled by middle-clicking a custom tab
    private static final int[] DYES = {
            0, 0xFFB4595A, 0xFFB9905A, 0xFF8AB36B, 0xFF5FA05F, 0xFF6E8BB9,
            0xFF7A5A9B, 0xFF9C8265, 0xFFC9A24B, 0xFF6E7B8B
    };

    private CrispEditBox searchBox;
    private EditBox renameBox;
    private boolean renaming = false;

    // store gauges stacked under the sockets
    private static final int GAUGE_W = 16;
    private static final int GAUGE_H = 40;
    private static final int GAUGE_GAP = 4;

    private int tabPitch = 25;
    private final List<int[]> tabRects = new ArrayList<>(); // x,y,w,h per rendered tab (screen coords)
    private int[] gaugeRect = null;   // fluid gauge, or null when there's no rack
    private int[] xpGaugeRect = null; // soul-vial gauge, or null when there's no vial
    private int[] energyGaugeRect = null; // charge-crystal gauge, or null when there's no crystal
    private int[] flaskGaugeRect = null;  // flask-harness gauge, or null (needs Mekanism + the fitting)

    public PackScreen(PackMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = PackMenu.IMAGE_W;
        this.imageHeight = PackMenu.IMAGE_H;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new CrispEditBox(this.font, leftPos + 11, topPos + 20, 152, 10,
                Component.translatable("packwork.ui.search"), 0xFF3A2A18);
        searchBox.setMaxLength(48);
        searchBox.setHintText(Component.translatable("packwork.ui.search"));
        searchBox.setValue(menu.search());
        searchBox.setResponder(s -> PackClientActions.setSearch(menu, s));
        addRenderableWidget(searchBox);

        renameBox = new EditBox(this.font, leftPos + 8, topPos + 5, 116, 12, Component.translatable("packwork.ui.rename_tab"));
        renameBox.setMaxLength(24);
        renameBox.setVisible(false);
        addWidget(renameBox);
    }

    // ---------- background ----------

    @Override
    protected void containerTick() {
        super.containerTick();
        // The pack's contents ride on the item's data component and sync a tick after
        // the menu opens; recompute the tab view each tick so the grid reflects the
        // live contents (and re-sorts itself as items move).
        menu.rebuildView();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(BG, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight);
        drawTabRail(g, mouseX, mouseY);
        drawTrinketRail(g);
        drawToolRoll(g);
    }

    /**
     * The Tinker's Kit, unrolled: a leather tool roll laid across the pack's lower rows, with
     * three canvas pockets of workspace and a brass-ringed well for what comes out. Draws over
     * the grid rows the menu has deactivated, so the pack above stays usable.
     */
    private void drawToolRoll(GuiGraphics g) {
        if (!menu.rollActive()) return;
        int x = leftPos + 5, y = topPos + PackMenu.ROLL_Y - 6;
        int w = imageWidth - 10, h = 3 * 18 + 10;

        g.fill(x, y - 1, x + w, y, 0xFF241708);                    // the roll casts a shadow on the pack
        g.fill(x, y, x + w, y + h, 0xFF3D2A16);                    // dark oiled leather, distinct from the panel
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF6A4A2A);        // lit top roll
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF241708);
        g.renderOutline(x, y, w, h, 0xFFC9A24B);                   // brass binding all round

        // a canvas working field behind the 3x3 - the tool pockets are sewn onto it
        int cx = leftPos + PackMenu.ROLL_GRID_X - 4, cy = topPos + PackMenu.ROLL_Y - 3;
        int cw = 3 * 18 + 7, ch = 3 * 18 + 5;
        g.fill(cx, cy, cx + cw, cy + ch, 0xFF8A7A56);
        g.fill(cx, cy, cx + cw, cy + 1, 0xFFA89A74);
        g.fill(cx, cy + ch - 1, cx + cw, cy + ch, 0xFF6A5C3C);
        for (int sx = cx + 2; sx < cx + cw - 2; sx += 4) {          // stitched to the leather
            g.fill(sx, cy + 2, sx + 2, cy + 3, 0xFF4A3A1E);
            g.fill(sx, cy + ch - 4, sx + 2, cy + ch - 3, 0xFF4A3A1E);
        }

        // brass tacks at the roll's corners, and the two ties that hold it open
        for (int[] t : new int[][]{{x + 3, y + 3}, {x + w - 5, y + 3},
                {x + 3, y + h - 5}, {x + w - 5, y + h - 5}}) {
            g.fill(t[0], t[1], t[0] + 2, t[1] + 2, 0xFF8A6A28);
            g.fill(t[0], t[1], t[0] + 1, t[1] + 1, 0xFFE7CC82);
        }
        for (int ty : new int[]{y + 16, y + h - 22}) {
            g.fill(x + 1, ty, x + 4, ty + 3, 0xFF6B4A2F);
            g.fill(x + w - 4, ty, x + w - 1, ty + 3, 0xFF6B4A2F);
        }

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                slotWell(g, leftPos + PackMenu.ROLL_GRID_X + col * 18,
                        topPos + PackMenu.ROLL_Y + row * 18);

        // a brass awl pointing from the workspace to the finished piece
        int ax = leftPos + 96, ay = topPos + PackMenu.ROLL_RESULT_Y + 7;
        g.fill(ax, ay, ax + 12, ay + 2, 0xFFC9A24B);
        for (int i = 0; i < 4; i++) g.fill(ax + 8 + i, ay - 3 + i, ax + 9 + i, ay + 5 - i, 0xFFE7CC82);

        slotWell(g, leftPos + PackMenu.ROLL_RESULT_X, topPos + PackMenu.ROLL_RESULT_Y);
        g.renderOutline(leftPos + PackMenu.ROLL_RESULT_X - 2, topPos + PackMenu.ROLL_RESULT_Y - 2,
                20, 20, 0xFFC9A24B);
    }

    /** One recessed slot well in the leather, matching the ones baked into the panel. */
    private void slotWell(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF3C2A19);
        g.fill(x - 1, y - 1, x + 16, y, 0xFF2A1C10);
        g.fill(x - 1, y - 1, x, y + 16, 0xFF2A1C10);
        g.fill(x - 1, y + 16, x + 17, y + 17, 0xFF8A6540);
        g.fill(x + 16, y - 1, x + 17, y + 17, 0xFF8A6540);
    }

    /** Brass sockets on the right rail; the socket items render themselves as normal slots. */
    private void drawTrinketRail(GuiGraphics g) {
        int n = menu.trinketSlotCount();
        if (n <= 0) return;
        int railX = leftPos + PackMenu.TRINKET_X - 4;
        int railY = topPos + PackMenu.TRINKET_Y0 - 4;
        int railH = n * PackMenu.TRINKET_PITCH + 6;
        // a strip of stitched brass backing the sockets
        g.fill(railX - 1, railY - 1, railX + 24, railY + railH, 0xFF3E2A18);
        g.renderOutline(railX - 1, railY - 1, 25, railH + 1, 0xFFC9A24B);
        for (int i = 0; i < n; i++) {
            int x = leftPos + PackMenu.TRINKET_X - 1;
            int y = topPos + PackMenu.TRINKET_Y0 + i * PackMenu.TRINKET_PITCH - 1;
            g.fill(x, y, x + 18, y + 18, 0xFF3C2A19);
            g.fill(x, y, x + 17, y + 1, 0xFF2A1C10);
            g.fill(x, y, x + 1, y + 17, 0xFF2A1C10);
            g.fill(x, y + 17, x + 18, y + 18, 0xFF8A6540);
            g.fill(x + 17, y, x + 18, y + 18, 0xFF8A6540);
        }
    }

    private void drawTabRail(GuiGraphics g, int mouseX, int mouseY) {
        List<TabView> tabs = menu.tabs();
        tabRects.clear();
        int availH = imageHeight - RAIL_TOP - 8;
        tabPitch = Math.max(18, Math.min(25, tabs.isEmpty() ? 25 : availH / tabs.size()));
        int th = tabPitch - 1;

        for (int i = 0; i < tabs.size(); i++) {
            TabView t = tabs.get(i);
            boolean active = !menu.flatten() && t.id().equals(menu.activeTab());
            int y = topPos + RAIL_TOP + i * tabPitch;
            int x = leftPos - TAB_W + (active ? 1 : TAB_TUCK); // active tab pulls out to the left
            tabRects.add(new int[]{x, y, TAB_W - (active ? 1 : TAB_TUCK), th});

            // leather tab; inactive dimmed so the active one reads as pulled forward
            if (active) RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            else RenderSystem.setShaderColor(0.68f, 0.68f, 0.68f, 1f);
            g.blit(TAB, x, y, 0f, 0f, TAB_W, th, TAB_W, 24);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            // dye wash for custom tabs, plus a solid colour pip on the brass binding so
            // the dye reads even under the item icon
            if (t.color() != 0) {
                int wash = (t.color() & 0x00FFFFFF) | 0x88000000;
                g.fill(x + 3, y + 1, x + TAB_W - 2, y + th - 1, wash);
                int pip = t.color() | 0xFF000000;
                g.fill(x + 1, y + 2, x + 3, y + th - 2, pip);
            }

            g.renderItem(t.iconStack(), x + 5, y + (th - 16) / 2);
        }
    }

    // ---------- foreground ----------

    /** A bold red corner ribbon + brass tack on every item pinned to the active tab. */
    private void drawPinMarkers(GuiGraphics g) {
        if (menu.flatten()) return;
        String active = menu.activeTab();
        com.sappersquad.packwork.sort.PackLayout layout = menu.layout();
        g.pose().pushPose();
        g.pose().translate(0, 0, 300); // ride above the item sprites (item z ~150, count ~200)
        for (Slot s : menu.slots) {
            if (!(s instanceof PackViewSlot vs) || !vs.isActive() || !s.hasItem()) continue;
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(s.getItem().getItem());
            if (active.equals(layout.pinnedTab(key))) {
                drawPinRibbon(g, leftPos + s.x, topPos + s.y);
            }
        }
        g.pose().popPose();
    }

    /** A folded red ribbon in the slot's top-left corner, studded with a brass tack. Big and
     *  unmistakable so a pinned slot reads at a glance; only clips the very corner of the item. */
    private void drawPinRibbon(GuiGraphics g, int x, int y) {
        // triangular ribbon fold from the corner (rows of shrinking width) + a dark edge under it
        for (int i = 0; i < 8; i++) g.fill(x - 1, y - 1 + i, x - 1 + (8 - i), y + i, 0xFF48120F); // shadow/backing
        for (int i = 0; i < 7; i++) g.fill(x - 1, y - 1 + i, x - 1 + (7 - i), y + i, 0xFFC0332F); // red ribbon
        for (int i = 0; i < 4; i++) g.fill(x - 1, y - 1 + i, x - 1 + (4 - i), y + i, 0xFFDA5A54); // lit inner fold
        // a short tail hanging off the corner
        g.fill(x + 4, y + 4, x + 6, y + 8, 0xFFA82B28);
        g.fill(x + 5, y + 6, x + 7, y + 9, 0xFF8A211F);
        // brass tack pinning the ribbon
        g.fill(x, y, x + 4, y + 4, 0xFF2A1C10);          // dark seat
        g.fill(x, y, x + 3, y + 3, 0xFFC9A24B);          // brass head
        g.fill(x + 1, y + 1, x + 3, y + 3, 0xFFF0DCA0);  // highlight
        g.fill(x + 2, y + 2, x + 3, y + 3, 0xFF8A6A28);  // shaded corner
    }

    /** Append a "[P] Pin to this tab" line to a hovered grid item's tooltip, so it's discoverable. */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tip = super.getTooltipFromContainerItem(stack);
        if (!menu.flatten() && this.hoveredSlot instanceof PackViewSlot && this.hoveredSlot.hasItem()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            boolean pinnedHere = menu.activeTab().equals(menu.layout().pinnedTab(key));
            List<Component> out = new ArrayList<>(tip);
            out.add(Component.translatable(pinnedHere ? "packwork.ui.unpin_key" : "packwork.ui.pin_key",
                            PackKeyMappings.PIN.getTranslatedKeyMessage())
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            return out;
        }
        return tip;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        if (!renaming) {
            g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xE8DCC0, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        drawPinMarkers(g);
        if (renaming) renameBox.render(g, mouseX, mouseY, partialTick);
        drawButtons(g, mouseX, mouseY);
        drawPageNav(g, mouseX, mouseY);
        drawStoreGauges(g);
        drawHoverTooltips(g, mouseX, mouseY);
        this.renderTooltip(g, mouseX, mouseY);
    }

    // Title-strip buttons: tool roll (only with a kit fitted), flatten toggle, tidy up, new tab.
    private int rollBtnX() { return leftPos + 116; }
    private int flatBtnX() { return leftPos + 130; }
    private int tidyBtnX() { return leftPos + 144; }
    private int newBtnX()  { return leftPos + 158; }
    private int btnY()     { return topPos + 4; }
    private static final int BTN = 12;

    private boolean hasKit() {
        return menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.TINKERS_KIT);
    }

    private void drawButtons(GuiGraphics g, int mouseX, int mouseY) {
        drawPlate(g, flatBtnX(), btnY(), inRect(mouseX, mouseY, flatBtnX(), btnY(), BTN, BTN), menu.flatten());
        drawPlate(g, tidyBtnX(), btnY(), inRect(mouseX, mouseY, tidyBtnX(), btnY(), BTN, BTN), false);
        drawPlate(g, newBtnX(), btnY(), inRect(mouseX, mouseY, newBtnX(), btnY(), BTN, BTN), false);

        // glyphs (brass on the plate)
        int gl = 0xFFEAD9A6;
        if (hasKit()) {   // the tool-roll latch appears only once a Tinker's Kit is fitted
            drawPlate(g, rollBtnX(), btnY(), inRect(mouseX, mouseY, rollBtnX(), btnY(), BTN, BTN), menu.rollActive());
            g.fill(rollBtnX() + 2, btnY() + 4, rollBtnX() + 10, btnY() + 6, gl);   // the rolled leather
            g.fill(rollBtnX() + 2, btnY() + 7, rollBtnX() + 10, btnY() + 8, 0xFF8A6A28);
            g.fill(rollBtnX() + 4, btnY() + 2, rollBtnX() + 5, btnY() + 4, gl);    // two tools poking out
            g.fill(rollBtnX() + 7, btnY() + 2, rollBtnX() + 8, btnY() + 4, gl);
        }
        // flatten: a 2x2 grid of dots
        g.fill(flatBtnX() + 3, btnY() + 3, flatBtnX() + 5, btnY() + 5, gl);
        g.fill(flatBtnX() + 7, btnY() + 3, flatBtnX() + 9, btnY() + 5, gl);
        g.fill(flatBtnX() + 3, btnY() + 7, flatBtnX() + 5, btnY() + 9, gl);
        g.fill(flatBtnX() + 7, btnY() + 7, flatBtnX() + 9, btnY() + 9, gl);
        // tidy: three descending sort lines
        g.fill(tidyBtnX() + 3, btnY() + 3, tidyBtnX() + 9, btnY() + 4, gl);
        g.fill(tidyBtnX() + 3, btnY() + 6, tidyBtnX() + 8, btnY() + 7, gl);
        g.fill(tidyBtnX() + 3, btnY() + 9, tidyBtnX() + 6, btnY() + 10, gl);
        // new: a plus
        g.fill(newBtnX() + 5, btnY() + 3, newBtnX() + 7, btnY() + 9, gl);
        g.fill(newBtnX() + 3, btnY() + 5, newBtnX() + 9, btnY() + 7, gl);
    }

    /** Glass store gauges under the trinket sockets: a waterskin, then a soul vial. */
    private void drawStoreGauges(GuiGraphics g) {
        gaugeRect = null;
        xpGaugeRect = null;
        energyGaugeRect = null;
        flaskGaugeRect = null;
        int x = leftPos + PackMenu.TRINKET_X - 1;
        int y = topPos + gaugeTopY();
        int w = GAUGE_W, h = GAUGE_H;

        if (menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.WATERSKIN)) {
            gaugeRect = new int[]{x, y, w, h};
            drawFluidGauge(g, x, y, w, h);
            y += h + GAUGE_GAP;
        }
        if (menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.SOUL_VIAL)) {
            xpGaugeRect = new int[]{x, y, w, h};
            drawXpGauge(g, x, y, w, h);
            y += h + GAUGE_GAP;
        }
        if (menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL)) {
            energyGaugeRect = new int[]{x, y, w, h};
            drawEnergyGauge(g, x, y, w, h);
            y += h + GAUGE_GAP;
        }
        // Gas store: only meaningful with Mekanism, so the gauge appears only when it's loaded.
        if (menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.FLASK_HARNESS)
                && net.neoforged.fml.ModList.get().isLoaded("mekanism")) {
            flaskGaugeRect = new int[]{x, y, w, h};
            drawFlaskGauge(g, x, y, w, h);
        }
    }

    /** First gauge's top edge, relative to {@code topPos} - the sockets end here. */
    private int gaugeTopY() {
        return PackMenu.TRINKET_Y0 + Math.max(menu.trinketSlotCount(), 1) * PackMenu.TRINKET_PITCH + 4;
    }

    /** How many store gauges the rail is showing right now (drives the rail's click region). */
    private int gaugeCount() {
        int n = 0;
        if (menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.WATERSKIN)) n++;
        if (menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.SOUL_VIAL)) n++;
        if (menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.CHARGE_CRYSTAL)) n++;
        if (menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.FLASK_HARNESS)
                && net.neoforged.fml.ModList.get().isLoaded("mekanism")) n++;
        return n;
    }

    private void drawFlaskGauge(GuiGraphics g, int x, int y, int w, int h) {
        gaugeFrame(g, x, y, w, h, 0xFF241F2C);
        long stored = menu.chemicalStored();
        long cap = menu.chemicalCapacity();
        if (stored > 0 && cap > 0) {
            int filled = Math.max(1, (int) (h * Math.min(stored, cap) / cap));
            g.fill(x, y + h - filled, x + w, y + h, 0xFFB08AD8);          // bottled-vapor violet
            g.fill(x, y + h - filled, x + w, y + h - filled + 1, 0xFFE4CCFF);
        }
        g.fill(x + 1, y + 1, x + 3, y + h - 1, 0x33FFFFFF);
    }

    private void drawEnergyGauge(GuiGraphics g, int x, int y, int w, int h) {
        gaugeFrame(g, x, y, w, h, 0xFF15323B);                          // cool crystal-teal glass
        int stored = menu.energyStored();
        int cap = menu.energyCapacity();
        if (stored > 0 && cap > 0) {
            int filled = Math.max(1, (int) ((long) h * Math.min(stored, cap) / cap));
            g.fill(x, y + h - filled, x + w, y + h, 0xFF3EA9C4);        // Charge-Crystal blue (matches the fitting)
            g.fill(x, y + h - filled, x + w, y + h - filled + 1, 0xFFAEEFF7);
        }
        g.fill(x + 1, y + 1, x + 3, y + h - 1, 0x33FFFFFF);
    }

    private void gaugeFrame(GuiGraphics g, int x, int y, int w, int h, int glass) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF3E2A18);
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, 0xFFC9A24B);
        g.fill(x, y, x + w, y + h, glass);
    }

    private void drawFluidGauge(GuiGraphics g, int x, int y, int w, int h) {
        gaugeFrame(g, x, y, w, h, 0xFF20303A);
        FluidStack fs = menu.fluidStack();
        int cap = menu.fluidCapacity();
        if (!fs.isEmpty() && cap > 0) {
            int filled = Math.max(1, (int) ((long) h * Math.min(fs.getAmount(), cap) / cap));
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fs.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fs));
            int tint = ext.getTintColor(fs);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(
                    ((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f, 1f);
            for (int yy = 0; yy < filled; yy += 16) {
                int hh = Math.min(16, filled - yy);
                g.blit(x, y + h - yy - hh, 0, w, hh, sprite);
            }
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        g.fill(x + 1, y + 1, x + 3, y + h - 1, 0x33FFFFFF);
    }

    private void drawXpGauge(GuiGraphics g, int x, int y, int w, int h) {
        gaugeFrame(g, x, y, w, h, 0xFF1C241A);
        int stored = menu.xpStored();
        int cap = menu.xpCapacity();
        if (stored > 0 && cap > 0) {
            int filled = Math.max(1, (int) ((long) h * Math.min(stored, cap) / cap));
            // experience green, with a lighter top edge
            g.fill(x, y + h - filled, x + w, y + h, 0xFF74C043);
            g.fill(x, y + h - filled, x + w, y + h - filled + 1, 0xFFB6F27A);
        }
        g.fill(x + 1, y + 1, x + 3, y + h - 1, 0x33FFFFFF);
    }

    private void drawPlate(GuiGraphics g, int x, int y, boolean hover, boolean on) {
        int base = on ? 0xFF8A6A28 : 0xFF6B4A2F;
        int edge = hover ? 0xFFE7CC82 : 0xFFC9A24B;
        g.fill(x, y, x + BTN, y + BTN, base);
        g.renderOutline(x, y, BTN, BTN, edge);
    }

    private void drawPageNav(GuiGraphics g, int mouseX, int mouseY) {
        if (menu.pageCount() <= 1) return;
        int y = topPos + 143;
        int lx = leftPos + 8, rx = leftPos + 160;
        boolean lh = inRect(mouseX, mouseY, lx, y, 8, 8);
        boolean rh = inRect(mouseX, mouseY, rx, y, 8, 8);
        triangle(g, lx, y, true, lh);
        triangle(g, rx, y, false, rh);
        String p = (menu.page() + 1) + "/" + menu.pageCount();
        g.drawString(this.font, p, leftPos + 88 - this.font.width(p) / 2, y, 0xE8DCC0, false);
    }

    private void triangle(GuiGraphics g, int x, int y, boolean left, boolean hover) {
        int c = hover ? 0xFFE7CC82 : 0xFFC9A24B;
        for (int i = 0; i < 4; i++) {
            int col = left ? x + i : x + 7 - i;
            g.fill(col, y + 3 - i, col + 1, y + 5 + i, c);
        }
    }

    private void drawHoverTooltips(GuiGraphics g, int mouseX, int mouseY) {
        // tab names
        List<TabView> tabs = menu.tabs();
        for (int i = 0; i < tabRects.size() && i < tabs.size(); i++) {
            int[] r = tabRects.get(i);
            if (inRect(mouseX, mouseY, r[0], r[1], r[2], r[3])) {
                TabView t = tabs.get(i);
                List<Component> lines = new ArrayList<>();
                lines.add(t.name());
                if (t.loose()) lines.add(Component.translatable("packwork.ui.loose_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                else if (t.editable()) lines.add(Component.translatable("packwork.ui.tab_edit_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                else lines.add(Component.translatable("packwork.ui.tab_pin_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }
        if (gaugeRect != null && inRect(mouseX, mouseY, gaugeRect[0], gaugeRect[1], gaugeRect[2], gaugeRect[3])) {
            FluidStack fs = menu.fluidStack();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("packwork.ui.waterskin"));
            if (fs.isEmpty()) lines.add(Component.translatable("packwork.ui.tank_empty").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            else lines.add(fs.getHoverName().copy().append(" - " + fs.getAmount() + " / " + menu.fluidCapacity() + " mB").withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.add(Component.translatable("packwork.ui.tank_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        if (xpGaugeRect != null && inRect(mouseX, mouseY, xpGaugeRect[0], xpGaugeRect[1], xpGaugeRect[2], xpGaugeRect[3])) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("packwork.ui.soul_vial"));
            lines.add(Component.literal(menu.xpStored() + " / " + menu.xpCapacity() + " XP")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.add(Component.translatable("packwork.ui.vial_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        if (energyGaugeRect != null && inRect(mouseX, mouseY, energyGaugeRect[0], energyGaugeRect[1], energyGaugeRect[2], energyGaugeRect[3])) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("packwork.ui.charge_crystal"));
            lines.add(Component.literal(menu.energyStored() + " / " + menu.energyCapacity() + " FE")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.add(Component.translatable("packwork.ui.crystal_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        if (flaskGaugeRect != null && inRect(mouseX, mouseY, flaskGaugeRect[0], flaskGaugeRect[1], flaskGaugeRect[2], flaskGaugeRect[3])) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("packwork.ui.flask_harness"));
            lines.add(Component.literal(menu.chemicalStored() + " / " + menu.chemicalCapacity() + " mB")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
            lines.add(Component.translatable("packwork.ui.flask_hint").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        if (hasKit() && inRect(mouseX, mouseY, rollBtnX(), btnY(), BTN, BTN)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(menu.rollActive()
                    ? "packwork.ui.roll_up" : "packwork.ui.unroll"));
            lines.add(Component.translatable("packwork.ui.roll_hint")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        } else if (inRect(mouseX, mouseY, flatBtnX(), btnY(), BTN, BTN))
            g.renderTooltip(this.font, Component.translatable("packwork.ui.flatten"), mouseX, mouseY);
        else if (inRect(mouseX, mouseY, tidyBtnX(), btnY(), BTN, BTN))
            g.renderTooltip(this.font, Component.translatable("packwork.ui.tidy"), mouseX, mouseY);
        else if (inRect(mouseX, mouseY, newBtnX(), btnY(), BTN, BTN))
            g.renderTooltip(this.font, Component.translatable("packwork.ui.new_tab"), mouseX, mouseY);
    }

    // ---------- input ----------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (renaming) {
            if (renameBox.mouseClicked(mx, my, button)) return true;
            commitRename();
        }
        // title buttons
        if (button == 0) {
            if (hasKit() && inRect((int) mx, (int) my, rollBtnX(), btnY(), BTN, BTN)) {
                PackClientActions.toggleRoll(menu);
                return true;
            }
            if (inRect((int) mx, (int) my, flatBtnX(), btnY(), BTN, BTN)) {
                PackClientActions.toggleFlatten(menu);
                return true;
            }
            if (inRect((int) mx, (int) my, tidyBtnX(), btnY(), BTN, BTN)) {
                PackClientActions.tidyUp(menu);
                return true;
            }
            if (inRect((int) mx, (int) my, newBtnX(), btnY(), BTN, BTN)) {
                PackClientActions.newTab(menu);
                return true;
            }
            if (menu.pageCount() > 1) {
                int y = topPos + 143;
                if (inRect((int) mx, (int) my, leftPos + 8, y, 8, 8)) { PackClientActions.page(menu, -1); return true; }
                if (inRect((int) mx, (int) my, leftPos + 160, y, 8, 8)) { PackClientActions.page(menu, 1); return true; }
            }
            if (gaugeRect != null && inRect((int) mx, (int) my, gaugeRect[0], gaugeRect[1], gaugeRect[2], gaugeRect[3])) {
                PackClientActions.fluidInteract(menu); // fill/drain with the item on the cursor
                return true;
            }
            if (xpGaugeRect != null && inRect((int) mx, (int) my, xpGaugeRect[0], xpGaugeRect[1], xpGaugeRect[2], xpGaugeRect[3])) {
                if (hasShiftDown()) PackClientActions.xpPour(menu);
                else PackClientActions.xpSiphon(menu);
                return true;
            }
            // The charge crystal and flask harness have no click verb, but a click still has
            // to be swallowed here so it never reaches vanilla's outside-the-panel handling.
            if (energyGaugeRect != null && inRect((int) mx, (int) my, energyGaugeRect[0], energyGaugeRect[1], energyGaugeRect[2], energyGaugeRect[3])) {
                return true;
            }
            if (flaskGaugeRect != null && inRect((int) mx, (int) my, flaskGaugeRect[0], flaskGaugeRect[1], flaskGaugeRect[2], flaskGaugeRect[3])) {
                return true;
            }
        }
        // rail tabs
        int tab = tabAt((int) mx, (int) my);
        if (tab >= 0) {
            TabView t = menu.tabs().get(tab);
            if (button == 0) {
                PackClientActions.selectTab(menu, t.id());
            } else if (button == 1 && t.editable()) {
                PackClientActions.deleteTab(menu, t.id()); // remove a custom compartment
            } else if (button == 2 && t.editable()) {
                cycleDye(t);
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    /**
     * The tab rail and the fittings rail deliberately hang OUTSIDE the panel rect, which is
     * exactly the region vanilla treats as "clicked outside the GUI" - i.e. throw whatever is
     * on the cursor onto the floor. That fired on mouse RELEASE
     * ({@code AbstractContainerScreen.mouseReleased} -&gt; {@code slotClicked(null, -999, PICKUP)}
     * -&gt; {@code player.drop(...)}), so consuming the press was never enough: clicking the
     * waterskin gauge with a bucket filled the tank AND threw the bucket on the ground.
     * Teaching the screen that its own rails count as inside kills it at the source, for the
     * press and the release both.
     */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        if (isOverRail((int) mouseX, (int) mouseY)) return false;
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton);
    }

    /** Everything the pack draws beyond the panel edges: the tab rail left, the fittings rail right. */
    private boolean isOverRail(int mx, int my) {
        // left: stamped leather tabs (they tuck under the frame, so start a touch further out)
        if (mx >= leftPos - TAB_W && mx < leftPos
                && my >= topPos + RAIL_TOP && my < topPos + imageHeight) return true;
        // right: brass sockets, then the stack of store gauges beneath them
        int bottom = gaugeTopY() + gaugeCount() * (GAUGE_H + GAUGE_GAP);
        return mx >= leftPos + PackMenu.TRINKET_X - 5 && mx < leftPos + PackMenu.TRINKET_X + 24
                && my >= topPos + PackMenu.TRINKET_Y0 - 5 && my < topPos + bottom;
    }

    private int tabAt(int mx, int my) {
        for (int i = 0; i < tabRects.size(); i++) {
            int[] r = tabRects.get(i);
            if (inRect(mx, my, r[0], r[1], r[2], r[3])) return i;
        }
        return -1;
    }

    private void cycleDye(TabView t) {
        int cur = t.color();
        int idx = 0;
        for (int i = 0; i < DYES.length; i++) if (DYES[i] == cur) { idx = i; break; }
        int next = DYES[(idx + 1) % DYES.length];
        PackClientActions.tabColor(menu, t.id(), next);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (renaming) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { commitRename(); return true; }
            if (key == GLFW.GLFW_KEY_ESCAPE) { renaming = false; renameBox.setVisible(false); return true; }
            return renameBox.keyPressed(key, scan, mods) || renameBox.canConsumeInput() || super.keyPressed(key, scan, mods);
        }
        if (searchBox != null && searchBox.isFocused()) {
            return super.keyPressed(key, scan, mods);
        }
        Slot hovered = this.hoveredSlot;
        boolean overGrid = hovered instanceof PackViewSlot && hovered.hasItem();

        // Pin/unpin the hovered item to the active tab. Rebindable keybind (default P), so it
        // reads the mapping rather than a hardcoded key and shows up in vanilla Controls.
        if (PackKeyMappings.PIN.matches(key, scan) && overGrid && !menu.flatten()) {
            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(hovered.getItem().getItem());
            String pinned = menu.layout().pinnedTab(itemKey);
            if (menu.activeTab().equals(pinned)) PackClientActions.unpin(menu, itemKey.toString());
            else PackClientActions.pin(menu, menu.activeTab(), itemKey.toString());
            return true;
        }

        switch (key) {
            case GLFW.GLFW_KEY_I -> { // stamp hovered item as the active custom tab's icon
                if (overGrid && isActiveCustom()) {
                    String id = BuiltInRegistries.ITEM.getKey(hovered.getItem().getItem()).toString();
                    PackClientActions.tabIcon(menu, menu.activeTab(), id);
                    return true;
                }
            }
            case GLFW.GLFW_KEY_R -> { if (isActiveCustom()) { beginRename(); return true; } }
            case GLFW.GLFW_KEY_O -> { // toggle hovered item on the Compass Rose void list
                if (overGrid && menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.COMPASS_ROSE)) {
                    String id = BuiltInRegistries.ITEM.getKey(hovered.getItem().getItem()).toString();
                    PackClientActions.voidToggle(menu, id);
                    return true;
                }
            }
            case GLFW.GLFW_KEY_LEFT_BRACKET -> { PackClientActions.moveTab(menu, menu.activeTab(), -1); return true; }
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> { PackClientActions.moveTab(menu, menu.activeTab(), 1); return true; }
            default -> {}
        }
        return super.keyPressed(key, scan, mods);
    }

    /** Dev harness only: the tool-roll latch's centre in GUI space (null without a kit fitted). */
    public int[] devRollButtonCenter() {
        return hasKit() ? new int[]{rollBtnX() + BTN / 2, btnY() + BTN / 2} : null;
    }

    /** Dev harness only: the waterskin gauge's centre in GUI space (null when there's no rack). */
    public int[] devFluidGaugeCenter() {
        return gaugeRect == null ? null
                : new int[]{gaugeRect[0] + gaugeRect[2] / 2, gaugeRect[1] + gaugeRect[3] / 2};
    }

    /** Dev harness only: force which slot counts as hovered, so a synthetic key press can target it. */
    public void devHover(int menuIndex) {
        this.hoveredSlot = (menuIndex >= 0 && menuIndex < menu.slots.size()) ? menu.slots.get(menuIndex) : null;
    }

    private boolean isActiveCustom() {
        for (TabView t : menu.tabs()) if (t.id().equals(menu.activeTab())) return t.editable();
        return false;
    }

    private void beginRename() {
        String current = "";
        for (TabView t : menu.tabs()) if (t.id().equals(menu.activeTab())) current = t.name().getString();
        renameBox.setValue(current);
        renameBox.setVisible(true);
        renameBox.setFocused(true);
        setFocused(renameBox);
        renaming = true;
    }

    private void commitRename() {
        if (!renaming) return;
        renaming = false;
        renameBox.setVisible(false);
        if (isActiveCustom()) PackClientActions.renameTab(menu, menu.activeTab(), renameBox.getValue().trim());
    }

    private static boolean inRect(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}
