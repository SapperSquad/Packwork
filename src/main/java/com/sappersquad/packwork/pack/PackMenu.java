package com.sappersquad.packwork.pack;

import com.sappersquad.packwork.net.PackAction;
import com.sappersquad.packwork.reg.ModComponents;
import com.sappersquad.packwork.reg.ModMenus;
import com.sappersquad.packwork.sort.AutoTabs;
import com.sappersquad.packwork.sort.PackLayout;
import com.sappersquad.packwork.sort.SortEngine;
import com.sappersquad.packwork.sort.TabView;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The organizer menu. Holds one flat backing inventory (the pack) plus a fixed
 * grid of {@link PackViewSlot}s that rebind to whichever backing slots the active
 * tab / search / page selects. Server-authoritative; the client mirrors the same
 * view state so a click on a grid cell resolves to the same backing slot on both
 * sides.
 */
public class PackMenu extends AbstractContainerMenu {

    // --- GUI geometry (shared with the screen) ---
    public static final int GRID_X = 8;
    public static final int GRID_Y = 34;
    public static final int PLAYER_X = 8;
    public static final int PLAYER_INV_Y = 158;
    public static final int HOTBAR_Y = 216;
    public static final int IMAGE_W = 176;
    public static final int IMAGE_H = 240;

    private static final int VIEW_SLOTS = PackTier.VIEW_SLOTS;

    private final Inventory playerInv;
    private final int boundSlot;
    private final ItemStack packStack;
    private final PackTier tier;
    private final PackInventory packInv;
    private final List<PackViewSlot> viewSlots = new ArrayList<>();

    private PackLayout layout;
    private List<TabView> tabs;

    // View state (menu-only; never persisted on the item to avoid churn).
    private String activeTab;
    private String search = "";
    private boolean flatten = true; // Phase 0 default: one grid. Phase 1 flips this off.
    private int page = 0;
    private int pageCount = 1;

    // ---- factories ----

    public static PackMenu server(int id, Inventory playerInv, int boundSlot) {
        return new PackMenu(id, playerInv, boundSlot);
    }

    public static PackMenu client(int id, Inventory playerInv, int boundSlot) {
        return new PackMenu(id, playerInv, boundSlot);
    }

    private PackMenu(int id, Inventory playerInv, int boundSlot) {
        super(ModMenus.PACK.get(), id);
        this.playerInv = playerInv;
        this.boundSlot = boundSlot;
        this.packStack = playerInv.getItem(boundSlot);
        this.tier = PackItem.tierOf(packStack);
        this.packInv = new PackInventory(packStack, tier);
        this.layout = packStack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        this.tabs = SortEngine.tabsFor(layout);
        this.activeTab = firstRealTab();

        // Grid of view slots (indices 0 .. VIEW_SLOTS-1).
        for (int row = 0; row < PackTier.VIEW_ROWS; row++) {
            for (int col = 0; col < PackTier.VIEW_COLS; col++) {
                PackViewSlot s = new PackViewSlot(packInv,
                        GRID_X + col * 18, GRID_Y + row * 18);
                viewSlots.add(s);
                addSlot(s);
            }
        }

        addPlayerInventory(playerInv);
        rebuildView();
    }

    private String firstRealTab() {
        for (TabView t : tabs) {
            if (!t.loose()) return t.id();
        }
        return AutoTabs.LOOSE_ID;
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                addSlot(makePlayerSlot(inv, index, PLAYER_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(makePlayerSlot(inv, col, PLAYER_X + col * 18, HOTBAR_Y));
        }
    }

    /** Lock the slot that holds the open pack so it cannot be moved out from under us. */
    private Slot makePlayerSlot(Inventory inv, int index, int x, int y) {
        if (index == boundSlot) {
            return new Slot(inv, index, x, y) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            };
        }
        return new Slot(inv, index, x, y);
    }

    // ---- view rebuild ----

    /** Recompute which backing slots each grid cell shows. Runs identically on both sides. */
    public void rebuildView() {
        // Re-read the durable layout from the (synced) stack so both sides stay current.
        this.layout = packStack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        this.tabs = SortEngine.tabsFor(layout);
        List<Integer> order = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT).trim();
        boolean searching = !q.isEmpty();

        if (flatten) {
            for (int i = 0; i < packInv.getSlots(); i++) {
                ItemStack s = packInv.getStackInSlot(i);
                if (searching && (s.isEmpty() || !matchesSearch(s, q))) continue;
                order.add(i);
            }
            if (!searching) {
                // include trailing empties so the grid stays a full droppable surface
                // (already added all indices above only when not searching)
            }
        } else {
            // tab view: matching non-empty first, then empties to drop into
            List<Integer> empties = new ArrayList<>();
            for (int i = 0; i < packInv.getSlots(); i++) {
                ItemStack s = packInv.getStackInSlot(i);
                if (s.isEmpty()) {
                    empties.add(i);
                    continue;
                }
                if (searching && !matchesSearch(s, q)) continue;
                String route = SortEngine.route(s, tabs, layout);
                if (route.equals(activeTab)) order.add(i);
            }
            if (!searching) {
                order.addAll(empties);
            }
        }

