package com.sappersquad.packwork.client;

import net.minecraft.client.Minecraft;

/** Client-only bridge for HandbookItem - see its class doc for why this is separate. */
public final class HandbookClientHooks {

    private HandbookClientHooks() {}

    public static void open() {
        Minecraft.getInstance().gui.setScreen(new OutfitterHandbookScreen());
    }
}
