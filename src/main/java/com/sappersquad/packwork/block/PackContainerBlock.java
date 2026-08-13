package com.sappersquad.packwork.block;

import com.mojang.serialization.MapCodec;
import com.sappersquad.packwork.pack.PackItem;
import com.sappersquad.packwork.pack.PackMenu;
import com.sappersquad.packwork.pack.PackTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A pack sitting in the world - leather-and-brass, on a small stand, never a machine.
 * Placing the pack item creates it (see {@code PackItem.useOn}); it renders as the pack,
 * tier-tinted and facing the player, and breaking it returns the pack item with every bit
 * of content intact (see {@link #getDrops}). Right-click opens the same tabbed organizer.
 */
public class PackContainerBlock extends BaseEntityBlock {

    public static final MapCodec<PackContainerBlock> CODEC = simpleCodec(PackContainerBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** The material tier, set from the placed pack item so per-tier models/textures resolve
     *  statically in the world (studs/plates/runes). Contents still live on the block entity;
     *  this is render state only and never feeds the drop (see {@link #getDrops}). */
    public static final EnumProperty<PackTier> TIER = EnumProperty.create("tier", PackTier.class);

    // A pack resting on the ground: a squat box, not a full cube.
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);

    public PackContainerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, PackTier.LEATHER));
    }

    @Override
    protected MapCodec<PackContainerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Front (the buckle/flap face) turns to face whoever placed it; the tier comes from the
        // pack being set down, so the placed model shows that tier's trim.
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection().getOpposite())
                .setValue(TIER, PackItem.tierOf(ctx.getItemInHand()));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PackContainerBlockEntity(pos, state);
    }

    /** Right-click opens the same tabbed organizer, bound to this block's pack stack. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof PackContainerBlockEntity be) || be.isEmpty()) {
            return InteractionResult.PASS;
        }
        // Mirror PackItem.openPack's extra data: the host kind, the pos, and the tier so
        // the client builds the SAME trinket-socket count before anything syncs.
        player.openMenu(new net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<com.sappersquad.packwork.net.PackOpenData>() {
            @Override
            public Component getDisplayName() {
                return be.getPackStack().getHoverName();
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player p) {
                return PackMenu.serverForBlock(id, playerInv, be);
            }

            @Override
            public com.sappersquad.packwork.net.PackOpenData getScreenOpeningData(
                    net.minecraft.server.level.ServerPlayer sp) {
                return com.sappersquad.packwork.net.PackOpenData.block(pos, be.getTier().ordinal());
            }
        });
        return InteractionResult.CONSUME;
    }

    /** Break (or explode) returns the pack item itself, with all its contents on it. */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof PackContainerBlockEntity pbe && !pbe.isEmpty()) {
            return List.of(pbe.getPackStack().copy());
        }
        return List.of();
    }

    /** Middle-click (pick block) in creative hands back the pack with its contents. */
    @Override
    protected ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos,
                                          BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof PackContainerBlockEntity be && !be.isEmpty()) {
            return be.getPackStack().copy();
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }
}
