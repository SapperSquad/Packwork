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
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The keybind that opens the pack straight from the inventory - no need to hold it.
 * Default B (for backpack); the server finds the first pack the player carries.
 */
public final class PackKeyMappings {

    public static final KeyMapping OPEN = new KeyMapping(
            "key.packwork.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.inventory");

    @EventBusSubscriber(modid = Packwork.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registrar {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
        }
    }

    @EventBusSubscriber(modid = Packwork.MODID, value = Dist.CLIENT)
    public static final class Input {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) return;
            while (OPEN.consumeClick()) {
                PacketDistributor.sendToServer(new OpenPackPayload(-1));
            }
        }
    }

    private PackKeyMappings() {}
}
