package com.sappersquad.packwork.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sappersquad.packwork.block.PackContainerBlock;
import com.sappersquad.packwork.config.PackworkConfig;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackTier;
import com.sappersquad.packwork.reg.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The pack on your back, visibly: when a pack is worn in the Trinkets back slot it renders
 * on the player - the same per-tier block model a set-down pack uses (leather, buckle,
 * trim, the Sculkhide's echo-lit hide), scaled onto the shoulders and riding the body's
 * crouch/swim pose. Every screenshot of a worn pack is the mod's own art.
 *
 * <p>The placement numbers below are the ones tuned in the pixels on the 1.21.1 original and
 * carried across every branch since: scale 0.50, y 0.30, z 0.27 (0.32 riding over a
 * chestplate). Behaviour matches too - steps aside for an elytra, hides with invisibility,
 * honours {@code show_worn_pack} in {@code packwork-client.toml}.
 *
 * <p>(Fabric) Drawing a block on 26.x is two steps: a {@link BlockModelResolver} - handed out
 * only through the renderer context, so it arrives in the constructor - bakes the blockstate
 * into a {@link BlockModelRenderState}, and THAT submits itself to the collector, which draws
 * later in the frame. The baked state is parked on the player's render state (Fabric's
 * {@link RenderStateDataKey}, the analogue of NeoForge's ContextKey) exactly as vanilla parks
 * the enderman's carried block on its own.
 *
 * <p>The NeoForge branches also carry the worn STACK across on the render state, because
 * NeoForge has an event for modifying a state as it is extracted. Fabric API has no such
 * hook, and adding a fifth mixin for it would buy nothing: vanilla already writes the
 * entity's id onto {@link AvatarRenderState}, so the layer looks the player back up in the
 * client level - a map lookup, once per player per frame, on the same thread that filled the
 * state in the first place. Mannequins share this renderer and are not players; they simply
 * fail the lookup and draw nothing.
 *
 * <p>Gates: the layer is only registered when Trinkets is loaded (see
 * {@code PackworkClient}), and the worn stack is read through {@code TrinketsCompat} - the
 * one-class rule holds.
 */
public class WornPackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    /** The baked block model for this player's pack, kept per render state (see class doc). */
    private static final RenderStateDataKey<BlockModelRenderState> WORN_PACK_MODEL =
            RenderStateDataKey.create(() -> "packwork:worn_pack_model");

    /** A plain marker the resolver wants; it carries no state of its own. */
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    /** Kept as a field so the gate is checked once, not per frame. */
    private static final boolean TRINKETS_LOADED =
            FabricLoader.getInstance().isModLoaded("trinkets");

    private final BlockModelResolver blockModels;

    public WornPackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                         BlockModelResolver blockModels) {
        super(parent);
        this.blockModels = blockModels;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int packedLight,
                       AvatarRenderState state, float yRot, float xRot) {
        if (!TRINKETS_LOADED || !PackworkConfig.showWornPack()) return;
        if (state.isInvisible) return;
        if (state.chestEquipment.is(Items.ELYTRA)) return; // wings own the back - hide, never clip
        ItemStack pack = wornPack(state);
        if (!(pack.getItem() instanceof PackItem)) return;
        PackTier tier = PackItem.tierOf(pack);

        BlockState blockState = ModBlocks.PACK.get().defaultBlockState()
                .setValue(PackContainerBlock.FACING, Direction.NORTH)
                .setValue(PackContainerBlock.TIER, tier);
        FabricRenderState data = (FabricRenderState) state;
        BlockModelRenderState model = data.getData(WORN_PACK_MODEL);
        if (model == null) {
            model = new BlockModelRenderState();
            data.setData(WORN_PACK_MODEL, model);
        }
        blockModels.update(model, blockState, DISPLAY_CONTEXT);

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
        model.submit(pose, collector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
    }

    /** The stack in this avatar's back slot, or EMPTY if it is not a player wearing a pack. */
    private static ItemStack wornPack(AvatarRenderState state) {
        var level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;
        return level.getEntity(state.id) instanceof Player player
                ? com.sappersquad.packwork.compat.trinkets.TrinketsCompat.wornPack(player)
                : ItemStack.EMPTY;
    }
}
