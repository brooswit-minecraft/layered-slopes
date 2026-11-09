package io.github.brooswitminecraft.layeredslopes;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A 1/16-height layered block. Floor-anchored layers grow upward from the
 * bottom of the cell; ceiling-anchored layers grow downward from the top.
 * Placing more of the same block on a matching face adds a layer, up to a
 * full block. Layers stay where placed unless {@code caveInsEnabled} finds
 * them unsupported, in which case {@link CaveIns} drops them under gravity
 * like vanilla sand/gravel (see {@link net.minecraft.world.entity.item.FallingBlockEntity#fall}).
 */
public class LayeredBlock extends Block {
    public static final MapCodec<LayeredBlock> CODEC = simpleCodec(LayeredBlock::new);
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", LayerMath.MIN_LAYERS, LayerMath.MAX_LAYERS);
    public static final EnumProperty<Anchor> ANCHOR = EnumProperty.create("anchor", Anchor.class);

    private static final VoxelShape[][] SHAPES = buildShapes();

    public LayeredBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, LayerMath.MIN_LAYERS).setValue(ANCHOR, Anchor.FLOOR));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    private static VoxelShape[][] buildShapes() {
        VoxelShape[][] shapes = new VoxelShape[Anchor.values().length][LayerMath.MAX_LAYERS + 1];
        for (Anchor anchor : Anchor.values()) {
            for (int layers = LayerMath.MIN_LAYERS; layers <= LayerMath.MAX_LAYERS; layers++) {
                shapes[anchor.ordinal()][layers] = Block.box(0, LayerMath.minY(anchor.isCeiling(), layers), 0,
                        16, LayerMath.maxY(anchor.isCeiling(), layers), 16);
            }
        }
        return shapes;
    }

    private static VoxelShape shapeOf(BlockState state) {
        return SHAPES[state.getValue(ANCHOR).ordinal()][state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    /**
     * Matches vanilla SnowLayerBlock's pattern: LAND is walkable below a
     * height threshold scaled to this block's 16-layer granularity (vanilla
     * snow's {@code < 5} of 8 layers is {@code < 8} of 16 here); WATER and
     * AIR are never pathfindable through a LayeredBlock. Without this
     * override, the inherited default treats any non-full-block shape as
     * pathfindable, which resolves most of a smoothed slope to
     * PathType.OPEN instead of WALKABLE and freezes ground mobs.
     */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return switch (pathComputationType) {
            case LAND -> LayerMath.isWalkable(state.getValue(LAYERS));
            case WATER, AIR -> false;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, ANCHOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) {
            return existing.setValue(LAYERS, LayerMath.addLayer(existing.getValue(LAYERS)));
        }
        Anchor anchor = context.getClickedFace() == Direction.DOWN ? Anchor.CEILING : Anchor.FLOOR;
        return defaultBlockState().setValue(ANCHOR, anchor);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!context.getItemInHand().is(asItem()) || !LayerMath.canAddLayer(state.getValue(LAYERS))) {
            return false;
        }
        if (!context.replacingClickedOnBlock()) {
            return true;
        }
        Direction growing = state.getValue(ANCHOR).isCeiling() ? Direction.DOWN : Direction.UP;
        return context.getClickedFace() == growing;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this, state.getValue(LAYERS)));
    }
}
