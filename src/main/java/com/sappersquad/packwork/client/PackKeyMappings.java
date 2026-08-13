package com.sappersquad.packwork.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.net.OpenPackPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The keybind that opens the pack straight from the inventory - no need to hold it.
 * Default B (for backpack); the server finds the first pack the player carries, and if
 * the pockets hold none, the one worn on the back (Curios). Shift-B asks for the WORN
 * pack first, for the player carrying spares who wants the one on their shoulders.
 */
public final class PackKeyMappings {

    public static final KeyMapping OPEN = new KeyMapping(
            "key.packwork.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KeyMapping.Category.INVENTORY);

    /**
     * Open the pack worn in the Curios back slot, explicitly. Default Shift-B (rebindable
     * in Controls like any mapping); NeoForge's modifier system hands Shift-B clicks to
     * this mapping and plain-B clicks to {@link #OPEN}. Registered even without Curios -
     * the server just falls back to the pocket scan, so the key never dead-ends.
     */
    public static final KeyMapping OPEN_WORN = new KeyMapping(
            "key.packwork.open_worn",
            net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
            net.neoforged.neoforge.client.settings.KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, KeyMapping.Category.INVENTORY);

    /**
     * Pin/unpin the hovered grid item to the active tab. Only meaningful inside the pack GUI,
     * so it has no client-tick handler - {@code PackScreen.keyPressed} matches against it. It's
     * registered so it shows up (and is rebindable) in vanilla Controls; default P.
     */
    public static final KeyMapping PIN = new KeyMapping(
            "key.packwork.pin", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, KeyMapping.Category.INVENTORY);

    @EventBusSubscriber(modid = Packwork.MODID, value = Dist.CLIENT)
    public static final class Registrar {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
            event.register(OPEN_WORN);
            event.register(PIN);
        }
    }

    @EventBusSubscriber(modid = Packwork.MODID, value = Dist.CLIENT)
    public static final class Input {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            while (OPEN_WORN.consumeClick()) {
                ClientPacketDistributor.sendToServer(new OpenPackPayload(-1, true));
            }
            while (OPEN.consumeClick()) {
                ClientPacketDistributor.sendToServer(new OpenPackPayload(-1, false));
            }
        }
    }

    private PackKeyMappings() {}
}
