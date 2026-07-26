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

    // a stitched parchment note confirming a pin/unpin ("Pinned Bread to Ores...")
    private Component pinNote = null;
    private int pinNoteTicks = 0;

    // ---- the Recipe Ledger: the Tinker's Kit browser (pure client; items move server-side only) ----
    private static final int BR_W = 96;
    private static final int BR_COLS = 4;

    /** One ledger row: the recipe plus its result + name, resolved ONCE per recompute so
     *  neither the per-frame draw nor the sort re-derives them. */
    private record LedgerEntry(net.minecraft.world.item.crafting.RecipeHolder<?> holder,
                               ItemStack result, String lowerName) {}

    private boolean browserOpen = false;
    private EditBox browserSearch;
    private int browserScroll = 0;
    private final List<LedgerEntry> allCraftable = new ArrayList<>(); // everything stock covers
    private final List<LedgerEntry> craftable = new ArrayList<>();    // the searched view of it
    private int browserRecomputeIn = 0;
    private boolean hasKitCached = false; // trinket reads stream a component; refresh per tick, not per frame
    private net.minecraft.world.item.crafting.RecipeHolder<?> ghost = null;
    private net.minecraft.world.item.crafting.Ingredient[] ghostGrid =
            new net.minecraft.world.item.crafting.Ingredient[9];

    // ---- the Rule Editor: the Quill & Ledger's parchment sheet for a custom tab's filters ----
    private static final int RU_W = 112;
    private static final int RULE_ROW_H = 11;
    private boolean rulesOpen = false;
    private EditBox ruleValueBox;
    private int rulesScroll = 0;

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

        browserSearch = new EditBox(this.font, browserX() + 5, browserY() + 16, BR_W - 10, 12,
                Component.translatable("packwork.ui.search"));
        browserSearch.setMaxLength(32);
        browserSearch.setVisible(browserOpen);   // a window resize re-inits mid-ledger
        browserSearch.setResponder(s -> {
            browserScroll = 0;
            applyLedgerFilter();   // typing filters the cached list; it never rescans recipes
        });
        addWidget(browserSearch);
        repositionForBrowser();   // a resize re-inits; keep the ledger shift if it's open

        ruleValueBox = new EditBox(this.font, rulesX() + 5, rulesAddTop() + 10, RU_W - 10, 12,
                Component.translatable("packwork.ui.rules_add"));
        ruleValueBox.setMaxLength(48);
        ruleValueBox.setHint(Component.translatable("packwork.ui.rules_hint_value"));
        ruleValueBox.setVisible(rulesOpen);   // a window resize re-inits mid-edit
        addWidget(ruleValueBox);

        // dropping an item into a tab it wouldn't sort to auto-pins it there; say so
        menu.setPinToast((stack, tabName) -> showPinNote(
                Component.translatable("packwork.ui.pinned_note", stack.getHoverName(), tabName)));
    }

    /** Raise the pin note over the panel for a few seconds. */
    private void showPinNote(Component note) {
        this.pinNote = note;
        this.pinNoteTicks = 70;
    }

    /** The parchment pin note, centred over the seam between the grid and your pockets. */
    private void drawPinNote(GuiGraphics g) {
        if (pinNote == null) return;
        int w = this.font.width(pinNote) + 12;
        int x = leftPos + (imageWidth - w) / 2;
        int y = topPos + 141;
        g.pose().pushPose();
        g.pose().translate(0, 0, 350); // above items, ribbons, and the page nav
        g.fill(x, y, x + w, y + 13, 0xFFC8B892);            // parchment
        g.fill(x, y, x + w, y + 1, 0xFFE2D6AE);             // lit top edge
        g.fill(x, y + 12, x + w, y + 13, 0xFFA89A74);       // shaded bottom edge
        g.renderOutline(x - 1, y - 1, w + 2, 15, 0xFFC9A24B); // brass binding
        g.drawString(this.font, pinNote, x + 6, y + 3, 0xFF3A2A18, false);
        g.pose().popPose();
    }

    /**
     * The open ledger widens the whole ensemble past what a small window centres, so shift
     * the GUI left to make room - the same move vanilla's recipe book makes. Everything
     * derives from {@code leftPos} except the two absolutely-placed edit boxes, which follow.
     */
    private void repositionForBrowser() {
        int sheet = ledgerVisible() ? BR_W : rulesVisible() ? RU_W : 0;
        int total = sheet > 0 ? imageWidth + 30 + sheet : imageWidth;
        this.leftPos = Math.max(TAB_W + 2, (this.width - total) / 2);
        if (searchBox != null) searchBox.setX(leftPos + 11);
        if (renameBox != null) renameBox.setX(leftPos + 8);
        if (browserSearch != null) {   // the sheet's search rides the sheet, nowhere else
            browserSearch.setX(browserX() + 4);
            browserSearch.setY(browserY() + 16);
        }
        if (ruleValueBox != null) {
            ruleValueBox.setX(rulesX() + 5);
            ruleValueBox.setY(rulesAddTop() + 10);
        }
    }

    // ---------- background ----------

    @Override
    protected void containerTick() {
        super.containerTick();
        // The pack's contents ride on the item's data component and sync a tick after
        // the menu opens; recompute the tab view each tick so the grid reflects the
        // live contents (and re-sorts itself as items move).
        menu.rebuildView();

        if (pinNoteTicks > 0 && --pinNoteTicks == 0) pinNote = null;

        // The ledger lives and dies with the tool roll; refresh its list as stock shifts.
        hasKitCached = menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.TINKERS_KIT);
        if (!menu.rollActive() && (browserOpen || ghost != null)) {
            closeBrowser();
        }
        if (browserOpen && --browserRecomputeIn <= 0) {
            recomputeCraftable();
        }

        // The rule editor lives and dies with its gate: the Quill & Ledger fitted and a
        // custom tab active. Pull either and the sheet folds away.
        if (rulesOpen && !canEditRules()) {
            closeRules();
        }
    }

    private void closeBrowser() {
        browserOpen = false;
        ghost = null;
        java.util.Arrays.fill(ghostGrid, null);
        if (browserSearch != null) browserSearch.setVisible(false);
        repositionForBrowser();
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

    // ---- the Recipe Ledger panel ----

    private int browserX() { return leftPos + PackMenu.TRINKET_X + 26; }
    private int browserY() { return topPos + 4; }
    private int browserH() { return imageHeight - 8; }
    private int browserGridY() { return browserY() + 32; }
    private int browserRows() { return (browserH() - 40) / 18; }

    /**
     * Everything the pack could make RIGHT NOW: every 3x3-able crafting recipe checked
     * against the pack's own stock (at full depth) plus what's already on the roll. Runs
     * client-side over synced data - the same check vanilla's book runs against the player
     * inventory, pointed at the pack instead. Recomputed on open and every couple of
     * seconds while stock shifts; each entry caches its result + name once so the sort,
     * the search, and the per-frame draw never re-derive them. A search keystroke only
     * re-filters this list ({@link #applyLedgerFilter}) - it never rescans the recipes.
     */
    private void recomputeCraftable() {
        browserRecomputeIn = 40;
        allCraftable.clear();
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var contents = new net.minecraft.world.entity.player.StackedContents();
        menu.fillPackStacked(contents);
        for (var holder : level.getRecipeManager().getAllRecipesFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            var r = holder.value();
            if (!r.canCraftInDimensions(3, 3)) continue;
            if (r.getIngredients().isEmpty()) continue;   // special recipes have no layable shape
            ItemStack result = r.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;
            if (!contents.canCraft(r, null)) continue;
            allCraftable.add(new LedgerEntry(holder, result,
                    result.getHoverName().getString().toLowerCase(java.util.Locale.ROOT)));
        }
        allCraftable.sort(java.util.Comparator.comparing(LedgerEntry::lowerName));
        applyLedgerFilter();
    }

    /** Narrow the cached list by the search text - cheap enough to run per keystroke. */
    private void applyLedgerFilter() {
        craftable.clear();
        String q = browserSearch == null ? "" : browserSearch.getValue().toLowerCase(java.util.Locale.ROOT).trim();
        for (LedgerEntry e : allCraftable) {
            if (q.isEmpty() || e.lowerName().contains(q)) craftable.add(e);
        }
        browserScroll = Math.min(browserScroll, maxLedgerScroll());
    }

    /** The parchment ledger: a searchable sheet of everything craftable from pack stock. */
    private void drawBrowser(GuiGraphics g, int mouseX, int mouseY) {
        if (!ledgerVisible()) return;
        int x = browserX(), y = browserY(), w = BR_W, h = browserH();

        // parchment sheet with a leather spine toward the pack and brass tacks
        g.fill(x - 2, y, x + w, y + h, 0xFF3E2A18);
        g.fill(x, y + 1, x + w - 1, y + h - 1, 0xFFC8B892);
        g.fill(x, y + 1, x + w - 1, y + 2, 0xFFE2D6AE);
        g.fill(x, y + h - 2, x + w - 1, y + h - 1, 0xFFA89A74);
        g.renderOutline(x - 2, y, w + 2, h, 0xFFC9A24B);
        for (int[] t : new int[][]{{x + 2, y + 3}, {x + w - 5, y + 3}, {x + 2, y + h - 5}, {x + w - 5, y + h - 5}}) {
            g.fill(t[0], t[1], t[0] + 2, t[1] + 2, 0xFF8A6A28);
        }
        g.drawString(this.font, Component.translatable("packwork.ui.recipe_ledger"),
                x + 5, y + 5, 0xFF3A2A18, false);
        browserSearch.render(g, mouseX, mouseY, 0);

        if (craftable.isEmpty()) {
            g.drawWordWrap(this.font, Component.translatable("packwork.ui.ledger_empty"),
                    x + 5, browserGridY() + 4, w - 10, 0xFF6A5A40);
            return;
        }

        int rows = browserRows();
        for (int rIdx = 0; rIdx < rows * BR_COLS; rIdx++) {
            int i = (browserScroll * BR_COLS) + rIdx;
            if (i >= craftable.size()) break;
            LedgerEntry entry = craftable.get(i);
            int cx = cellX(rIdx), cy = cellY(rIdx);
            boolean hovered = inRect(mouseX, mouseY, cx - 1, cy - 1, 18, 18);
            boolean selected = ghost != null && ghost.id().equals(entry.holder().id());
            if (selected) {
                g.fill(cx - 1, cy - 1, cx + 17, cy + 17, 0xFF8A6A28);
            } else if (hovered) {
                g.fill(cx - 1, cy - 1, cx + 17, cy + 17, 0x40573B23);
            }
            g.renderItem(entry.result(), cx, cy);
            g.renderItemDecorations(this.font, entry.result(), cx, cy);
            if (hovered) {
                List<Component> tip = new ArrayList<>();
                tip.add(entry.result().getHoverName());
                tip.add(Component.translatable(selected ? "packwork.ui.ledger_unchalk" : "packwork.ui.ledger_chalk")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                if (selected) {
                    tip.add(Component.translatable("packwork.ui.ghost_hint")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                }
                g.renderComponentTooltip(this.font, tip, mouseX, mouseY);
            }
        }
        // scroll hint: a thin brass track when there's more than fits
        int maxScroll = maxLedgerScroll();
        if (maxScroll > 0) {
            int trackY = browserGridY(), trackH = rows * 18 - 2;
            g.fill(x + w - 4, trackY, x + w - 3, trackY + trackH, 0xFF8A6A28);
            int nub = trackY + (int) ((trackH - 8) * (browserScroll / (double) maxScroll));
            g.fill(x + w - 5, nub, x + w - 2, nub + 8, 0xFFC9A24B);
        }
    }

    /** Ghost the chalked recipe into the roll's EMPTY cells - paint only, items never move here. */
    private void drawGhost(GuiGraphics g) {
        if (ghost == null || !menu.rollActive()) return;
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        long cycle = level.getGameTime() / 30;
        for (int cell = 0; cell < 9; cell++) {
            var ing = ghostGrid[cell];
            if (ing == null || ing.isEmpty()) continue;
            Slot slot = menu.slots.get(menu.craftStart() + cell);
            if (slot.hasItem()) continue;                      // real items win over chalk
            ItemStack[] options = ing.getItems();
            if (options.length == 0) continue;
            ItemStack show = options[(int) (cycle % options.length)];
            ghostInto(g, show, leftPos + slot.x, topPos + slot.y);
        }
        // and the promised result, washed the same way, in the empty result well
        Slot well = menu.slots.get(menu.resultIndex());
        if (!well.hasItem()
                && ghost.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe cr) {
            ItemStack result = cr.getResultItem(level.registryAccess());
            if (!result.isEmpty()) ghostInto(g, result, leftPos + well.x, topPos + well.y);
        }
    }

    /** One chalked item: the fake render under a parchment wash (vanilla's own ghost pattern). */
    private static void ghostInto(GuiGraphics g, ItemStack stack, int x, int y) {
        g.renderFakeItem(stack, x, y);
        g.fill(net.minecraft.client.renderer.RenderType.guiGhostRecipeOverlay(),
                x, y, x + 16, y + 16, 0x80C8B892);
    }

    /** Chalk a recipe onto the roll (or wipe it). The 3x3 arrangement comes from the SAME
     *  helper the server lays out with ({@link PackMenu#arrangeOn3x3}), so they cannot drift. */
    private void setGhost(net.minecraft.world.item.crafting.RecipeHolder<?> holder) {
        if (holder == null || ghost != null && holder.id().equals(ghost.id())
                || !(holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe)) {
            ghost = null;   // clicking the chalked recipe again wipes the chalk
            java.util.Arrays.fill(ghostGrid, null);
            return;
        }
        var arranged = PackMenu.arrangeOn3x3(recipe);
        if (arranged == null) {
            ghost = null;
            java.util.Arrays.fill(ghostGrid, null);
            return;
        }
        ghost = holder;
        ghostGrid = arranged;
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

    /**
     * Deep-slot counts, legible: vanilla anchors the count text at the slot's right edge and
     * lets three digits spill left into the NEIGHBOURING cell, where they mash into its count
     * ("64" + "384" read as one smear). For a pack cell holding more than two digits, draw the
     * EXACT number at 3/4 scale instead - "384" fits inside its own 16px cell with room to
     * spare, and the numbers stay exact (no "2.5K" rounding) as preferred.
     */
    @Override
    protected void renderSlotContents(GuiGraphics g, ItemStack stack, Slot slot, String countString) {
        if (slot instanceof PackViewSlot && countString == null && stack.getCount() > 99) {
            super.renderSlotContents(g, stack, slot, "");   // item + durability bar, no count
            String txt = String.valueOf(stack.getCount());
            g.pose().pushPose();
            g.pose().translate(slot.x + 17f, slot.y + 16f, 200f);
            g.pose().scale(0.75f, 0.75f, 1f);
            g.drawString(this.font, txt, -this.font.width(txt), -9, 0xFFFFFF, true);
            g.pose().popPose();
            return;
        }
        super.renderSlotContents(g, stack, slot, countString);
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
        drawGhost(g);
        if (renaming) renameBox.render(g, mouseX, mouseY, partialTick);
        drawButtons(g, mouseX, mouseY);
        drawPageNav(g, mouseX, mouseY);
        drawStoreGauges(g);
        drawBrowser(g, mouseX, mouseY);
        drawRulesSheet(g, mouseX, mouseY);
        drawPinNote(g);
        drawHoverTooltips(g, mouseX, mouseY);
        this.renderTooltip(g, mouseX, mouseY);
    }

    // Title-strip buttons: ledger + tool roll (only with a kit fitted), flatten, tidy up, new tab.
    private int bookBtnX() { return leftPos + 102; }
    private int rollBtnX() { return leftPos + 116; }
    private int flatBtnX() { return leftPos + 130; }
    private int tidyBtnX() { return leftPos + 144; }
    private int newBtnX()  { return leftPos + 158; }
    private int btnY()     { return topPos + 4; }
    private static final int BTN = 12;

    private boolean hasKit() {
        return hasKitCached;
    }

    /** The ledger is on screen (containerTick closes it the moment the roll goes away). */
    private boolean ledgerVisible() {
        return browserOpen && menu.rollActive();
    }

    /** The ledger sheet's hit-box, spine included - the ONE definition every consumer uses. */
    private boolean overLedger(int mx, int my) {
        return ledgerVisible()
                && inRect(mx, my, browserX() - 2, browserY(), BR_W + 2, browserH());
    }

    /** Top-left of the {@code rIdx}-th visible ledger cell (cells are 18x18 with a 1px halo). */
    private int cellX(int rIdx) { return browserX() + 5 + (rIdx % BR_COLS) * 22; }
    private int cellY(int rIdx) { return browserGridY() + (rIdx / BR_COLS) * 18; }

    private int maxLedgerScroll() {
        return Math.max(0, (craftable.size() + BR_COLS - 1) / BR_COLS - browserRows());
    }

    // ---- the Rule Editor sheet ----

    private int rulesX() { return leftPos + PackMenu.TRINKET_X + 26; } // same anchor as the ledger
    private int rulesY() { return topPos + 4; }
    private int rulesH() { return imageHeight - 8; }
    private int rulesListY() { return rulesY() + 40; }
    private int rulesAddTop() { return rulesY() + rulesH() - 90; }
    private int rulesListRows() { return (rulesAddTop() - 4 - rulesListY()) / RULE_ROW_H; }

    /** The editor exists while its gate holds: a Quill &amp; Ledger fitted, a custom tab active. */
    private boolean canEditRules() {
        return !menu.flatten() && isActiveCustom()
                && menu.hasTrinket(com.sappersquad.packwork.trinket.TrinketType.QUILL_LEDGER);
    }

    private boolean rulesVisible() {
        return rulesOpen && canEditRules();
    }

    /** The rule sheet's hit-box, spine included - every consumer uses this one definition. */
    private boolean overRules(int mx, int my) {
        return rulesVisible() && inRect(mx, my, rulesX() - 2, rulesY(), RU_W + 2, rulesH());
    }

    private void openRules() {
        closeBrowser();          // one sheet at a time on the right flank
        rulesOpen = true;
        rulesScroll = 0;
        if (ruleValueBox != null) {
            ruleValueBox.setValue("");
            ruleValueBox.setVisible(true);
        }
        repositionForBrowser();
    }

    private void closeRules() {
        rulesOpen = false;
        rulesScroll = 0;
        if (ruleValueBox != null) ruleValueBox.setVisible(false);
        repositionForBrowser();
    }

    /** The active custom tab's stored definition (authored rules live here, not on the view). */
    private com.sappersquad.packwork.sort.TabDef activeCustomDef() {
        return menu.layout().customTab(menu.activeTab());
    }

    /** One authored rule, in words. */
    private Component ruleText(com.sappersquad.packwork.sort.SortRule r) {
        return switch (r.type()) {
            case NAME -> Component.translatable("packwork.ui.rules_name_row", r.value());
            case MODID -> Component.translatable("packwork.ui.rules_mod_row", r.value());
            case TAG -> Component.translatable("packwork.ui.rules_tag_row", r.value());
            case PREDICATE -> Component.translatable("packwork.ui.rules_kind_row",
                    kindName(r.value()));
        };
    }

    private Component kindName(String predicateName) {
        var kind = com.sappersquad.packwork.sort.PredicateKind.byNameOrNull(predicateName);
        return kind == null ? Component.literal(predicateName)
                : Component.translatable("packwork.kind." + kind.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static final com.sappersquad.packwork.sort.PredicateKind[] CHIP_KINDS = {
            com.sappersquad.packwork.sort.PredicateKind.IS_FOOD,
            com.sappersquad.packwork.sort.PredicateKind.IS_TOOL,
            com.sappersquad.packwork.sort.PredicateKind.IS_WEAPON,
            com.sappersquad.packwork.sort.PredicateKind.IS_ARMOR,
            com.sappersquad.packwork.sort.PredicateKind.IS_BLOCK,
            com.sappersquad.packwork.sort.PredicateKind.IS_POTION,
    };

    /** Top-left of a category chip (two per row, three rows, under the Name/Mod buttons). */
    private int[] chipRect(int i) {
        int w = (RU_W - 12) / 2;
        int x = rulesX() + 4 + (i % 2) * (w + 4);
        int y = rulesAddTop() + 42 + (i / 2) * 12;
        return new int[]{x, y, w, 10};
    }

    /** The index of a stored predicate rule for this kind, or -1. */
    private static int predicateRuleIndex(com.sappersquad.packwork.sort.TabDef def,
                                          com.sappersquad.packwork.sort.PredicateKind kind) {
        for (int i = 0; i < def.rules().size(); i++) {
            var r = def.rules().get(i);
            if (r.type() == com.sappersquad.packwork.sort.SortRule.Type.PREDICATE
                    && kind.name().equalsIgnoreCase(r.value())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The parchment rule sheet: what this compartment gathers and why. The stamp line is
     * the always-on baseline; the list below is the authored rules the Quill &amp; Ledger
     * unlocks; the controls at the bottom write new ones.
     */
    private void drawRulesSheet(GuiGraphics g, int mouseX, int mouseY) {
        if (!rulesVisible()) return;
        var def = activeCustomDef();
        if (def == null) return;
        int x = rulesX(), y = rulesY(), w = RU_W, h = rulesH();

        // parchment sheet with a leather spine toward the pack and brass tacks (ledger chrome)
        g.fill(x - 2, y, x + w, y + h, 0xFF3E2A18);
        g.fill(x, y + 1, x + w - 1, y + h - 1, 0xFFC8B892);
        g.fill(x, y + 1, x + w - 1, y + 2, 0xFFE2D6AE);
        g.fill(x, y + h - 2, x + w - 1, y + h - 1, 0xFFA89A74);
        g.renderOutline(x - 2, y, w + 2, h, 0xFFC9A24B);
        for (int[] t : new int[][]{{x + 2, y + 3}, {x + w - 5, y + 3}, {x + 2, y + h - 5}, {x + w - 5, y + h - 5}}) {
            g.fill(t[0], t[1], t[0] + 2, t[1] + 2, 0xFF8A6A28);
        }
        g.drawString(this.font, Component.translatable("packwork.ui.rules_title"),
                x + 5, y + 5, 0xFF3A2A18, false);
        String tabName = this.font.plainSubstrByWidth(menu.tabName(def.id()).getString(), w - 10);
        g.drawString(this.font, tabName, x + 5, y + 16, 0xFF6A4A2A, false);

        // the stamp baseline - always on, trinket or no
        var stampRule = com.sappersquad.packwork.sort.SortEngine.iconRule(def.icon());
        Component stampLine = stampRule == null
                ? Component.translatable("packwork.ui.rules_stamp_none")
                : Component.translatable("packwork.ui.rules_stamp", kindName(stampRule.value()));
        g.drawString(this.font, this.font.plainSubstrByWidth(stampLine.getString(), w - 10),
                x + 5, y + 28, 0xFF3A2A18, false);
        if (inRect(mouseX, mouseY, x + 3, y + 26, w - 6, 11)) {
            g.renderTooltip(this.font, Component.translatable("packwork.ui.rules_stamp_hint"),
                    mouseX, mouseY);
        }

        // authored rules, one per row, each with a strike-off box
        int rows = rulesListRows();
        rulesScroll = Math.max(0, Math.min(rulesScroll, Math.max(0, def.rules().size() - rows)));
        for (int v = 0; v < rows; v++) {
            int i = rulesScroll + v;
            if (i >= def.rules().size()) break;
            int ry = rulesListY() + v * RULE_ROW_H;
            var rule = def.rules().get(i);
            String line = this.font.plainSubstrByWidth(ruleText(rule).getString(), w - 24);
            g.drawString(this.font, line, x + 5, ry + 1, 0xFF3A2A18, false);
            int bx = x + w - 14;
            boolean hov = inRect(mouseX, mouseY, bx, ry, 9, 9);
            g.fill(bx, ry, bx + 9, ry + 9, hov ? 0xFF8A3A2A : 0xFF6B4A2F);
            g.renderOutline(bx, ry, 9, 9, hov ? 0xFFE7CC82 : 0xFFC9A24B);
            g.fill(bx + 2, ry + 4, bx + 7, ry + 5, 0xFFEAD9A6); // the strike
            if (hov) {
                g.renderTooltip(this.font, Component.translatable("packwork.ui.rules_remove"),
                        mouseX, mouseY);
            }
        }
        if (def.rules().isEmpty()) {
            g.drawWordWrap(this.font, Component.translatable("packwork.ui.rules_empty"),
                    x + 5, rulesListY() + 2, w - 10, 0xFF6A5A40);
        }
        if (def.rules().size() > rows) { // a thin brass track when there's more than fits
            int trackY = rulesListY(), trackH = rows * RULE_ROW_H - 2;
            int maxScroll = def.rules().size() - rows;
            g.fill(x + w - 4, trackY, x + w - 3, trackY + trackH, 0xFF8A6A28);
            int nub = trackY + (int) ((trackH - 8) * (rulesScroll / (double) maxScroll));
            g.fill(x + w - 5, nub, x + w - 2, nub + 8, 0xFFC9A24B);
        }

        // the writing desk: a value, two ways to file it, and the category chips
        int at = rulesAddTop();
        g.drawString(this.font, Component.translatable("packwork.ui.rules_add"),
                x + 5, at, 0xFF6A4A2A, false);
        ruleValueBox.render(g, mouseX, mouseY, 0);
        int bw = (RU_W - 14) / 2;
        drawTextButton(g, x + 4, at + 26, bw, Component.translatable("packwork.ui.rules_add_name"),
                inRect(mouseX, mouseY, x + 4, at + 26, bw, 12), false);
        drawTextButton(g, x + 8 + bw, at + 26, bw, Component.translatable("packwork.ui.rules_add_mod"),
                inRect(mouseX, mouseY, x + 8 + bw, at + 26, bw, 12), false);
        for (int i = 0; i < CHIP_KINDS.length; i++) {
            int[] r = chipRect(i);
            boolean on = predicateRuleIndex(def, CHIP_KINDS[i]) >= 0;
            drawTextButton(g, r[0], r[1], r[2],
                    Component.translatable("packwork.kind." + CHIP_KINDS[i].name().toLowerCase(java.util.Locale.ROOT)),
                    inRect(mouseX, mouseY, r[0], r[1], r[2], r[3]), on);
        }
        g.drawString(this.font, Component.translatable("packwork.ui.rules_footer"),
                x + 5, y + h - 12, 0xFF6A5A40, false);
    }

    /** A small labelled brass-edged plate (chips and the Name/Mod buttons). */
    private void drawTextButton(GuiGraphics g, int x, int y, int w, Component label,
                                boolean hover, boolean on) {
        int base = on ? 0xFF8A6A28 : 0xFF6B4A2F;
        g.fill(x, y, x + w, y + 10, base);
        g.renderOutline(x, y, w, 10, hover ? 0xFFE7CC82 : 0xFFC9A24B);
        String s = this.font.plainSubstrByWidth(label.getString(), w - 4);
        g.drawString(this.font, s, x + (w - this.font.width(s)) / 2, y + 1, 0xFFEAD9A6, false);
    }

    /** Clicks inside the rule sheet: focus the box, add or strike rules, or just be swallowed. */
    private boolean handleRulesClick(double mx, double my, int button) {
        if (!overRules((int) mx, (int) my)) return false;
        if (ruleValueBox.mouseClicked(mx, my, button)) {
            setFocused(ruleValueBox);
            return true;
        }
        var def = activeCustomDef();
        if (def == null || button != 0) return true;
        int x = rulesX(), w = RU_W;

        // strike a rule off
        int rows = rulesListRows();
        for (int v = 0; v < rows; v++) {
            int i = rulesScroll + v;
            if (i >= def.rules().size()) break;
            int ry = rulesListY() + v * RULE_ROW_H;
            if (inRect((int) mx, (int) my, x + w - 14, ry, 9, 9)) {
                PackClientActions.removeTabRule(menu, def.id(), i);
                return true;
            }
        }

        // file the typed value as a name or mod rule
        int at = rulesAddTop();
        int bw = (RU_W - 14) / 2;
        String typed = ruleValueBox.getValue().trim().toLowerCase(java.util.Locale.ROOT);
        if (inRect((int) mx, (int) my, x + 4, at + 26, bw, 12)) {
            if (!typed.isEmpty()) {
                PackClientActions.addTabRule(menu, def.id(),
                        com.sappersquad.packwork.sort.SortRule.Type.NAME.ordinal(), typed);
                ruleValueBox.setValue("");
            }
            return true;
        }
        if (inRect((int) mx, (int) my, x + 8 + bw, at + 26, bw, 12)) {
            if (!typed.isEmpty()) {
                PackClientActions.addTabRule(menu, def.id(),
                        com.sappersquad.packwork.sort.SortRule.Type.MODID.ordinal(), typed);
                ruleValueBox.setValue("");
            }
            return true;
        }

        // toggle a category chip
        for (int i = 0; i < CHIP_KINDS.length; i++) {
            int[] r = chipRect(i);
            if (inRect((int) mx, (int) my, r[0], r[1], r[2], r[3])) {
                int existing = predicateRuleIndex(def, CHIP_KINDS[i]);
                if (existing >= 0) {
                    PackClientActions.removeTabRule(menu, def.id(), existing);
                } else {
                    PackClientActions.addTabRule(menu, def.id(),
                            com.sappersquad.packwork.sort.SortRule.Type.PREDICATE.ordinal(),
                            CHIP_KINDS[i].name());
                }
                return true;
            }
        }
        return true; // anywhere else on the sheet: consumed, never vanilla's drop-outside
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
        if (hasKit() && menu.rollActive()) {  // the ledger only means anything with the roll out
            drawPlate(g, bookBtnX(), btnY(), inRect(mouseX, mouseY, bookBtnX(), btnY(), BTN, BTN), browserOpen);
            // a little open ledger: two pages + the spine
            g.fill(bookBtnX() + 2, btnY() + 3, bookBtnX() + 10, btnY() + 9, gl);
            g.fill(bookBtnX() + 5, btnY() + 2, bookBtnX() + 7, btnY() + 10, 0xFF8A6A28);
            g.fill(bookBtnX() + 3, btnY() + 5, bookBtnX() + 5, btnY() + 6, 0xFF8A6A28);
            g.fill(bookBtnX() + 7, btnY() + 5, bookBtnX() + 9, btnY() + 6, 0xFF8A6A28);
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

        // per-compartment controls sit under the grid, by the page nav. First: the
        // arrangement switch - does the pack tidy this compartment, or do you keep your
        // own layout? Shown for every tab (hidden only while the tool roll covers the row).
        if (!menu.flatten() && !menu.rollActive()) {
            int mx2 = modeBtnX(), my2 = perTabBtnY();
            boolean kept = menu.activeTabManual();
            drawPlate(g, mx2, my2, inRect(mouseX, mouseY, mx2, my2, BTN, BTN), kept);
            if (kept) {
                // your own grid: four laid-out dots and a brass tack holding them
                g.fill(mx2 + 3, my2 + 3, mx2 + 5, my2 + 5, gl);
                g.fill(mx2 + 7, my2 + 3, mx2 + 9, my2 + 5, gl);
                g.fill(mx2 + 3, my2 + 7, mx2 + 5, my2 + 9, gl);
                g.fill(mx2 + 7, my2 + 7, mx2 + 9, my2 + 9, gl);
                g.fill(mx2 + 8, my2 + 2, mx2 + 10, my2 + 4, 0xFFE7CC82); // the tack
            } else {
                // the pack tidies: three bars falling into line
                g.fill(mx2 + 2, my2 + 3, mx2 + 10, my2 + 4, gl);
                g.fill(mx2 + 3, my2 + 6, mx2 + 9, my2 + 7, gl);
                g.fill(mx2 + 4, my2 + 9, mx2 + 8, my2 + 10, gl);
            }
        }

        // the quill (edit this compartment's rules) sits under the grid, by the page nav -
        // it appears exactly when it means something: ledger fitted, custom tab showing
        if (canEditRules() && !menu.rollActive()) {
            int qx = quillBtnX(), qy = perTabBtnY();
            drawPlate(g, qx, qy, inRect(mouseX, mouseY, qx, qy, BTN, BTN), rulesOpen);
            for (int i = 0; i < 6; i++) {                      // feather shaft, nib to tip
                g.fill(qx + 2 + i, qy + 8 - i, qx + 3 + i, qy + 9 - i, gl);
            }
            g.fill(qx + 5, qy + 3, qx + 9, qy + 4, gl);        // barbs
            g.fill(qx + 6, qy + 5, qx + 9, qy + 6, gl);
            g.fill(qx + 2, qy + 9, qx + 3, qy + 10, 0xFF3A2A18); // ink nib
        }
    }

    private int modeBtnX() { return leftPos + 124; }
    private int quillBtnX() { return leftPos + 138; }
    private int perTabBtnY() { return topPos + 142; }

    /** Glass store gauges under the trinket sockets: a waterskin, then a soul vial. */
    private void drawStoreGauges(GuiGraphics g) {
        gaugeRect = null;
        xpGaugeRect = null;
        energyGaugeRect = null;
        flaskGaugeRect = null;
        int x = leftPos + PackMenu.TRINKET_X - 1;
        int y = topPos + gaugeTopY();
        int w = GAUGE_W, h = gaugeHeight();

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

    /**
     * Per-gauge height, shrunk when the rail is crowded: a Dragonhide pack's five sockets
     * plus several store gauges must still fit beside the panel rather than hanging past
     * its bottom edge. Full 40px whenever there's room.
     */
    private int gaugeHeight() {
        int n = Math.max(1, gaugeCount());
        int avail = imageHeight - gaugeTopY() - 4 - (n - 1) * GAUGE_GAP;
        return Math.max(22, Math.min(GAUGE_H, avail / n));
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
        if (hasKit() && menu.rollActive() && inRect(mouseX, mouseY, bookBtnX(), btnY(), BTN, BTN)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("packwork.ui.ledger_btn"));
            lines.add(Component.translatable("packwork.ui.ledger_btn_hint")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        } else if (hasKit() && inRect(mouseX, mouseY, rollBtnX(), btnY(), BTN, BTN)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(menu.rollActive()
                    ? "packwork.ui.roll_up" : "packwork.ui.unroll"));
            lines.add(Component.translatable("packwork.ui.roll_hint")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        } else if (!menu.flatten() && !menu.rollActive()
                && inRect(mouseX, mouseY, modeBtnX(), perTabBtnY(), BTN, BTN)) {
            boolean kept = menu.activeTabManual();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(kept ? "packwork.ui.mode_btn_keep" : "packwork.ui.mode_btn_tidy"));
            lines.add(Component.translatable(kept ? "packwork.ui.mode_btn_keep_hint" : "packwork.ui.mode_btn_tidy_hint")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        } else if (canEditRules() && !menu.rollActive()
                && inRect(mouseX, mouseY, quillBtnX(), perTabBtnY(), BTN, BTN)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("packwork.ui.rules_btn"));
            lines.add(Component.translatable("packwork.ui.rules_btn_hint")
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
        // the Recipe Ledger and the rule sheet each swallow every click inside their sheet
        if (ledgerVisible() && handleBrowserClick(mx, my, button)) {
            return true;
        }
        if (rulesVisible() && handleRulesClick(mx, my, button)) {
            return true;
        }
        // title buttons
        if (button == 0) {
            if (!menu.flatten() && !menu.rollActive()
                    && inRect((int) mx, (int) my, modeBtnX(), perTabBtnY(), BTN, BTN)) {
                PackClientActions.toggleTabMode(menu, menu.activeTab());
                return true;
            }
            if (canEditRules() && !menu.rollActive()
                    && inRect((int) mx, (int) my, quillBtnX(), perTabBtnY(), BTN, BTN)) {
                if (rulesOpen) closeRules();
                else openRules();
                return true;
            }
            if (hasKit() && inRect((int) mx, (int) my, rollBtnX(), btnY(), BTN, BTN)) {
                PackClientActions.toggleRoll(menu);
                return true;
            }
            if (hasKit() && menu.rollActive() && inRect((int) mx, (int) my, bookBtnX(), btnY(), BTN, BTN)) {
                if (browserOpen) {
                    closeBrowser();
                } else {
                    closeRules();     // one sheet at a time on the right flank
                    browserOpen = true;
                    browserSearch.setVisible(true);
                    repositionForBrowser();
                    recomputeCraftable();
                }
                return true;
            }
            // a chalked recipe + a click on the empty result well = lay it out from stock
            if (ghost != null && menu.rollActive()
                    && inRect((int) mx, (int) my, leftPos + PackMenu.ROLL_RESULT_X - 2,
                            topPos + PackMenu.ROLL_RESULT_Y - 2, 20, 20)
                    && !menu.slots.get(menu.resultIndex()).hasItem()) {
                PackClientActions.layOutGhost(menu, ghost.id().toString());
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

    /** Everything the pack draws beyond the panel edges: the tab rail left, the fittings rail
     *  right, and the Recipe Ledger sheet beyond it. All count as INSIDE the GUI. */
    private boolean isOverRail(int mx, int my) {
        // left: stamped leather tabs (they tuck under the frame, so start a touch further out)
        if (mx >= leftPos - TAB_W && mx < leftPos
                && my >= topPos + RAIL_TOP && my < topPos + imageHeight) return true;
        // whichever sheet is open on the right flank
        if (overLedger(mx, my) || overRules(mx, my)) return true;
        // right: brass sockets, then the stack of store gauges beneath them
        int bottom = gaugeTopY() + gaugeCount() * (gaugeHeight() + GAUGE_GAP);
        return mx >= leftPos + PackMenu.TRINKET_X - 5 && mx < leftPos + PackMenu.TRINKET_X + 24
                && my >= topPos + PackMenu.TRINKET_Y0 - 5 && my < topPos + bottom;
    }

    /** Clicks inside the ledger sheet: focus the search, chalk a recipe, or just be swallowed. */
    private boolean handleBrowserClick(double mx, double my, int button) {
        if (!overLedger((int) mx, (int) my)) return false;
        if (browserSearch.mouseClicked(mx, my, button)) {
            setFocused(browserSearch);
            return true;
        }
        if (button == 0) {
            for (int rIdx = 0; rIdx < browserRows() * BR_COLS; rIdx++) {
                int i = (browserScroll * BR_COLS) + rIdx;
                if (i >= craftable.size()) break;
                if (inRect((int) mx, (int) my, cellX(rIdx) - 1, cellY(rIdx) - 1, 18, 18)) {
                    setGhost(craftable.get(i).holder());
                    return true;
                }
            }
        }
        return true; // anywhere else on the sheet: consumed, never vanilla's drop-outside
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (overLedger((int) mx, (int) my)) {
            browserScroll = Math.max(0, Math.min(maxLedgerScroll(), browserScroll - (int) Math.signum(dy)));
            return true;
        }
        if (overRules((int) mx, (int) my)) {
            rulesScroll = Math.max(0, rulesScroll - (int) Math.signum(dy)); // draw clamps the top end
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
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
        if (ruleValueBox != null && ruleValueBox.isFocused()) {
            return super.keyPressed(key, scan, mods);
        }
        Slot hovered = this.hoveredSlot;
        boolean overGrid = hovered instanceof PackViewSlot && hovered.hasItem();

        // Pin/unpin the hovered item to the active tab. Rebindable keybind (default P), so it
        // reads the mapping rather than a hardcoded key and shows up in vanilla Controls.
        if (PackKeyMappings.PIN.matches(key, scan) && overGrid && !menu.flatten()) {
            ItemStack held = hovered.getItem();
            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(held.getItem());
            String pinned = menu.layout().pinnedTab(itemKey);
            if (menu.activeTab().equals(pinned)) {
                PackClientActions.unpin(menu, itemKey.toString());
                showPinNote(Component.translatable("packwork.ui.unpinned_note", held.getHoverName()));
            } else {
                PackClientActions.pin(menu, menu.activeTab(), itemKey.toString());
                showPinNote(Component.translatable("packwork.ui.pinned_note",
                        held.getHoverName(), menu.tabName(menu.activeTab())));
            }
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

    /** Dev harness only: the Recipe Ledger button's centre (null unless the roll is out). */
    public int[] devLedgerButtonCenter() {
        return hasKit() && menu.rollActive() ? new int[]{bookBtnX() + BTN / 2, btnY() + BTN / 2} : null;
    }

    /** Dev harness only: the centre of a recipe's cell on the open ledger, or null if not visible. */
    public int[] devLedgerCellCenter(String recipeId) {
        if (!browserOpen) return null;
        for (int rIdx = 0; rIdx < browserRows() * BR_COLS; rIdx++) {
            int i = (browserScroll * BR_COLS) + rIdx;
            if (i >= craftable.size()) break;
            if (craftable.get(i).holder().id().toString().equals(recipeId)) {
                return new int[]{cellX(rIdx) + 8, cellY(rIdx) + 8};
            }
        }
        return null;
    }

    /** Dev harness only: the tool roll's result well centre. */
    public int[] devResultWellCenter() {
        Slot well = menu.slots.get(menu.resultIndex());
        return new int[]{leftPos + well.x + 8, topPos + well.y + 8};
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
