package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import com.sappersquad.packwork.block.PackContainerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Packwork.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackContainerBlockEntity>> PACK =
            BLOCK_ENTITIES.register("pack", () ->
                    BlockEntityType.Builder.of(PackContainerBlockEntity::new, ModBlocks.PACK.get()).build(null));
}
