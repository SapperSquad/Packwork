package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.PackTier;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {

    public static final RegHandle<CreativeModeTab> PACKWORK =
            new RegHandle<>(Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Packwork.id("packwork"),
                    FabricCreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.packwork"))
                            .icon(() -> new ItemStack(ModItems.pack(PackTier.LEATHER).get()))
                            .displayItems((params, output) ->
                                    ModItems.ALL.forEach(item -> {
                                        // The Flask Harness bottles Mekanism vapors, and Mekanism is
                                        // NeoForge-only - on Fabric the gate can never light, so the
                                        // fitting stays off the shelf (its recipe is condition-gated
                                        // dark too; no dead craftables).
                                        if (item.get() instanceof com.sappersquad.packwork.trinket.TrinketItem t) {
                                            if (t.type() == com.sappersquad.packwork.trinket.TrinketType.FLASK_HARNESS) {
                                                return;
                                            }
                                            // A config-retired fitting leaves the shelf along
                                            // with its recipe - no dead craftables, nothing to
                                            // wonder about.
                                            if (!com.sappersquad.packwork.config.PackworkConfig.get().enabled(t.type())) {
                                                return;
                                            }
                                        }
                                        output.accept(item.get());
                                    }))
                            .build()));

    /** Touch to classload + register everything above (called once from mod init). */
    public static void init() {}
}