        this.pageCount = Math.max(1, (order.size() + VIEW_SLOTS - 1) / VIEW_SLOTS);
        if (page >= pageCount) page = pageCount - 1;
        if (page < 0) page = 0;

        int start = page * VIEW_SLOTS;
        for (int p = 0; p < VIEW_SLOTS; p++) {
            int gi = start + p;
            if (gi < order.size()) {
                viewSlots.get(p).bind(order.get(gi), true);
            } else {
                viewSlots.get(p).bind(-1, false);
            }
        }
    }

    private static boolean matchesSearch(ItemStack stack, String q) {
        if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) return true;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.toString().toLowerCase(Locale.ROOT).contains(q);
    }

    @Override
    public void broadcastChanges() {
        rebuildView();
        super.broadcastChanges();
    }

    // ---- shift-click ----

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();

        if (index < VIEW_SLOTS) {
            // pack -> player inventory
            if (!moveItemStackTo(inSlot, VIEW_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
            slot.setChanged();
        } else {
            // player -> pack backing store, auto-routed into the flat inventory
            ItemStack leftover = insertIntoPack(inSlot.copy());
            int moved = inSlot.getCount() - leftover.getCount();
            if (moved <= 0) return ItemStack.EMPTY;
            inSlot.shrink(moved);
        }

        if (inSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        rebuildView();
        return original;
    }

    /** Insert into the whole backing store: merge into existing stacks first, then fill empties. */
    private ItemStack insertIntoPack(ItemStack stack) {
        ItemStack remaining = stack;
        for (int i = 0; i < packInv.getSlots() && !remaining.isEmpty(); i++) {
            if (!packInv.getStackInSlot(i).isEmpty()) {
                remaining = packInv.insertItem(i, remaining, false);
            }
        }
        for (int i = 0; i < packInv.getSlots() && !remaining.isEmpty(); i++) {
            if (packInv.getStackInSlot(i).isEmpty()) {
                remaining = packInv.insertItem(i, remaining, false);
            }
        }
        return remaining;
    }

    // ---- accessors for screen / actions ----

    public List<TabView> tabs() {
        return tabs;
    }

    public String activeTab() {
        return activeTab;
    }

    public String search() {
        return search;
    }

    public boolean flatten() {
        return flatten;
    }

    public int page() {
        return page;
    }

    public int pageCount() {
        return pageCount;
    }

    public PackTier tier() {
        return tier;
    }

    public PackLayout layout() {
        return layout;
    }

    // ---- actions (server dispatch + shared apply logic used optimistically on the client) ----

    public void handleAction(int actionId, int arg, String s1, String s2) {
        PackAction a = PackAction.byId(actionId);
        if (a == null) return;
        switch (a) {
            case SELECT_TAB -> applySelectTab(s1);
            case SET_SEARCH -> applySearch(s1);
            case TOGGLE_FLATTEN -> applyFlatten(!flatten);
            case PAGE -> applyPage(page + arg);
            case TIDY_UP -> applyTidyUp();
            case CREATE_TAB -> applyCreateTab();
            case DELETE_TAB -> applyDeleteTab(s1);
            case RENAME_TAB -> applyRenameTab(s1, s2);
            case MOVE_TAB -> applyMoveTab(s1, arg);
            case SET_TAB_COLOR -> applyTabColor(s1, arg);
            case SET_TAB_ICON -> applyTabIcon(s1, s2);
            case PIN_ITEM -> applyPin(s1, s2);
            case UNPIN_ITEM -> applyUnpin(s2);
        }
    }

    public void applySelectTab(String id) {
        this.activeTab = id;
        this.flatten = false;
        this.page = 0;
        rebuildView();
    }

    public void applySearch(String q) {
        this.search = q == null ? "" : q;
        this.page = 0;
        rebuildView();
    }

    public void applyFlatten(boolean f) {
        this.flatten = f;
        this.page = 0;
        rebuildView();
    }

    public void applyPage(int p) {
        this.page = p;
        rebuildView();
    }

    /** Merge partial stacks, compact to the front, and order by tab then item id. */
    public void applyTidyUp() {
        List<ItemStack> merged = new ArrayList<>();
        for (int i = 0; i < packInv.getSlots(); i++) {
            ItemStack s = packInv.getStackInSlot(i);
            if (s.isEmpty()) continue;
            s = s.copy();
            for (ItemStack m : merged) {
                if (s.isEmpty()) break;
                if (ItemStack.isSameItemSameComponents(m, s)) {
                    int space = m.getMaxStackSize() - m.getCount();
                    if (space > 0) {
                        int move = Math.min(space, s.getCount());
                        m.grow(move);
                        s.shrink(move);
                    }
                }
            }
            if (!s.isEmpty()) merged.add(s);
        }
        List<TabView> t = SortEngine.tabsFor(layout);
        java.util.Map<String, Integer> tabIndex = new java.util.HashMap<>();
        for (int i = 0; i < t.size(); i++) tabIndex.put(t.get(i).id(), i);
        merged.sort(java.util.Comparator
                .comparingInt((ItemStack s) -> tabIndex.getOrDefault(SortEngine.route(s, t, layout), 999))
                .thenComparing(s -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
        for (int i = 0; i < packInv.getSlots(); i++) {
            packInv.setStackInSlot(i, i < merged.size() ? merged.get(i) : ItemStack.EMPTY);
        }
        rebuildView();
    }

    public void applyCreateTab() {
        PackLayout cur = currentLayout();
        String id = cur.nextCustomId();
        com.sappersquad.packwork.sort.TabDef def = new com.sappersquad.packwork.sort.TabDef(
                id, "New Tab", net.minecraft.resources.ResourceLocation.withDefaultNamespace("leather"),
                0, List.of());
        List<com.sappersquad.packwork.sort.TabDef> customs = new ArrayList<>(cur.customTabs());
        customs.add(def);
        List<String> order = ensureOrder(cur);
        order.add(id);
        saveLayout(new PackLayout(order, customs, cur.pins()));
        this.activeTab = id;
        this.flatten = false;
        rebuildView();
    }

    public void applyDeleteTab(String id) {
        if (!id.startsWith("custom:")) return;
        PackLayout cur = currentLayout();
        List<com.sappersquad.packwork.sort.TabDef> customs = new ArrayList<>();
        for (var td : cur.customTabs()) if (!td.id().equals(id)) customs.add(td);
        List<String> order = ensureOrder(cur);
        order.remove(id);
        List<PackLayout.Pin> pins = new ArrayList<>();
        for (var p : cur.pins()) if (!p.tabId().equals(id)) pins.add(p);
        saveLayout(new PackLayout(order, customs, pins));
        if (activeTab.equals(id)) activeTab = firstRealTab();
        rebuildView();
    }

    public void applyRenameTab(String id, String name) {
        mutateCustom(id, td -> td.withName(name));
    }

    public void applyTabColor(String id, int color) {
        mutateCustom(id, td -> td.withColor(color));
    }

    public void applyTabIcon(String id, String iconId) {
        net.minecraft.resources.ResourceLocation r = net.minecraft.resources.ResourceLocation.tryParse(iconId);
        if (r != null) mutateCustom(id, td -> td.withIcon(r));
    }

    public void applyMoveTab(String id, int delta) {
        PackLayout cur = currentLayout();
        List<String> order = ensureOrder(cur);
        int i = order.indexOf(id);
        if (i < 0) return;
        int j = Math.max(0, Math.min(order.size() - 1, i + delta));
        if (i == j) return;
        order.remove(i);
        order.add(j, id);
        saveLayout(cur.withTabOrder(order));
    }

    public void applyPin(String tabId, String itemId) {
        net.minecraft.resources.ResourceLocation r = net.minecraft.resources.ResourceLocation.tryParse(itemId);
        if (r == null) return;
        PackLayout cur = currentLayout();
        List<PackLayout.Pin> pins = new ArrayList<>();
        for (var p : cur.pins()) if (!p.item().equals(r)) pins.add(p);
        pins.add(new PackLayout.Pin(r, tabId));
        saveLayout(cur.withPins(pins));
    }

    public void applyUnpin(String itemId) {
        net.minecraft.resources.ResourceLocation r = net.minecraft.resources.ResourceLocation.tryParse(itemId);
        if (r == null) return;
        PackLayout cur = currentLayout();
        List<PackLayout.Pin> pins = new ArrayList<>();
        for (var p : cur.pins()) if (!p.item().equals(r)) pins.add(p);
        saveLayout(cur.withPins(pins));
    }

    private void mutateCustom(String id, java.util.function.UnaryOperator<com.sappersquad.packwork.sort.TabDef> op) {
        PackLayout cur = currentLayout();
        List<com.sappersquad.packwork.sort.TabDef> customs = new ArrayList<>();
        boolean changed = false;
        for (var td : cur.customTabs()) {
            if (td.id().equals(id)) {
                customs.add(op.apply(td));
                changed = true;
            } else {
                customs.add(td);
            }
        }
        if (changed) saveLayout(cur.withCustomTabs(customs));
    }

    private PackLayout currentLayout() {
        return packStack.getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
    }

    private List<String> ensureOrder(PackLayout cur) {
        if (!cur.tabOrder().isEmpty()) return cur.mutableTabOrder();
        List<String> o = new ArrayList<>(AutoTabs.defaultOrder());
        for (var td : cur.customTabs()) o.add(td.id());
        return o;
    }

    private void saveLayout(PackLayout newLayout) {
        packStack.set(ModComponents.PACK_LAYOUT.get(), newLayout);
        this.layout = newLayout;
        rebuildView();
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack at = playerInv.getItem(boundSlot);
        return at == packStack && at.getItem() instanceof PackItem;
    }
}
