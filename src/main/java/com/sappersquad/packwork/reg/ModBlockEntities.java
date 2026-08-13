package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.block.PackContainerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    // 26.1 vanilla's BlockEntityType constructor is private on Fabric (NeoForge patches it
    // open); FabricBlockEntityTypeBuilder is the supported path.
    public static final RegHandle<BlockEntityType<PackContainerBlockEntity>> PACK =
            new RegHandle<>(Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Packwork.id("pack"),
                    FabricBlockEntityTypeBuilder.create(PackContainerBlockEntity::new, ModBlocks.PACK.get())
                            .build()));

    /** Touch to classload + register everything above (called once from mod init). */
    public static void init() {}
}
