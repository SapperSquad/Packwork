package com.sappersquad.packwork.client;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.pack.PackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The pack, opened. Phase 0: the leather-and-brass panel with the item grid and
 * the player inventory. The tab rail, search field, and buttons arrive in Phase 1.
 */
public class PackScreen extends AbstractContainerScreen<PackMenu> {

    private static final ResourceLocation BG = Packwork.id("textures/gui/pack.png");

    public PackScreen(PackMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = PackMenu.IMAGE_W;
        this.imageHeight = PackMenu.IMAGE_H;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // renderBg only fires from renderBackground() - never leave this empty.
        g.blit(BG, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Pack name in the stitched title strip; skip the default "Inventory" label.
        g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xE8DCC0, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
