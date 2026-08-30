package com.sappersquad.packwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sappersquad.packwork.block.PackContainerBlock;
import com.sappersquad.packwork.config.PackworkConfig;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.reg.ModBlocks;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/**
 * The pack on your back, visibly: when a pack is worn in the Curios back slot it renders
 * on the player - the same per-tier block model a set-down pack uses (leather, buckle,
 * trim, the Sculkhide's echo-lit hide), scaled onto the shoulders and riding the body's
 * crouch/swim pose. Every screenshot of a worn pack is the mod's own art.
 *
 * <p>(1.21.10+ port) Two things moved under this layer at once. Layers see a render STATE,
 * not the entity, so the worn stack cannot be read here - it is attached each frame in
 * {@code ClientSetup}'s render-state modifier and picked back up through {@link #WORN_PACK}.
 * And the draw call is no longer immediate: {@code render} became {@code submit}, the layer
 * hands work to a {@link SubmitNodeCollector} that draws later in the frame, and
 * {@code BlockRenderDispatcher.renderSingleBlock} gave way to
 * {@code SubmitNodeCollector.submitBlock}. The geometry below is unchanged from the
 * 1.21.1 original - those numbers were tuned in the pixels and carry forward verbatim.
 *
 * <p>Gates: the layer and the modifier are only registered when Curios is loaded (see
 * {@code ClientSetup}), and the worn stack is read through {@code CuriosCompat} - the
 * one-class rule holds. It steps aside for an elytra (wings own the back; no z-fighting,
 * no clipping), hides with invisibility, and the client can turn it off in
 * {@code packwork-client.toml} ({@code show_worn_pack = false}).
 */
public class WornPackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    /**
     * The worn pack, carried on the render state. Always set (EMPTY when there is none)
     * rather than left absent, so a stale value from a previous frame can never draw a
     * pack onto a player who just took theirs off.
     */
    public static final ContextKey<ItemStack> WORN_PACK = new ContextKey<>(
            ResourceLocation.fromNamespaceAndPath("packwork", "worn_pack"));

    /** Kept as a field so the gate is checked once, not per frame. */
    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    public WornPackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (!CURIOS_LOADED || !PackworkConfig.showWornPack()) return;
        if (state.isInvisible) return;
        if (state.chestEquipment.is(Items.ELYTRA)) return; // wings own the back - hide, never clip
        ItemStack pack = state.getRenderData(WORN_PACK);
        if (pack == null || !(pack.getItem() instanceof PackItem)) return;
        PackTier tier = PackItem.tierOf(pack);

        pose.pushPose();
        // Ride the body part so the pack follows crouching, swimming and mount poses.
        getParentModel().body.translateAndRotate(pose);
        // Body space runs y-DOWN from the shoulders with the back at +z, one unit per block.
        // The torso is 0.5 wide by 0.75 tall, so a 0.5-scaled block leaves a hand's width of
        // shoulder above the flap and a strip of back below it - it reads as a pack you put
        // on, not a wardrobe strapped to your spine. Sitting the pack's inner face a hair
        // INSIDE the back (0.0825 against a 0.125 surface) kills the gap without clipping
        // through the chest; a chestplate inflates the body, so the pack rides out to match.
        pose.translate(0.0F, 0.30F, state.chestEquipment.isEmpty() ? 0.27F : 0.32F);
        // One X-flip turns block space (y up) into body space (y down) with the block's
        // north-facing front (the flap and trim) pointing outward, away from the spine.
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        float s = 0.50F;
        pose.scale(s, s, s);
        pose.translate(-0.5F, -0.5F, -0.5F);
        BlockState blockState = ModBlocks.PACK.get().defaultBlockState()
                .setValue(PackContainerBlock.FACING, Direction.NORTH)
                .setValue(PackContainerBlock.TIER, tier);
        collector.submitBlock(pose, blockState, packedLight, OverlayTexture.NO_OVERLAY,
                state.outlineColor);
        pose.popPose();
    }
}
