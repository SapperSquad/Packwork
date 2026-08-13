package com.sappersquad.packwork.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sappersquad.packwork.net.OpenPackPayload;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * The keybind that opens the pack straight from the inventory - no need to hold it.
 * Default B (for backpack); the server finds the first pack the player carries, and if
 * the pockets hold none, the one worn on the back (Trinkets). <b>Shift-B</b> asks for
 * the WORN pack first, for the player carrying spares who wants the one on their
 * shoulders.
 *
 * <p>Fabric has no NeoForge-style key-modifier system, so Shift-B rides the OPEN
 * binding itself (shift held = worn-first) - out of the box it feels identical to the
 * NeoForge branches. OPEN_WORN still exists as its own rebindable mapping (unbound by
 * default) for anyone who wants worn-open on a dedicated key.
 */
public final class PackKeyMappings {

    public static final KeyMapping OPEN = new KeyMapping(
            "key.packwork.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KeyMapping.Category.INVENTORY);

    /** Open the worn back-slot pack outright. Unbound by default (Shift-B covers it);
     *  rebindable in Controls like any mapping. The server just falls back to the pocket
     *  scan without Trinkets, so the key never dead-ends. */
    public static final KeyMapping OPEN_WORN = new KeyMapping(
            "key.packwork.open_worn", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
            KeyMapping.Category.INVENTORY);

    /**
     * Pin/unpin the hovered grid item to the active tab. Only meaningful inside the pack GUI,
     * so it has no client-tick handler - {@code PackScreen.keyPressed} matches against it. It's
     * registered so it shows up (and is rebindable) in vanilla Controls; default P.
     */
    public static final KeyMapping PIN = new KeyMapping(
            "key.packwork.pin", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, KeyMapping.Category.INVENTORY);

    /** Register the mappings + the tick handler (called once from the client entrypoint). */
    public static void register() {
        KeyMappingHelper.registerKeyMapping(OPEN);
        KeyMappingHelper.registerKeyMapping(OPEN_WORN);
        KeyMappingHelper.registerKeyMapping(PIN);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.screen != null) return;
            while (OPEN_WORN.consumeClick()) {
                ClientPlayNetworking.send(new OpenPackPayload(-1, true));
            }
            while (OPEN.consumeClick()) {
                // Shift-B: the worn back-slot pack first (the NeoForge branches' KeyModifier
                // gesture, expressed as a live GLFW shift poll here - 26.1's input rework
                // dropped Screen.hasShiftDown).
                boolean shift = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)
                        || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
                ClientPlayNetworking.send(new OpenPackPayload(-1, shift));
            }
        });
    }

    private PackKeyMappings() {}
}
