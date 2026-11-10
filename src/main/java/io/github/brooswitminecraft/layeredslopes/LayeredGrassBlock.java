package io.github.brooswitminecraft.layeredslopes;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * Layered dirt with a grass top. Like a grass block, it drops dirt, not
 * itself. Also tracks vanilla's {@code snowy} property exactly like
 * {@link net.minecraft.world.level.block.SnowyDirtBlock}: floor-anchored
 * grass under a block in {@link BlockTags#SNOW} renders the frosted side
 * texture with no grass tint. Ceiling-anchored grass hangs with nothing
 * meaningfully "above" it, so it stays {@code snowy=false}.
 */
public class LayeredGrassBlock extends LayeredBlock {
    public static final MapCodec<LayeredGrassBlock> CODEC = simpleCodec(LayeredGrassBlock::new);
    public static final BooleanProperty SNOWY = BlockStateProperties.SNOWY;

    public LayeredGrassBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SNOWY, Boolean.FALSE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(LayeredSlopesMod.LAYERED_DIRT.get(), state.getValue(LAYERS)));
    }

    private static boolean isSnowySetting(BlockState above) {
        return above.is(BlockTags.SNOW);
    }

    /** Recompute {@code snowy} for {@code state} from whatever is currently above {@code pos}. */
    public BlockState withSnowy(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.getValue(ANCHOR).isCeiling()) {
            return state;
        }
        return state.setValue(SNOWY, isSnowySetting(level.getBlockState(pos.above())));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (direction == Direction.UP && !updated.getValue(ANCHOR).isCeiling()) {
            return updated.setValue(SNOWY, isSnowySetting(neighborState));
        }
        return updated;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withSnowy(context.getLevel(), context.getClickedPos(), super.getStateForPlacement(context));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SNOWY);
    }
}
