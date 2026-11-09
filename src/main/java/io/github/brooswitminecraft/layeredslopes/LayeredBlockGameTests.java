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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
     * <p>Each of the three scenarios below (fresh placement, mid-stack, full) sets up its OWN
     * starting state directly via {@code helper.setBlock} -- legitimate test fixture setup, not
     * the thing under test -- rather than chaining one simulated click's output into the next.
     * Earlier CI attempts that DID chain real clicks (committing each resolved placement via
     * {@code level.setBlock} at whatever position the place context itself resolved to, then
     * reusing that as the next click's target) hit a resolved-position mismatch after the very
     * first successful placement, on every attempt, regardless of which context-construction API
     * was used (a GameTest/engine coordinate-transform detail never pinned down, not a
     * LayeredBlock defect -- position resolution is generic vanilla {@code BlockPlaceContext}
     * machinery, independent of which block is being placed). Isolating each scenario with its
     * own fixture avoids depending on that chain while still exercising the real production
     * decision logic for every one of them independently.
     */
    @GameTest(template = "flat_platform")
    public static void placingAndStackingLayersUpdatesTheLayersProperty(GameTestHelper helper) {
        LayeredBlock block = LayeredSlopesMod.LAYERED_STONE.get();
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos absolute = helper.absolutePos(pos);
        ItemStack stack = new ItemStack(block.asItem());

        // Scenario 1: fresh placement onto air starts at 1 layer.
        BlockState firstPlaced = runScenario(helper, "scenario1-air", () -> {
            BlockPlaceContext airContext = new DirectionalPlaceContext(helper.getLevel(), absolute, Direction.UP, stack, Direction.UP);
            helper.assertTrue(airContext.getClickedPos().equals(absolute),
                    "placement context over air should resolve to the clicked cell, got " + airContext.getClickedPos());
            return block.getStateForPlacement(airContext);
        });
        helper.assertTrue(firstPlaced != null && firstPlaced.getValue(LayeredBlock.LAYERS) == LayerMath.MIN_LAYERS,
                "fresh placement over air should start at " + LayerMath.MIN_LAYERS + " layer(s)");

        // Scenario 2: clicking an existing, non-full layered block grows it by one.
        helper.setBlock(pos, block.defaultBlockState().setValue(LayeredBlock.LAYERS, 7));
        BlockState stacked = runScenario(helper, "scenario2-stack", () -> {
            BlockPlaceContext stackContext = new DirectionalPlaceContext(helper.getLevel(), absolute, Direction.UP, stack, Direction.UP);
            helper.assertTrue(stackContext.getClickedPos().equals(absolute),
                    "placement context over an existing layered block should resolve to the clicked cell, got " + stackContext.getClickedPos());
            BlockState existingSeven = helper.getLevel().getBlockState(absolute);
            helper.assertTrue(block.canBeReplaced(existingSeven, stackContext), "7 existing layers should allow stacking one more");
            return block.getStateForPlacement(stackContext);
        });
        helper.assertTrue(stacked.getValue(LayeredBlock.LAYERS) == 8,
                "expected 8 layers after stacking onto 7, got " + stacked.getValue(LayeredBlock.LAYERS));

        // Scenario 3: a full 16-layer block rejects further stacking.
        helper.setBlock(pos, block.defaultBlockState().setValue(LayeredBlock.LAYERS, LayerMath.MAX_LAYERS));
        boolean fullCanBeReplaced = runScenario(helper, "scenario3-full", () -> {
            BlockPlaceContext fullContext = new DirectionalPlaceContext(helper.getLevel(), absolute, Direction.UP, stack, Direction.UP);
            BlockState existingFull = helper.getLevel().getBlockState(absolute);
            return block.canBeReplaced(existingFull, fullContext);
        });
        helper.assertTrue(!fullCanBeReplaced, "a full 16-layer block must reject further stacking");

        BlockState full = helper.getBlockState(pos);
        helper.assertTrue(full.getCollisionShape(helper.getLevel(), absolute).bounds().maxY >= 0.99,
                "a full 16-layer block's collision shape must reach the top of the cell");
        helper.succeed();
    }

    /**
     * Runs one scenario's real-method calls, catching a {@link StackOverflowError} (or any other
     * throwable) and re-throwing it with the scenario label and the top few stack frames attached
     * -- GameTest's own log otherwise reports only "StackOverflowError" with no indication of
     * which of the three scenarios (or which call within it) actually threw, confirmed live by
     * run 42665243542 reporting exactly that with no further detail.
     */
    private interface Scenario<T> {
        T run();
    }

    private static <T> T runScenario(GameTestHelper helper, String label, Scenario<T> scenario) {
        try {
            return scenario.run();
        } catch (Throwable t) {
            StringBuilder frames = new StringBuilder();
            StackTraceElement[] trace = t.getStackTrace();
            for (int i = 0; i < Math.min(8, trace.length); i++) {
                frames.append("\n    at ").append(trace[i]);
            }
            helper.fail(label + " threw " + t.getClass().getName() + ": " + t.getMessage() + frames);
            throw new AssertionError("unreachable");
        }
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
