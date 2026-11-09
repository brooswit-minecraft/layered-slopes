package io.github.brooswitminecraft.layeredslopes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * MINECRAFT-499 acceptance evidence: real runtime proof (not just compile-time) that the
 * extracted layered block family loads and behaves correctly standalone, under this mod's own
 * {@code dynamicterrain} legacy registry namespace. {@code ci.yml}'s "Run GameTests" step runs
 * {@code ./gradlew runGameTestServer}, which boots a real NeoForge server with only this mod
 * installed -- the run succeeding at all is itself the standalone-load/classloading evidence;
 * {@code scripts/check_gametest_count.py} then fails closed unless every test below is named as
 * having actually run and passed, so a silently-empty or crashed run cannot read as success.
 */
@GameTestHolder(LayeredSlopesMod.MODID)
@PrefixGameTestTemplate(false)
public final class LayeredBlockGameTests {
    private LayeredBlockGameTests() {}

    /**
     * Placement and stacking through the REAL decision-making methods (not a direct state
     * mutation): calls this block's own {@code getStateForPlacement}/{@code canBeReplaced} --
     * the exact same methods {@link BlockItem#useOn} dispatches to -- rather than hardcoding
     * {@code LAYERS} via {@code setValue} (self-review 52615's finding on the original version of
     * this test).
     *
     * <p>Root cause of every earlier attempt's failure, finally pinned down with John's CI
     * diagnostic on run 42665888135's full stack trace: {@code DirectionalPlaceContext} (the
     * class vanilla's dispenser-placing-a-block behavior uses) overrides
     * {@code replacingClickedOnBlock()} to COMPUTE it by calling {@code canPlace()}, which calls
     * {@code state.canBeReplaced(this)} -- our override -- which calls
     * {@code context.replacingClickedOnBlock()} again: infinite recursion, StackOverflowError,
     * for any block (like this one, or vanilla's snow/candle-style blocks) whose
     * {@code canBeReplaced} consults that flag. Regular {@code BlockPlaceContext} has no such
     * cycle: {@code replacingClickedOnBlock()} there just returns a plain field computed ONCE in
     * the constructor (which, for this specific block, happens to settle on {@code true} because
     * the field reads back {@code false} by Java's own default-before-assignment semantics during
     * that one-time computation, which makes our override's early-return branch fire and the
     * field end up {@code true}) -- never calling back into our override, so no cycle. Switching
     * the context-construction API, not reverting to direct state mutation, is the actual fix.
     *
     * <p>Each of the three scenarios below (fresh placement, mid-stack, full) sets up its OWN
     * starting state directly via {@code helper.setBlock} -- legitimate test fixture setup, not
     * the thing under test.
     */
    @GameTest(template = "flat_platform")
    public static void placingAndStackingLayersUpdatesTheLayersProperty(GameTestHelper helper) {
        LayeredBlock block = LayeredSlopesMod.LAYERED_STONE.get();
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(pos);
        ItemStack stack = new ItemStack(block.asItem());

        // Scenario 1: fresh placement onto air starts at 1 layer.
        BlockPlaceContext airContext = placeContextAt(helper, stack, absolute);
        helper.assertTrue(airContext.getClickedPos().equals(absolute),
                "placement context over air should resolve to the clicked cell, got " + airContext.getClickedPos());
        BlockState firstPlaced = block.getStateForPlacement(airContext);
        helper.assertTrue(firstPlaced != null && firstPlaced.getValue(LayeredBlock.LAYERS) == LayerMath.MIN_LAYERS,
                "fresh placement over air should start at " + LayerMath.MIN_LAYERS + " layer(s)");

        // Scenario 2: clicking an existing, non-full layered block grows it by one.
        helper.setBlock(pos, block.defaultBlockState().setValue(LayeredBlock.LAYERS, 7));
        BlockPlaceContext stackContext = placeContextAt(helper, stack, absolute);
        helper.assertTrue(stackContext.getClickedPos().equals(absolute),
                "placement context over an existing layered block should resolve to the clicked cell, got " + stackContext.getClickedPos());
        BlockState existingSeven = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(block.canBeReplaced(existingSeven, stackContext), "7 existing layers should allow stacking one more");
        BlockState stacked = block.getStateForPlacement(stackContext);
        helper.assertTrue(stacked.getValue(LayeredBlock.LAYERS) == 8,
                "expected 8 layers after stacking onto 7, got " + stacked.getValue(LayeredBlock.LAYERS));

        // Scenario 3: a full 16-layer block rejects further stacking.
        helper.setBlock(pos, block.defaultBlockState().setValue(LayeredBlock.LAYERS, LayerMath.MAX_LAYERS));
        BlockPlaceContext fullContext = placeContextAt(helper, stack, absolute);
        BlockState existingFull = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(!block.canBeReplaced(existingFull, fullContext), "a full 16-layer block must reject further stacking");

        BlockState full = helper.getBlockState(pos);
        helper.assertTrue(full.getCollisionShape(helper.getLevel(), absolute).bounds().maxY >= 0.99,
                "a full 16-layer block's collision shape must reach the top of the cell");
        helper.succeed();
    }

    private static BlockPlaceContext placeContextAt(GameTestHelper helper, ItemStack stack, BlockPos absolutePos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
        UseOnContext useOnContext = new UseOnContext(helper.getLevel(), null, InteractionHand.MAIN_HAND, stack, hit);
        return new BlockPlaceContext(useOnContext);
    }

    /** Drops: breaking a layered block drops exactly as many items as it had layers. */
    @GameTest(template = "flat_platform")
    public static void breakingALayeredBlockDropsOneItemPerLayer(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        int layers = 5;
        helper.setBlock(pos, LayeredSlopesMod.LAYERED_STONE.get().defaultBlockState()
                .setValue(LayeredBlock.LAYERS, layers));
        // helper.destroyBlock(pos) passes dropBlock=false (it's meant for test-prep cleanup, not
        // simulating a player mining); call the level directly with dropBlock=true to actually
        // get loot, confirmed live by the first CI run of this test: "got 0" with destroyBlock.
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);

        helper.succeedWhen(() -> {
            BlockPos absolute = helper.absolutePos(pos);
            int totalDropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            new AABB(absolute).inflate(1.5))
                    .stream()
                    .filter(e -> e.getItem().is(LayeredSlopesMod.LAYERED_STONE.get().asItem()))
                    .mapToInt(e -> e.getItem().getCount())
                    .sum();
            helper.assertTrue(totalDropped == layers,
                    "expected " + layers + " dropped layered-stone items, got " + totalDropped);
        });
    }

    /** Compatibility contract (MINECRAFT-499): the pre-split registry name must still resolve, now to this mod's block. */
    @GameTest(template = "flat_platform")
    public static void legacyDynamicterrainRegistryNameStillResolves(GameTestHelper helper) {
        ResourceLocation legacyId = ResourceLocation.fromNamespaceAndPath(
                LayeredSlopesMod.LEGACY_NAMESPACE, "layered_dirt");
        Block resolved = BuiltInRegistries.BLOCK.get(legacyId);
        helper.assertTrue(resolved == LayeredSlopesMod.LAYERED_DIRT.get(),
                "dynamicterrain:layered_dirt must resolve to this mod's LAYERED_DIRT for existing saves to keep working");
        helper.assertTrue(resolved instanceof LayeredBlock, "the resolved block must actually be a LayeredBlock");
        helper.succeed();
    }
}
