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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

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
    // right-side trinket rail (sockets protrude past the panel's right edge)
    public static final int TRINKET_X = IMAGE_W + 4;
    public static final int TRINKET_Y0 = 26;
    public static final int TRINKET_PITCH = 20;

    // ---- the Tinker's Kit tool roll: unrolls across the pack's bottom three rows ----
    /** Grid rows the unrolled tool roll covers (the pack keeps the rows above it). */
    public static final int ROLL_ROWS = 3;
    /** View slots the roll hides while it's unrolled. */
    public static final int ROLL_HIDES = PackTier.VIEW_COLS * ROLL_ROWS;
    public static final int ROLL_Y = GRID_Y + (PackTier.VIEW_ROWS - ROLL_ROWS) * 18; // 88
    public static final int ROLL_GRID_X = 30;
    public static final int ROLL_RESULT_X = 122;
    public static final int ROLL_RESULT_Y = ROLL_Y + 18;

    private static final int VIEW_SLOTS = PackTier.VIEW_SLOTS;
    private static final int PLAYER_SLOTS = 36;

    private final Inventory playerInv;
    private final int boundSlot;   // player-inventory slot for a carried pack, or -1 for a placed one
    private final PackTier tier;
    private final PackInventory packInv;
    private final PackTrinketInventory trinketInv;
    private final List<PackViewSlot> viewSlots = new ArrayList<>();
    private final int trinketStart; // menu index where trinket slots begin
    private final int trinketEnd;   // one past the last trinket slot

    // Tinker's Kit. The slots exist on EVERY pack menu (client and server must agree on the
    // slot count before the trinket component has synced); they simply go inactive - and refuse
    // every interaction - unless the kit is fitted and the roll is unrolled.
    private final net.minecraft.world.inventory.CraftingContainer craftSlots =
            new net.minecraft.world.inventory.TransientCraftingContainer(this, 3, 3);
    private final net.minecraft.world.inventory.ResultContainer resultSlots =
            new net.minecraft.world.inventory.ResultContainer();
    private final int craftStart;   // menu index where the 3x3 begins
    private final int resultIndex;  // the roll's result slot
    private final int hostIndex;    // the hidden placed-pack host slot, or -1
    private boolean rollOpen = false;

    // Where the live pack stack comes from: a player-inventory slot (carried) or a
    // block-entity via a hidden synced host slot (placed). Both stay server-authoritative.
    private final Supplier<ItemStack> liveSupplier;
    private final PackStackSlotContainer hostContainer; // null for a carried pack
    private final ContainerLevelAccess access;          // NULL for a carried pack

    private PackLayout layout;
    private List<TabView> tabs;

    // View state (menu-only; never persisted on the item to avoid churn).
    private String activeTab;
    private String search = "";
    private boolean flatten = false; // tabs are the default experience; flatten is opt-in
    private int page = 0;
    private int pageCount = 1;

    // ---- factories ----

    // ---- carried pack (rides a player-inventory slot) ----

    public static PackMenu server(int id, Inventory playerInv, int boundSlot) {
        return new PackMenu(id, playerInv, boundSlot, null,
                PackItem.tierOf(playerInv.getItem(boundSlot)), ContainerLevelAccess.NULL);
    }

    public static PackMenu client(int id, Inventory playerInv, int boundSlot, PackTier tier) {
        return new PackMenu(id, playerInv, boundSlot, null, tier, ContainerLevelAccess.NULL);
    }

    // ---- placed pack (rides a block entity via a hidden synced host slot) ----

    public static PackMenu serverForBlock(int id, Inventory playerInv,
                                          com.sappersquad.packwork.block.PackContainerBlockEntity be) {
        return new PackMenu(id, playerInv, -1, new PackStackSlotContainer(be), be.getTier(),
                ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()));
    }

    public static PackMenu clientForBlock(int id, Inventory playerInv,
                                          net.minecraft.core.BlockPos pos, PackTier tier) {
        return new PackMenu(id, playerInv, -1, new PackStackSlotContainer(null), tier, ContainerLevelAccess.NULL);
    }

    private PackMenu(int id, Inventory playerInv, int boundSlot, PackStackSlotContainer hostContainer,
                     PackTier tier, ContainerLevelAccess access) {
        super(ModMenus.PACK.get(), id);
        this.playerInv = playerInv;
        this.boundSlot = boundSlot;
        this.tier = tier;
        this.hostContainer = hostContainer;
        this.access = access;
        this.liveSupplier = hostContainer != null
                ? () -> hostContainer.getItem(0)
                : () -> playerInv.getItem(boundSlot);
        this.packInv = new PackInventory(this::liveStack, tier);
        this.trinketInv = new PackTrinketInventory(this::liveStack, tier);
        this.layout = liveStack().getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        this.tabs = SortEngine.tabsFor(layout, fitted());
        this.activeTab = firstRealTab();

        // Grid of view slots (indices 0 .. VIEW_SLOTS-1).
        for (int row = 0; row < PackTier.VIEW_ROWS; row++) {
            for (int col = 0; col < PackTier.VIEW_COLS; col++) {
                PackViewSlot s = new PackViewSlot(packInv, this,
                        GRID_X + col * 18, GRID_Y + row * 18);
                viewSlots.add(s);
                addSlot(s);
            }
        }

        addPlayerInventory(playerInv);

        // Trinket sockets on the right rail (component-backed copy slots).
        this.trinketStart = slots.size();
        for (int i = 0; i < tier.trinketSlots(); i++) {
            addSlot(new net.neoforged.neoforge.items.ItemHandlerCopySlot(
                    trinketInv, i, TRINKET_X, TRINKET_Y0 + i * TRINKET_PITCH));
        }
        this.trinketEnd = slots.size();

        // The Tinker's Kit tool roll: a 3x3 and its result, always present, active only when
        // the kit is fitted and the roll is unrolled.
        this.craftStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftSlots, col + row * 3,
                        ROLL_GRID_X + col * 18, ROLL_Y + row * 18) {
                    @Override
                    public boolean isActive() {
                        return rollActive();
                    }

                    @Override
                    public boolean mayPlace(ItemStack s) {
                        return rollActive();
                    }

                    @Override
                    public boolean mayPickup(Player p) {
                        return rollActive();
                    }
                });
            }
        }
        this.resultIndex = slots.size();
        addSlot(new RollResultSlot(playerInv.player, craftSlots, resultSlots, 0,
                ROLL_RESULT_X, ROLL_RESULT_Y));

        // For a placed pack: one hidden, inactive slot mirrors the block entity's pack
        // stack so its components sync to the viewing client (as a carried pack's slot
        // does). Never rendered, never player-movable. Kept LAST so slot indices above are
        // identical on client and server (a mismatch overruns the container packet).
        if (hostContainer != null) {
            this.hostIndex = slots.size();
            addSlot(new Slot(hostContainer, 0, -9000, -9000) {
                @Override
                public boolean isActive() {
                    return false;
                }

                @Override
                public boolean mayPickup(Player p) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack s) {
                    return false;
                }
            });
        } else {
            this.hostIndex = -1;
        }

        rebuildView();
    }

    /** The tool roll's result slot: crafting a stack tops the grid back up from pack stock,
     *  so one pattern keeps producing for as long as the pack holds the makings. */
    private class RollResultSlot extends net.minecraft.world.inventory.ResultSlot {
        RollResultSlot(Player player, net.minecraft.world.inventory.CraftingContainer craft,
                       net.minecraft.world.Container result, int idx, int x, int y) {
            super(player, craft, result, idx, x, y);
        }

        @Override
        public boolean isActive() {
            return rollActive();
        }

        @Override
        public boolean mayPickup(Player p) {
            return rollActive();
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            net.minecraft.world.item.Item[] before = new net.minecraft.world.item.Item[9];
            for (int i = 0; i < 9; i++) {
                ItemStack s = craftSlots.getItem(i);
                before[i] = s.isEmpty() ? null : s.getItem();
            }
            super.onTake(player, stack);   // vanilla consumes the grid + handles container items
            refillRollFromPack(before);
        }
    }

    private String firstRealTab() {
        for (TabView t : tabs) {
            if (!t.loose()) return t.id();
        }
        return AutoTabs.LOOSE_ID;
    }

    // ---- watching the player's own hand on the grid (auto-pin + kept layouts) ----

    /** Client-side listener the screen registers so pin changes can show a note. */
    public interface PinToast {
        void pinned(ItemStack stack, Component tabName);
    }

    private PinToast pinToast; // only ever set on the client
    private ItemStack pendingPlaced = ItemStack.EMPTY;
    /** Every {cell, backing} the player's hand filled during this click - a quick-craft drag
     *  places into several cells in one click, and a kept layout must remember them all. */
    private final List<int[]> pendingCells = new ArrayList<>();
    private int pendingPickupBacking = -1; // backing slot the player emptied, or -1

    public void setPinToast(PinToast t) {
        this.pinToast = t;
    }

    /**
     * Called by {@link PackViewSlot#setByPlayer} whenever the PLAYER's own hand touches a
     * grid cell - cursor place, merge, swap, number-key swap, and the pickup that empties
     * a cell. Programmatic moves (shift-click routing, hoppers, refills) never come
     * through here, so this is exactly the "I put it HERE" / "I took it away" gesture.
     * Only recorded during the click; the decisions run after the click fully resolves
     * (see {@link #clicked}), because re-routing the view mid-click would rebind slots
     * under vanilla's own bookkeeping.
     */
    void onPlayerSetViewSlot(PackViewSlot slot, ItemStack now) {
        int p = viewSlots.indexOf(slot);
        if (p < 0) return;
        if (now.isEmpty()) {
            pendingPickupBacking = slot.backingIndex();
            return;
        }
        pendingPlaced = now.copy();
        pendingCells.add(new int[]{page * visibleSlots() + p, slot.backingIndex()});
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        // clean slate per click: if a previous click threw mid-way, its stale pendings must
        // never leak into this one
        pendingPlaced = ItemStack.EMPTY;
        pendingCells.clear();
        pendingPickupBacking = -1;
        super.clicked(slotId, button, type, player);
        flushPendingPlacement();
    }

    /**
     * Two gestures, decided once the click has fully resolved, identically on both sides
     * (active tab, page and layout are mirrored - no extra packet):
     * <ul>
     * <li><b>Auto-pin:</b> dropping an item into a tab its rules would NOT route it to
     * pins it there, so it stays where you put it instead of jumping back on the next
     * sort. Dropping it where it already belongs changes nothing. The client side also
     * raises the on-screen note.</li>
     * <li><b>Kept layouts:</b> in a keep-my-layout compartment, the cell you placed into
     * is remembered (and a pickup lets its cell go), so the arrangement is yours.</li>
     * </ul>
     */
    private void flushPendingPlacement() {
        ItemStack placed = pendingPlaced;
        List<int[]> cells = new ArrayList<>(pendingCells);
        int took = pendingPickupBacking;
        pendingPlaced = ItemStack.EMPTY;
        pendingCells.clear();
        pendingPickupBacking = -1;
        if (flatten) return;

        if (took >= 0) rememberManualPickup(took);
        if (placed.isEmpty()) return;
        if (search.isEmpty()) {
            for (int[] c : cells) rememberManualPlacement(c[0], c[1]);
        }

        String route = SortEngine.route(placed, tabs, layout);
        if (route.equals(activeTab)) return; // it belongs here already (or is pinned here)
        applyPin(activeTab, net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(placed.getItem()).toString());
        if (pinToast != null && isClient()) pinToast.pinned(placed, tabName(activeTab));
    }

    /** In a kept compartment, remember "this cell shows that backing slot". */
    private void rememberManualPlacement(int cell, int backing) {
        PackLayout cur = currentLayout();
        PackLayout.ManualTab kept = cur.manualFor(activeTab);
        if (kept == null || cell < 0 || backing < 0 || cell >= PackLayout.ManualTab.MAX_CELL) return;
        List<PackLayout.ManualTab.Cell> cells = new ArrayList<>();
        for (PackLayout.ManualTab.Cell c : kept.cells()) {
            if (c.cell() != cell && c.slot() != backing) cells.add(c); // one owner per cell & slot
        }
        cells.add(new PackLayout.ManualTab.Cell(cell, backing));
        saveManual(cur, kept.tabId(), cells);
    }

    /** A player pickup emptied a backing slot: its remembered cell lets go (gaps refill). */
    private void rememberManualPickup(int backing) {
        if (!search.isEmpty()) return;
        PackLayout cur = currentLayout();
        PackLayout.ManualTab kept = cur.manualFor(activeTab);
        if (kept == null) return;
        List<PackLayout.ManualTab.Cell> cells = new ArrayList<>();
        boolean changed = false;
        for (PackLayout.ManualTab.Cell c : kept.cells()) {
            if (c.slot() == backing) {
                changed = true;
                continue;
            }
            cells.add(c);
        }
        if (changed) saveManual(cur, kept.tabId(), cells);
    }

    private void saveManual(PackLayout cur, String tabId, List<PackLayout.ManualTab.Cell> cells) {
        List<PackLayout.ManualTab> manual = new ArrayList<>();
        for (PackLayout.ManualTab m : cur.manual()) {
            manual.add(m.tabId().equals(tabId) ? new PackLayout.ManualTab(tabId, cells) : m);
        }
        saveLayout(cur.withManual(manual));
    }

    /**
     * Flip a compartment between Tidy (the pack arranges it) and Keep-my-layout (the
     * player does). Flipping to Keep captures exactly what is showing right now, so
     * nothing moves on screen; flipping back to Tidy drops the remembered arrangement
     * and the compartment re-sorts. Items never move either way - this is pure view.
     */
    public void applyToggleTabMode(String tabId) {
        if (tabId == null || tabId.isEmpty()) return;
        PackLayout cur = currentLayout();
        PackLayout.ManualTab existing = cur.manualFor(tabId);
        List<PackLayout.ManualTab> manual = new ArrayList<>(cur.manual());
        if (existing != null) {
            manual.remove(existing);
        } else {
            boolean known = false;
            for (TabView t : tabs) known |= t.id().equals(tabId);
            if (!known) return;
            List<PackLayout.ManualTab.Cell> cells = new ArrayList<>();
            List<Integer> routed = routedIndicesFor(tabId);
            for (int i = 0; i < routed.size(); i++) {
                cells.add(new PackLayout.ManualTab.Cell(i, routed.get(i)));
            }
            manual.add(new PackLayout.ManualTab(tabId, cells));
        }
        saveLayout(cur.withManual(manual));
    }

    /** Backing indices whose stacks route to this tab, ascending - the tidy view's order. */
    private List<Integer> routedIndicesFor(String tabId) {
        List<Integer> routed = new ArrayList<>();
        for (int i = 0; i < packInv.getSlots(); i++) {
            ItemStack s = packInv.getStackInSlot(i);
            if (!s.isEmpty() && SortEngine.route(s, tabs, layout).equals(tabId)) routed.add(i);
        }
        return routed;
    }

    /** The display name of a tab on this pack's rail (falls back to the raw id). */
    public Component tabName(String tabId) {
        for (TabView t : tabs) {
            if (t.id().equals(tabId)) return t.name();
        }
        return Component.literal(tabId);
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
        // Re-read the durable layout from the (synced) live stack so both sides stay current.
        this.layout = liveStack().getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        this.tabs = SortEngine.tabsFor(layout, fitted());
        int visible = visibleSlots();
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
            // tab view: matching non-empty first, then empties to drop into - unless this
            // compartment is in keep-my-layout mode, in which case the player's own
            // arrangement decides which cell shows which backing slot.
            PackLayout.ManualTab kept = searching ? null : layout.manualFor(activeTab);
            List<Integer> routed = new ArrayList<>();
            List<Integer> empties = new ArrayList<>();
            for (int i = 0; i < packInv.getSlots(); i++) {
                ItemStack s = packInv.getStackInSlot(i);
                if (s.isEmpty()) {
                    empties.add(i);
                    continue;
                }
                if (searching && !matchesSearch(s, q)) continue;
                String route = SortEngine.route(s, tabs, layout);
                if (route.equals(activeTab)) routed.add(i);
            }
            if (kept != null) {
                buildKeptOrder(kept, routed, empties, order);
            } else {
                order.addAll(routed);
                if (!searching) order.addAll(empties);
            }
        }

        this.pageCount = Math.max(1, (order.size() + visible - 1) / visible);
        if (page >= pageCount) page = pageCount - 1;
        if (page < 0) page = 0;

        int start = page * visible;
        for (int p = 0; p < VIEW_SLOTS; p++) {
            int gi = start + p;
            if (p < visible && gi < order.size() && order.get(gi) >= 0) {
                viewSlots.get(p).bind(order.get(gi), true);
            } else {
                viewSlots.get(p).bind(-1, false); // rows the unrolled tool roll covers
            }
        }
    }

    /**
     * The keep-my-layout view, as a cell-indexed order list: remembered cells show their
     * backing slot, new arrivals fill the gaps lowest-cell-first, and every remaining cell
     * binds to a free empty backing slot so dropping things in still works. Deterministic
     * over synced state, so client and server always draw the same arrangement - and it is
     * strictly view-side: nothing here ever moves an item.
     *
     * <p>Remembered entries whose backing slot has emptied or re-routed (a hopper pulled
     * the stack, a rule changed) are simply skipped - the cell frees up. The stored list
     * is pruned as the player works, not here: rebuild runs every tick and must not write
     * the component.
     */
    private void buildKeptOrder(PackLayout.ManualTab kept, List<Integer> routed,
                                List<Integer> empties, List<Integer> order) {
        java.util.TreeMap<Integer, Integer> byCell = new java.util.TreeMap<>();
        java.util.Set<Integer> shown = new java.util.HashSet<>();
        for (PackLayout.ManualTab.Cell c : kept.cells()) {
            if (c.cell() < 0 || c.cell() >= PackLayout.ManualTab.MAX_CELL) continue;
            if (byCell.containsKey(c.cell()) || shown.contains(c.slot())) continue;
            if (!routed.contains(c.slot())) continue; // stale: emptied or re-routed
            byCell.put(c.cell(), c.slot());
            shown.add(c.slot());
        }
        int nextFree = 0;
        for (int b : routed) {                        // arrivals fill gaps, lowest cell first
            if (shown.contains(b)) continue;
            while (byCell.containsKey(nextFree)) nextFree++;
            byCell.put(nextFree, b);
            shown.add(b);
        }
        // as many cells as the tidy view would offer, or the arrangement needs - whichever is more
        int len = Math.max(byCell.isEmpty() ? 0 : byCell.lastKey() + 1, routed.size() + empties.size());
        int e = 0;
        for (int cell = 0; cell < len; cell++) {
            Integer b = byCell.get(cell);
            if (b != null) {
                order.add(b);
            } else {
                order.add(e < empties.size() ? empties.get(e++) : -1);
            }
        }
    }

    /** How many grid cells the pack is showing: the full grid, or the top rows when the roll is out. */
    public int visibleSlots() {
        return rollActive() ? VIEW_SLOTS - ROLL_HIDES : VIEW_SLOTS;
    }

    /** True while the tool roll is unrolled AND a Tinker's Kit is actually fitted. */
    public boolean rollActive() {
        return rollOpen && hasTrinket(com.sappersquad.packwork.trinket.TrinketType.TINKERS_KIT);
    }

    public int resultIndex() {
        return resultIndex;
    }

    /** The menu index of the tool roll's first 3x3 cell. */
    public int craftStart() {
        return craftStart;
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
        // Persist a placed pack's changes: every GUI mutation lands on the block entity's
        // stack, so mark it dirty while the menu is open (no-op for a carried pack).
        if (hostContainer != null) hostContainer.markChanged();
    }

    // ---- shift-click ----

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // The hidden host slot (a placed pack's own stack) is never shift-moved.
        if (hostIndex >= 0 && index == hostIndex) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();

        int playerStart = VIEW_SLOTS;
        int playerEnd = VIEW_SLOTS + PLAYER_SLOTS;

        if (index == resultIndex) {
            return quickCraftOut(player, slot, inSlot, original, playerStart, playerEnd);
        }
        if (index >= craftStart && index < resultIndex) {
            // off the tool roll -> back in the pack, else your pockets
            if (!rollActive()) return ItemStack.EMPTY;
            ItemStack leftover = insertIntoPack(inSlot.copy(), false);
            int moved = inSlot.getCount() - leftover.getCount();
            if (moved <= 0 && !moveItemStackTo(inSlot, playerStart, playerEnd, true)) return ItemStack.EMPTY;
            if (moved > 0) inSlot.shrink(moved);
            slot.setChanged();
        } else if (index < VIEW_SLOTS) {
            // With the roll unrolled, a shift-click lays ONE of that item on the bench - you're
            // setting out a pattern, not tipping the stack in. The bench then tops each cell back
            // up from the pack after every craft, so one of each is all you ever need to place.
            // Roll it back up to shift-click into your pockets again.
            if (rollActive() && layOneOnRoll(inSlot)) {
                slot.setChanged();
                rebuildView();
                return ItemStack.EMPTY;   // one per click, deliberately
            }
            if (!moveItemStackTo(inSlot, playerStart, playerEnd, true)) return ItemStack.EMPTY;
            slot.setChanged();
        } else if (index >= trinketStart && index < trinketEnd) {
            // trinket socket -> player inventory
            if (!moveItemStackTo(inSlot, playerStart, playerEnd, true)) return ItemStack.EMPTY;
            slot.setChanged();
        } else {
            // player -> a trinket socket if it's a fitting, else into the pack (auto-routed)
            if (inSlot.getItem() instanceof com.sappersquad.packwork.trinket.TrinketItem
                    && trinketStart < trinketEnd
                    && moveItemStackTo(inSlot, trinketStart, trinketEnd, false)) {
                // installed into a socket
            } else {
                ItemStack leftover = insertIntoPack(inSlot.copy());
                int moved = inSlot.getCount() - leftover.getCount();
                if (moved <= 0) return ItemStack.EMPTY;
                inSlot.shrink(moved);
            }
        }

        if (inSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        rebuildView();
        return original;
    }

    /**
     * Shift-click the tool roll's result: the crafted stack goes into the PACK first (you are,
     * after all, crafting inside it), your pockets second. Vanilla's quick-move loop calls this
     * repeatedly, and the grid tops itself back up from pack stock between crafts - so one
     * shift-click runs the batch until the pack runs out of makings.
     */
    private ItemStack quickCraftOut(Player player, Slot slot, ItemStack inSlot, ItemStack original,
                                    int playerStart, int playerEnd) {
        if (!rollActive()) return ItemStack.EMPTY;
        ItemStack leftover = insertIntoPack(inSlot.copy(), false);
        int placed = inSlot.getCount() - leftover.getCount();
        if (placed > 0) {
            inSlot.shrink(placed);
        } else if (!moveItemStackTo(inSlot, playerStart, playerEnd, true)) {
            return ItemStack.EMPTY;
        }
        slot.onQuickCraft(inSlot, original);
        if (inSlot.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (inSlot.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, inSlot);
        // anything the pack and the pockets both refused rides back to the player, never the void
        if (!inSlot.isEmpty() && !player.getInventory().add(inSlot)) player.drop(inSlot, false);
        rebuildView();
        return original;
    }

    /** Insert into the whole backing store: merge into existing stacks first, then fill empties. */
    ItemStack insertIntoPack(ItemStack stack) {
        return insertIntoPack(stack, true);
    }

    /**
     * @param allowVoid whether the Compass Rose's opt-in discard list applies. Items being HANDED
     *                  BACK (a cancelled craft, a spent bucket) always pass false: a return path
     *                  must return, never bin.
     */
    ItemStack insertIntoPack(ItemStack stack, boolean allowVoid) {
        // Compass Rose: the ONLY void path, opt-in. If this exact item is on the
        // trinket's discard list, it never enters the pack.
        ItemStack pack = liveStack();
        if (allowVoid
                && com.sappersquad.packwork.trinket.TrinketAccess.has(pack, com.sappersquad.packwork.trinket.TrinketType.COMPASS_ROSE)
                && layout.voids(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return ItemStack.EMPTY;
        }
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

    /** Number of trinket sockets this pack shows (0 for Canvas). */
    public int trinketSlotCount() {
        return tier.trinketSlots();
    }

    public boolean hasTrinket(com.sappersquad.packwork.trinket.TrinketType type) {
        return com.sappersquad.packwork.trinket.TrinketAccess.has(liveStack(), type);
    }

    /**
     * Every fitting installed in this pack right now. Drives the Quill &amp; Ledger's rule gate
     * AND the fitting-gated compartments (Charts, Catch), so a trinket adds a compartment with
     * one entry in {@link AutoTabs}. Read live from the (synced) stack, so client and server
     * compute the same tab list.
     */
    private java.util.Set<com.sappersquad.packwork.trinket.TrinketType> fitted() {
        return com.sappersquad.packwork.trinket.TrinketAccess.installed(liveStack());
    }

    public String activeTab() {
        return activeTab;
    }

    /** Is the showing compartment in keep-my-layout mode? */
    public boolean activeTabManual() {
        return layout.manualFor(activeTab) != null;
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
            case VOID_TOGGLE -> applyVoidToggle(s2);
            case FLUID_INTERACT -> applyFluidInteract();
            case XP_SIPHON -> applyXpSiphon();
            case XP_POUR -> applyXpPour();
            case TOGGLE_ROLL -> applyToggleRoll();
            case LAY_OUT_GHOST -> applyLayOutGhost(s1);
            case ADD_TAB_RULE -> applyAddTabRule(s1, arg, s2);
            case REMOVE_TAB_RULE -> applyRemoveTabRule(s1, arg);
            case TOGGLE_TAB_MODE -> applyToggleTabMode(s1);
            case TOGGLE_PACK_FIRST -> applyTogglePackFirst();
        }
    }

    /** Flip pack-first pickup for THIS pack (the Lodestone's route-what-files behaviour). */
    public void applyTogglePackFirst() {
        PackLayout cur = currentLayout();
        saveLayout(cur.withPackFirst(!cur.packFirst()));
    }

    /** Is pack-first pickup on for this pack? (Only meaningful with a Lodestone fitted.) */
    public boolean packFirst() {
        return layout.packFirst();
    }

    /**
     * The recipe browser's one server verb: lay ONE set of the named recipe's makings from
     * PACK stock onto the tool roll. All-or-nothing - the pack is checked for every
     * ingredient before a single item moves, so a half-laid pattern can never strand
     * anything. Items move only here, at the player's explicit request; the browser and its
     * ghosts are pure client-side paint.
     */
    public void applyLayOutGhost(String recipeId) {
        if (isClient()) return; // server-authoritative: it moves real items
        if (!rollActive()) return;
        Player player = playerInv.player;
        var server = player.level().getServer();
        if (server == null) return;
        var id = net.minecraft.resources.ResourceLocation.tryParse(recipeId);
        if (id == null) return;
        var holder = server.getRecipeManager().byKey(id).orElse(null);
        if (holder == null
                || !(holder.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe)
                || !recipe.canCraftInDimensions(3, 3)) {
            return;
        }
        net.minecraft.world.item.crafting.Ingredient[] wanted = arrangeOn3x3(recipe);
        if (wanted == null) return; // custom recipes (our own upgrades) have no layable shape

        // Pass 1 - SIMULATE: find a pack slot for every still-unfilled cell, spending nothing.
        // The pack is snapshotted ONCE - each getStackInSlot deserializes a component, so the
        // per-cell scan must not re-read all 256 slots nine times over.
        int slots = packInv.getSlots();
        ItemStack[] stock = new ItemStack[slots];
        for (int s = 0; s < slots; s++) stock[s] = packInv.getStackInSlot(s);
        int[] fromSlot = new int[9];
        java.util.Arrays.fill(fromSlot, -1);
        int[] spentPerSlot = new int[slots];
        for (int cell = 0; cell < 9; cell++) {
            var ing = wanted[cell];
            if (ing == null || ing.isEmpty()) continue;
            ItemStack already = craftSlots.getItem(cell);
            if (!already.isEmpty()) {
                if (ing.test(already)) continue;   // the cell already holds its makings
                return;                            // something foreign is on the roll - never overwrite
            }
            boolean found = false;
            for (int s = 0; s < slots; s++) {
                if (stock[s].isEmpty() || !ing.test(stock[s])) continue;
                if (stock[s].getCount() - spentPerSlot[s] <= 0) continue;
                fromSlot[cell] = s;
                spentPerSlot[s]++;
                found = true;
                break;
            }
            if (!found) return; // the pack can't cover the pattern: move NOTHING
        }

        // Pass 2 - EXECUTE: the pack covered everything, so pull exactly one per cell.
        for (int cell = 0; cell < 9; cell++) {
            if (fromSlot[cell] < 0) continue;
            ItemStack pulled = packInv.extractItem(fromSlot[cell], 1, false);
            if (!pulled.isEmpty()) craftSlots.setItem(cell, pulled); // setItem triggers slotsChanged
        }
        rebuildView();
    }

    // ---- the Tinker's Kit tool roll ----

    /**
     * Unroll or roll up the tool roll. Rolling it up empties the grid back into the pack, so a
     * half-set-up craft is never stranded - and neither is a cancelled one.
     */
    public void applyToggleRoll() {
        if (!hasTrinket(com.sappersquad.packwork.trinket.TrinketType.TINKERS_KIT)) {
            rollOpen = false;
        } else {
            rollOpen = !rollOpen;
            if (!rollOpen) emptyRollIntoPack();
        }
        this.page = 0;
        rebuildView();
    }

    @Override
    public void slotsChanged(net.minecraft.world.Container container) {
        if (container == craftSlots) recomputeCraftResult();
        super.slotsChanged(container);
    }

    /** Work out what the roll's pattern makes and push the result down to the viewing client. */
    private void recomputeCraftResult() {
        Player player = playerInv.player;
        if (player.level().isClientSide()) return;
        var level = player.level();
        var server = level.getServer();
        if (server == null) return;
        net.minecraft.world.item.crafting.CraftingInput input = craftSlots.asCraftInput();
        ItemStack result = ItemStack.EMPTY;
        var found = server.getRecipeManager().getRecipeFor(
                net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level);
        if (found.isPresent()) {
            // recipe-book bookkeeping only applies to a real connected player
            boolean allowed = !(player instanceof net.minecraft.server.level.ServerPlayer sp)
                    || resultSlots.setRecipeUsed(level, sp, found.get());
            if (allowed) {
                ItemStack out = found.get().value().assemble(input, level.registryAccess());
                if (out.isItemEnabled(level.enabledFeatures())) result = out;
            }
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(resultIndex, result);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                    containerId, incrementStateId(), resultIndex, result));
        }
    }

    /**
     * After a craft, top each emptied grid cell back up from pack stock - the whole point of a
     * tool roll fed by the pack. Conserves exactly: it can only put back what it takes out of
     * the store, so a batch craft stops the moment the pack runs dry.
     */
    private void refillRollFromPack(net.minecraft.world.item.Item[] before) {
        if (isClient()) return;
        boolean changed = false;
        for (int i = 0; i < before.length; i++) {
            if (before[i] == null || !craftSlots.getItem(i).isEmpty()) continue;
            ItemStack pulled = pullOneFromPack(before[i]);
            if (!pulled.isEmpty()) {
                craftSlots.setItem(i, pulled); // setItem re-runs slotsChanged -> recomputes
                changed = true;
            }
        }
        if (!changed) recomputeCraftResult();
    }

    /** Lay one of {@code from} into the first free cell of the roll (topping up a matching cell
     *  first). Returns false when the bench has no room for it. */
    private boolean layOneOnRoll(ItemStack from) {
        // Next EMPTY cell first, left to right: you're laying out a shape, so three shift-clicks
        // of wheat should be a row of three, not a pile of three in one corner. Only once every
        // cell is spoken for does another click deepen the stack already there.
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            if (craftSlots.getItem(i).isEmpty()) {
                craftSlots.setItem(i, from.split(1));
                return true;
            }
        }
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack cell = craftSlots.getItem(i);
            if (ItemStack.isSameItemSameComponents(cell, from) && cell.getCount() < cell.getMaxStackSize()) {
                from.shrink(1);
                cell.grow(1);
                craftSlots.setItem(i, cell);
                return true;
            }
        }
        return false;
    }

    /**
     * The one place a recipe's ingredient list is arranged onto the tool roll's 3x3 -
     * shaped recipes keep their shape, the rest fill left to right. The client's chalk
     * ghost and the server's lay-out BOTH call this, so they cannot drift apart.
     *
     * @return the 9-cell arrangement, or null for recipes with no layable shape (special
     *         recipes - our own pack upgrades - report no ingredients).
     */
    public static net.minecraft.world.item.crafting.Ingredient[] arrangeOn3x3(
            net.minecraft.world.item.crafting.CraftingRecipe recipe) {
        var ingredients = recipe.getIngredients();
        if (ingredients.isEmpty()) return null;
        net.minecraft.world.item.crafting.Ingredient[] wanted =
                new net.minecraft.world.item.crafting.Ingredient[9];
        if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
            for (int r = 0; r < shaped.getHeight(); r++)
                for (int c = 0; c < shaped.getWidth(); c++)
                    wanted[r * 3 + c] = ingredients.get(r * shaped.getWidth() + c);
        } else {
            for (int i = 0; i < ingredients.size() && i < 9; i++) wanted[i] = ingredients.get(i);
        }
        return wanted;
    }

    /**
     * Account everything craftable-from for the recipe browser: the pack's whole store (at
     * full DEPTH) plus whatever is already laid out on the roll. Client-safe - it reads the
     * synced component, so the browser computes the same answer the server would.
     */
    public void fillPackStacked(net.minecraft.world.entity.player.StackedContents contents) {
        int slots = packInv.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack s = packInv.getStackInSlot(i);
            if (!s.isEmpty()) contents.accountStack(s, s.getCount());
        }
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack s = craftSlots.getItem(i);
            if (!s.isEmpty()) contents.accountStack(s, s.getCount());
        }
    }

    /** Take exactly one of {@code item} out of the pack's store, or EMPTY if it holds none. */
    private ItemStack pullOneFromPack(net.minecraft.world.item.Item item) {
        for (int i = 0; i < packInv.getSlots(); i++) {
            ItemStack s = packInv.getStackInSlot(i);
            if (!s.isEmpty() && s.getItem() == item) return packInv.extractItem(i, 1, false);
        }
        return ItemStack.EMPTY;
    }

    /** Everything laid out on the roll goes back in the pack, then your pockets, then the floor. */
    private void emptyRollIntoPack() {
        if (isClient()) return;
        for (int i = 0; i < craftSlots.getContainerSize(); i++) {
            ItemStack s = craftSlots.removeItemNoUpdate(i);
            if (s.isEmpty()) continue;
            ItemStack left = insertIntoPack(s, false);
            if (left.isEmpty()) continue;
            if (!playerInv.player.getInventory().add(left)) playerInv.player.drop(left, false);
        }
        resultSlots.clearContent();
    }

    @Override
    public void removed(Player player) {
        emptyRollIntoPack();   // closing the pack never strands a half-laid-out craft
        super.removed(player);
    }

    public void applyXpSiphon() {
        if (isClient()) return; // server-authoritative: it moves the player's own XP
        if (hasTrinket(com.sappersquad.packwork.trinket.TrinketType.SOUL_VIAL)) {
            PackXpStore.siphon(liveStack(), playerInv.player);
            rebuildView();
        }
    }

    public void applyXpPour() {
        if (isClient()) return; // server-authoritative: it moves the player's own XP
        if (hasTrinket(com.sappersquad.packwork.trinket.TrinketType.SOUL_VIAL)) {
            PackXpStore.pour(liveStack(), playerInv.player);
            rebuildView();
        }
    }

    /**
     * Actions that move real items or XP run on the server ONLY. The client copy of the menu
     * mirrors layout verbs optimistically so the rail feels instant, but a cursor/inventory
     * mutation applied on both sides either double-applies or desyncs until the next sync -
     * so those wait for the server and take the sync back.
     */
    private boolean isClient() {
        return playerInv.player.level().isClientSide();
    }

    public int xpStored() {
        return PackXpStore.stored(liveStack());
    }

    public int xpCapacity() {
        return PackXpStore.capacityFor(liveStack());
    }

    public int energyStored() {
        return liveStack().getOrDefault(ModComponents.PACK_ENERGY.get(), 0);
    }

    public int energyCapacity() {
        return PackEnergyStorage.capacityFor(liveStack());
    }

    /** Flask Harness chemical amount (mB), read straight off the dist-neutral component. */
    public long chemicalStored() {
        return liveStack().getOrDefault(ModComponents.PACK_CHEMICAL.get(),
                com.sappersquad.packwork.pack.PackChemical.EMPTY).amount();
    }

    public long chemicalCapacity() {
        return com.sappersquad.packwork.pack.PackChemical.capacityFor(liveStack());
    }

    /**
     * Fill or drain the Waterskin tank using the item on the cursor (a bucket, flask, etc.).
     * Exactly ONE container is handled per click, whether you're holding one bucket or a
     * stack of sixteen: NeoForge's {@code FluidUtil.tryEmptyContainer/tryFillContainer}
     * operate on a single container and hand back a single result, so the old
     * {@code setCarried(result)} silently ate the rest of the stack.
     */
    public void applyFluidInteract() {
        if (isClient()) return; // server-authoritative: it moves a real item on the cursor
        if (!hasTrinket(com.sappersquad.packwork.trinket.TrinketType.WATERSKIN)) return;
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;
        ItemStack pack = liveStack();
        PackFluidHandler tank = new PackFluidHandler(pack, PackFluidHandler.capacityFor(pack));

        // Act on ONE container out of the carried stack.
        ItemStack one = carried.copyWithCount(1);
        Player player = playerInv.player;

        // first try to empty a filled container INTO the tank, else fill an empty one FROM it
        var result = net.neoforged.neoforge.fluids.FluidUtil.tryEmptyContainer(
                one, tank, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) {
            result = net.neoforged.neoforge.fluids.FluidUtil.tryFillContainer(
                    one, tank, Integer.MAX_VALUE, player, true);
        }
        if (!result.isSuccess()) return;

        spendOneCarried(result.getResult());
        rebuildView();
    }

    /**
     * Take one item off the cursor and hand the resulting container back, conserving exactly.
     * The result goes back on the cursor when it can merge there, otherwise into the player's
     * pockets, otherwise into the pack, and only as a last resort onto the floor - the pack
     * pauses, it never punishes.
     */
    private void spendOneCarried(ItemStack result) {
        ItemStack rest = getCarried().copy();
        rest.shrink(1);
        ItemStack give = result == null ? ItemStack.EMPTY : result.copy();

        if (give.isEmpty()) {                       // consumable container (e.g. a water bottle drunk dry)
            setCarried(rest);
            return;
        }
        if (rest.isEmpty()) {                       // the cursor is free - the result simply takes its place
            setCarried(give);
            return;
        }
        if (ItemStack.isSameItemSameComponents(rest, give)
                && rest.getCount() < rest.getMaxStackSize()) {
            int room = Math.min(rest.getMaxStackSize() - rest.getCount(), give.getCount());
            rest.grow(room);
            give.shrink(room);
        }
        setCarried(rest);
        if (give.isEmpty()) return;

        Player player = playerInv.player;
        if (!player.getInventory().add(give)) {     // pockets full? the pack takes it
            ItemStack leftover = insertIntoPack(give, false);
            if (!leftover.isEmpty()) player.drop(leftover, false); // truly nowhere left
        }
    }

    /** The fluid currently in the Waterskin tank (empty if none / no rack). */
    public net.neoforged.neoforge.fluids.FluidStack fluidStack() {
        return liveStack().getOrDefault(ModComponents.PACK_FLUID.get(),
                net.neoforged.neoforge.fluids.SimpleFluidContent.EMPTY).copy();
    }

    public int fluidCapacity() {
        return PackFluidHandler.capacityFor(liveStack());
    }

    /** Add/remove an item from the Compass Rose discard list. */
    public void applyVoidToggle(String itemId) {
        net.minecraft.resources.ResourceLocation r = net.minecraft.resources.ResourceLocation.tryParse(itemId);
        if (r == null) return;
        PackLayout cur = currentLayout();
        List<net.minecraft.resources.ResourceLocation> voids = new ArrayList<>(cur.voidList());
        if (voids.contains(r)) voids.remove(r);
        else voids.add(r);
        saveLayout(cur.withVoidList(voids));
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
        List<ItemStack> source = new ArrayList<>();
        for (int i = 0; i < packInv.getSlots(); i++) {
            ItemStack s = packInv.getStackInSlot(i);
            if (!s.isEmpty()) source.add(s);
        }
        List<ItemStack> merged = com.sappersquad.packwork.sort.PackSorting.tidy(
                source, SortEngine.tabsFor(layout, fitted()), layout, packInv::depthFor);
        for (int i = 0; i < packInv.getSlots(); i++) {
            packInv.setStackInSlot(i, i < merged.size() ? merged.get(i) : ItemStack.EMPTY);
        }
        // Tidy Up is the one-shot re-sort even for kept compartments: the sort just moved
        // every stack to a new backing slot, so the remembered arrangements are reset (the
        // sorted order becomes the new starting layout) while keep-my-layout MODE stays.
        PackLayout cur = currentLayout();
        boolean anyKept = false;
        List<PackLayout.ManualTab> cleared = new ArrayList<>();
        for (PackLayout.ManualTab m : cur.manual()) {
            anyKept |= !m.cells().isEmpty();
            cleared.add(new PackLayout.ManualTab(m.tabId(), List.of()));
        }
        if (anyKept) saveLayout(cur.withManual(cleared));
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
        saveLayout(new PackLayout(order, customs, cur.pins(), cur.voidList(), cur.manual(), cur.packFirst()));
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
        List<PackLayout.ManualTab> manual = new ArrayList<>();
        for (var m : cur.manual()) if (!m.tabId().equals(id)) manual.add(m);
        saveLayout(new PackLayout(order, customs, pins, cur.voidList(), manual, cur.packFirst()));
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

    /** The most rules one compartment may hold - plenty for a player, a lid for a hostile client. */
    private static final int MAX_RULES_PER_TAB = 16;

    /**
     * Add one authored rule to a custom tab - the Quill &amp; Ledger's whole job, so the
     * fitting must be present (checked on both sides; the editor UI is gated the same way).
     * Validated hard: only custom tabs, known rule types, a trimmed non-empty value with a
     * sane length, real predicate names, no duplicates, and a per-tab cap.
     */
    public void applyAddTabRule(String tabId, int typeOrdinal, String value) {
        if (!tabId.startsWith("custom:")) return;
        if (!hasTrinket(com.sappersquad.packwork.trinket.TrinketType.QUILL_LEDGER)) return;
        var types = com.sappersquad.packwork.sort.SortRule.Type.values();
        if (typeOrdinal < 0 || typeOrdinal >= types.length) return;
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || v.length() > 64) return;
        var rule = new com.sappersquad.packwork.sort.SortRule(types[typeOrdinal], v);
        if (rule.type() == com.sappersquad.packwork.sort.SortRule.Type.PREDICATE
                && com.sappersquad.packwork.sort.PredicateKind.byNameOrNull(v) == null) {
            return;
        }
        mutateCustom(tabId, td -> {
            if (td.rules().contains(rule) || td.rules().size() >= MAX_RULES_PER_TAB) return td;
            List<com.sappersquad.packwork.sort.SortRule> rules = new ArrayList<>(td.rules());
            rules.add(rule);
            return td.withRules(rules);
        });
    }

    /** Strike one authored rule off a custom tab (by index in its stored list). Ledger-gated. */
    public void applyRemoveTabRule(String tabId, int index) {
        if (!tabId.startsWith("custom:")) return;
        if (!hasTrinket(com.sappersquad.packwork.trinket.TrinketType.QUILL_LEDGER)) return;
        mutateCustom(tabId, td -> {
            if (index < 0 || index >= td.rules().size()) return td;
            List<com.sappersquad.packwork.sort.SortRule> rules = new ArrayList<>(td.rules());
            rules.remove(index);
            return td.withRules(rules);
        });
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

    /**
     * The pack as it currently sits, resolved live - a carried pack from its inventory slot,
     * a placed pack from the block entity (via the synced host slot). Never a captured copy.
     */
    private ItemStack liveStack() {
        return liveSupplier.get();
    }

    private PackLayout currentLayout() {
        return liveStack().getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
    }

    private List<String> ensureOrder(PackLayout cur) {
        if (!cur.tabOrder().isEmpty()) return cur.mutableTabOrder();
        List<String> o = new ArrayList<>(AutoTabs.defaultOrder());
        for (var td : cur.customTabs()) o.add(td.id());
        return o;
    }

    private void saveLayout(PackLayout newLayout) {
        liveStack().set(ModComponents.PACK_LAYOUT.get(), newLayout);
        this.layout = newLayout;
        rebuildView();
    }

    @Override
    public boolean stillValid(Player player) {
        if (hostContainer != null) {
            // placed pack: the block must still be there and the player in reach
            return AbstractContainerMenu.stillValid(access, player,
                    com.sappersquad.packwork.reg.ModBlocks.PACK.get());
        }
        return liveStack().getItem() instanceof PackItem;
    }
}
