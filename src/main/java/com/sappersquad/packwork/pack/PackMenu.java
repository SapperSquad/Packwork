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

    private static final int VIEW_SLOTS = PackTier.VIEW_SLOTS;
    private static final int PLAYER_SLOTS = 36;

    private final Inventory playerInv;
    private final int boundSlot;   // player-inventory slot for a carried pack, or -1 for a placed one
    private final PackTier tier;
    private final PackInventory packInv;
    private final PackTrinketInventory trinketInv;
    private final List<PackViewSlot> viewSlots = new ArrayList<>();
    private final int trinketStart; // menu index where trinket slots begin
    private final int trinketEnd;   // one past the last trinket slot (host slot, if any, sits here)

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
        this.tabs = SortEngine.tabsFor(layout, hasLedger());
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

        // Trinket sockets on the right rail (component-backed copy slots).
        this.trinketStart = slots.size();
        for (int i = 0; i < tier.trinketSlots(); i++) {
            addSlot(new net.neoforged.neoforge.items.ItemHandlerCopySlot(
                    trinketInv, i, TRINKET_X, TRINKET_Y0 + i * TRINKET_PITCH));
        }
        this.trinketEnd = slots.size();

        // For a placed pack: one hidden, inactive slot mirrors the block entity's pack
        // stack so its components sync to the viewing client (as a carried pack's slot
        // does). Never rendered, never player-movable. Kept LAST so slot indices above are
        // identical on client and server (a mismatch overruns the container packet).
        if (hostContainer != null) {
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
        }

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
        // Re-read the durable layout from the (synced) live stack so both sides stay current.
        this.layout = liveStack().getOrDefault(ModComponents.PACK_LAYOUT.get(), PackLayout.EMPTY);
        this.tabs = SortEngine.tabsFor(layout, hasLedger());
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
        // Persist a placed pack's changes: every GUI mutation lands on the block entity's
        // stack, so mark it dirty while the menu is open (no-op for a carried pack).
        if (hostContainer != null) hostContainer.markChanged();
    }

    // ---- shift-click ----

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // The hidden host slot (a placed pack's own stack) is never shift-moved.
        if (hostContainer != null && index >= trinketEnd) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();

        int playerStart = VIEW_SLOTS;
        int playerEnd = VIEW_SLOTS + PLAYER_SLOTS;

        if (index < VIEW_SLOTS) {
            // pack view -> player inventory (never into trinket sockets)
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

    /** Insert into the whole backing store: merge into existing stacks first, then fill empties. */
    ItemStack insertIntoPack(ItemStack stack) {
        // Compass Rose: the ONLY void path, opt-in. If this exact item is on the
        // trinket's discard list, it never enters the pack.
        ItemStack pack = liveStack();
        if (com.sappersquad.packwork.trinket.TrinketAccess.has(pack, com.sappersquad.packwork.trinket.TrinketType.COMPASS_ROSE)
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

    /** Whether a Quill &amp; Ledger is fitted, so custom tabs match by rule (not just pins). */
    private boolean hasLedger() {
        return hasTrinket(com.sappersquad.packwork.trinket.TrinketType.QUILL_LEDGER);
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
            case VOID_TOGGLE -> applyVoidToggle(s2);
            case FLUID_INTERACT -> applyFluidInteract();
            case XP_SIPHON -> applyXpSiphon();
            case XP_POUR -> applyXpPour();
        }
    }

    public void applyXpSiphon() {
        if (hasTrinket(com.sappersquad.packwork.trinket.TrinketType.SOUL_VIAL)) {
            PackXpStore.siphon(liveStack(), playerInv.player);
            rebuildView();
        }
    }

    public void applyXpPour() {
        if (hasTrinket(com.sappersquad.packwork.trinket.TrinketType.SOUL_VIAL)) {
            PackXpStore.pour(liveStack(), playerInv.player);
            rebuildView();
        }
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

    /** Fill or drain the Waterskin tank using the item on the cursor (a bucket, flask, etc.). */
    public void applyFluidInteract() {
        if (!hasTrinket(com.sappersquad.packwork.trinket.TrinketType.WATERSKIN)) return;
        ItemStack carried = getCarried();
        if (carried.isEmpty()) return;
        ItemStack pack = liveStack();
        PackFluidHandler tank = new PackFluidHandler(pack, PackFluidHandler.capacityFor(pack));

        // first try to empty a filled container INTO the tank, else fill an empty one FROM it
        var emptied = net.neoforged.neoforge.fluids.FluidUtil.tryEmptyContainer(
                carried, tank, Integer.MAX_VALUE, null, true);
        if (emptied.isSuccess()) {
            setCarried(emptied.getResult());
            rebuildView();
            return;
        }
        var filled = net.neoforged.neoforge.fluids.FluidUtil.tryFillContainer(
                carried, tank, Integer.MAX_VALUE, null, true);
        if (filled.isSuccess()) {
            setCarried(filled.getResult());
            rebuildView();
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
                source, SortEngine.tabsFor(layout, hasLedger()), layout);
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
        saveLayout(new PackLayout(order, customs, cur.pins(), cur.voidList()));
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
        saveLayout(new PackLayout(order, customs, pins, cur.voidList()));
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
