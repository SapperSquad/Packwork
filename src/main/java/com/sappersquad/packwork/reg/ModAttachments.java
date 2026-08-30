package com.sappersquad.packwork.reg;

import com.mojang.serialization.Codec;
import com.sappersquad.packwork.Packwork;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The death-handling stash: packs swept out of a dying player's drops (config
 * {@code death.handling = "keep"}, or {@code "place"} falling back) ride here across the
 * respawn - {@code copyOnDeath} carries the attachment onto the fresh player entity, and
 * it survives a relog mid-death because it serializes with the player file. The respawn
 * hook hands the packs back and clears it.
 *
 * <p>(Fabric) Fabric API's data-attachment module is the analogue of NeoForge's
 * attachments, and close enough that the shape reads the same: one persistent, copy-on-death
 * attachment holding a list of stacks. The one difference worth noting is that Fabric takes
 * a plain {@link Codec} where NeoForge wants a MapCodec.
 */
public final class ModAttachments {

    public static final AttachmentType<List<ItemStack>> KEPT_PACKS =
            AttachmentRegistry.<List<ItemStack>>builder()
                    .initializer(ArrayList::new)
                    .persistent(ItemStack.CODEC.listOf()
                            .xmap(l -> (List<ItemStack>) new ArrayList<>(l), l -> l))
                    .copyOnDeath()
                    .buildAndRegister(Packwork.id("kept_packs"));

    /** Forces the classload so the attachment is registered in order with everything else. */
    public static void init() {}

    private ModAttachments() {}
}
