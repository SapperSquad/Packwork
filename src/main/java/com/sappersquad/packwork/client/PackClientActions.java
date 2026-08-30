package com.sappersquad.packwork.client;

import com.sappersquad.packwork.net.PackAction;
import com.sappersquad.packwork.net.PackActionPayload;
import com.sappersquad.packwork.pack.PackMenu;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Client-side glue for the pack GUI: apply every action to the open menu
 * immediately (so the rail feels instant) and send it to the server (the
 * authority) in the same breath. Both sides run the same apply logic, so their
 * views agree.
 */
public final class PackClientActions {

    public static void send(PackMenu menu, PackAction action, int arg, String s1, String s2) {
        // Optimistic local apply for responsiveness - but only for the layout verbs. Anything
        // that moves a real item or the player's XP waits for the server and takes the sync
        // back, so a click can never half-apply twice.
        if (!action.serverAuthoritative()) {
            menu.handleAction(action.ordinal(), arg, s1 == null ? "" : s1, s2 == null ? "" : s2);
        }
        ClientPacketDistributor.sendToServer(PackActionPayload.of(action, arg, s1, s2));
    }

    public static void selectTab(PackMenu menu, String tabId) {
        send(menu, PackAction.SELECT_TAB, 0, tabId, "");
    }

    public static void setSearch(PackMenu menu, String text) {
        send(menu, PackAction.SET_SEARCH, 0, text, "");
    }

    public static void toggleFlatten(PackMenu menu) {
        send(menu, PackAction.TOGGLE_FLATTEN, 0, "", "");
    }

    public static void page(PackMenu menu, int delta) {
        send(menu, PackAction.PAGE, delta, "", "");
    }

    public static void tidyUp(PackMenu menu) {
        send(menu, PackAction.TIDY_UP, 0, "", "");
    }

    public static void newTab(PackMenu menu) {
        send(menu, PackAction.CREATE_TAB, 0, "", "");
    }

    public static void deleteTab(PackMenu menu, String tabId) {
        send(menu, PackAction.DELETE_TAB, 0, tabId, "");
    }

    public static void renameTab(PackMenu menu, String tabId, String name) {
        send(menu, PackAction.RENAME_TAB, 0, tabId, name);
    }

    public static void moveTab(PackMenu menu, String tabId, int delta) {
        send(menu, PackAction.MOVE_TAB, delta, tabId, "");
    }

    public static void tabColor(PackMenu menu, String tabId, int argb) {
        send(menu, PackAction.SET_TAB_COLOR, argb, tabId, "");
    }

    public static void tabIcon(PackMenu menu, String tabId, String itemId) {
        send(menu, PackAction.SET_TAB_ICON, 0, tabId, itemId);
    }

    public static void pin(PackMenu menu, String tabId, String itemId) {
        send(menu, PackAction.PIN_ITEM, 0, tabId, itemId);
    }

    public static void unpin(PackMenu menu, String itemId) {
        send(menu, PackAction.UNPIN_ITEM, 0, "", itemId);
    }

    public static void voidToggle(PackMenu menu, String itemId) {
        send(menu, PackAction.VOID_TOGGLE, 0, "", itemId);
    }

    public static void spillCycle(PackMenu menu, String itemId) {
        send(menu, PackAction.SPILL_CYCLE, 0, "", itemId);
    }

    public static void fluidInteract(PackMenu menu) {
        send(menu, PackAction.FLUID_INTERACT, 0, "", "");
    }

    public static void xpSiphon(PackMenu menu) {
        send(menu, PackAction.XP_SIPHON, 0, "", "");
    }

    public static void xpPour(PackMenu menu) {
        send(menu, PackAction.XP_POUR, 0, "", "");
    }

    public static void toggleRoll(PackMenu menu) {
        send(menu, PackAction.TOGGLE_ROLL, 0, "", "");
    }

    public static void layOutGhost(PackMenu menu, String recipeId) {
        send(menu, PackAction.LAY_OUT_GHOST, 0, recipeId, "");
    }

    public static void addTabRule(PackMenu menu, String tabId, int ruleTypeOrdinal, String value) {
        send(menu, PackAction.ADD_TAB_RULE, ruleTypeOrdinal, tabId, value);
    }

    public static void removeTabRule(PackMenu menu, String tabId, int index) {
        send(menu, PackAction.REMOVE_TAB_RULE, index, tabId, "");
    }

    public static void toggleTabMode(PackMenu menu, String tabId) {
        send(menu, PackAction.TOGGLE_TAB_MODE, 0, tabId, "");
    }

    public static void togglePackFirst(PackMenu menu) {
        send(menu, PackAction.TOGGLE_PACK_FIRST, 0, "", "");
    }

    public static void ledgerRefresh(PackMenu menu) {
        send(menu, PackAction.LEDGER_REFRESH, 0, "", "");
    }

    public static void requestGhost(PackMenu menu, String recipeId) {
        send(menu, PackAction.REQUEST_GHOST, 0, recipeId, "");
    }

    /** Server answered LEDGER_REFRESH: hand the craftable list to the open pack screen. */
    public static void handleLedgerSync(com.sappersquad.packwork.net.LedgerSyncPayload payload) {
        if (Minecraft.getInstance().screen instanceof PackScreen screen) {
            screen.applyLedgerSync(payload);
        }
    }

    /** Server answered REQUEST_GHOST: chalk (or clear) the arrangement on the open screen. */
    public static void handleGhostSync(com.sappersquad.packwork.net.GhostSyncPayload payload) {
        if (Minecraft.getInstance().screen instanceof PackScreen screen) {
            screen.applyGhostSync(payload);
        }
    }

    public static PackMenu openMenu() {
        return Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.containerMenu instanceof PackMenu m ? m : null;
    }

    private PackClientActions() {}
}
