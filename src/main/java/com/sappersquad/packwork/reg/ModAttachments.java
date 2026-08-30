package com.sappersquad.packwork.reg;

import com.sappersquad.packwork.Packwork;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The death-handling stash: packs swept out of a dying player's drops (config
 * {@code death.handling = "keep"}, or {@code "place"} falling back) ride here across the
 * respawn - {@code copyOnDeath} carries the attachment onto the fresh player entity, and
 * it survives a relog mid-death because it serializes with the player file. The respawn
 * hook hands the packs back and clears it.
 */
public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Packwork.MODID);

    public static final Supplier<AttachmentType<List<ItemStack>>> KEPT_PACKS =
            ATTACHMENTS.register("kept_packs", () -> AttachmentType.<List<ItemStack>>builder(
                            (Supplier<List<ItemStack>>) ArrayList::new)
                    .serialize(ItemStack.CODEC.listOf()
                            .xmap(l -> (List<ItemStack>) new ArrayList<>(l), l -> l), list -> !list.isEmpty())
                    .copyOnDeath()
                    .build());
}
