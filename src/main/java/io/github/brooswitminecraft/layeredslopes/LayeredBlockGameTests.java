package io.github.brooswitminecraft.layeredslopes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
     * mutation): each click builds a real {@link BlockPlaceContext} from a real right-click
     * simulation and calls this block's own {@code getStateForPlacement}/{@code canBeReplaced} --
     * the exact same methods {@link BlockItem#useOn} dispatches to -- rather than hardcoding
     * {@code LAYERS} via {@code setValue} (self-review 52615's finding on the original version).
     *
     * <p>Root cause of the recursion in earlier attempts (John's CI diagnostic on run
     * 42665888135's full stack trace): {@code DirectionalPlaceContext} overrides
     * {@code replacingClickedOnBlock()} to COMPUTE it by calling {@code canPlace()}, which calls
     * {@code state.canBeReplaced(this)} -- our override -- which calls
     * {@code context.replacingClickedOnBlock()} again. Regular {@code BlockPlaceContext} has no
     * such cycle: that flag is a plain field set once in the constructor.
     *
     * <p>Root cause of the position mismatch in every attempt, confirmed with real logged data
     * (not a guessed offset, per John's 52644) on run 42667344681: clicking an AIR cell directly
     * does NOT resolve the placement target to that same cell -- {@code BlockPlaceContext}
     * resolves it to {@code hitPos.relative(clickedFace)}, a clean single-axis shift, meaning air
     * is not treated as "replace in place" at construction. This is exactly the real interaction a
     * player performs anyway: to place the FIRST layer you click the solid floor BELOW the empty
     * cell, face up, and the game places the new block in the cell above. Every later click (the
     * cell now holds our own LayeredBlock, which {@code canBeReplaced} genuinely allows replacing
     * in place) targets the cell itself directly, face up, with no shift.
     */
    @GameTest(template = "flat_platform")
    public static void placingAndStackingLayersUpdatesTheLayersProperty(GameTestHelper helper) {
        LayeredBlock block = LayeredSlopesMod.LAYERED_STONE.get();
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(pos);
        ItemStack stack = new ItemStack(block.asItem());

        // Explicitly establish air before the fresh-placement scenario (John's 52644): don't
        // assume the template leaves this cell empty, assert it.
        helper.setBlock(pos, Blocks.AIR.defaultBlockState());
        log(helper, "scenario1-air", pos, absolute, helper.getLevel().getBlockState(absolute));

        // Scenario 1: click the solid floor below, face up -- the real interaction a player
        // performs to place the first layer. canBeReplaced/getStateForPlacement are called for
        // every one of the 16 layers below, not just once.
        BlockPos below = absolute.below();
        for (int expectedLayers = LayerMath.MIN_LAYERS; expectedLayers <= LayerMath.MAX_LAYERS; expectedLayers++) {
            BlockPos clickAt = expectedLayers == LayerMath.MIN_LAYERS ? below : absolute;
            BlockPlaceContext context = placeContextAt(helper, stack, clickAt);
            BlockState existing = helper.getLevel().getBlockState(context.getClickedPos());
            LayeredSlopesMod.LOGGER.info(
                    "layer {}: clickAt={} clickedPos={} replacingClickedOnBlock={} existing={}",
                    expectedLayers, clickAt, context.getClickedPos(), context.replacingClickedOnBlock(), existing);
            helper.assertTrue(context.getClickedPos().equals(absolute),
                    "layer " + expectedLayers + ": expected clicked pos " + absolute + ", got " + context.getClickedPos());
            if (expectedLayers > LayerMath.MIN_LAYERS) {
                helper.assertTrue(block.canBeReplaced(existing, context),
                        "layer " + expectedLayers + ": expected to be able to stack onto " + (expectedLayers - 1) + " existing layers");
            }
            BlockState placed = block.getStateForPlacement(context);
            helper.assertTrue(placed != null && placed.getValue(LayeredBlock.LAYERS) == expectedLayers,
                    "layer " + expectedLayers + ": expected getStateForPlacement to report " + expectedLayers
                            + " layers, got " + (placed == null ? "null" : placed.getValue(LayeredBlock.LAYERS)));
            helper.getLevel().setBlock(absolute, placed, 3);
        }

        // One more click at full height must be rejected: the block stays unchanged.
        BlockPlaceContext rejectedContext = placeContextAt(helper, stack, absolute);
        BlockState existingFull = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(!block.canBeReplaced(existingFull, rejectedContext),
                "a full 16-layer block must reject further stacking");
        helper.assertTrue(existingFull.getValue(LayeredBlock.LAYERS) == LayerMath.MAX_LAYERS,
                "the full block must stay at " + LayerMath.MAX_LAYERS + " layers after the rejected click");

        helper.assertTrue(existingFull.getCollisionShape(helper.getLevel(), absolute).bounds().maxY >= 0.99,
                "a full 16-layer block's collision shape must reach the top of the cell");
        helper.succeed();
    }

    private static void log(GameTestHelper helper, String label, BlockPos relative, BlockPos absolute, BlockState state) {
        LayeredSlopesMod.LOGGER.info("{}: relative={} absolute={} state={}", label, relative, absolute, state);
    }

    private static BlockPlaceContext placeContextAt(GameTestHelper helper, ItemStack stack, BlockPos hitPos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(hitPos), Direction.UP, hitPos, false);
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
