package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.net.PackOpenData;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {

    public static final RegHandle<MenuType<PackMenu>> PACK =
            new RegHandle<>(Registry.register(BuiltInRegistries.MENU, Packwork.id("pack"),
                    new ExtendedMenuType<>(PackItem.CLIENT_FACTORY, PackOpenData.STREAM_CODEC)));

    /** Touch to classload + register everything above (called once from mod init). */
    public static void init() {}
}
