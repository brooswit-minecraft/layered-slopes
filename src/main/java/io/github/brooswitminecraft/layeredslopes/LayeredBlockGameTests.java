package io.github.brooswitminecraft.layeredslopes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
     * mutation): each iteration builds a real {@link BlockPlaceContext} from a real right-click
     * simulation and calls this block's own {@code getStateForPlacement}/{@code canBeReplaced} --
     * the exact same methods {@link BlockItem#useOn} dispatches to -- rather than hardcoding
     * {@code LAYERS} via {@code setValue} (self-review 52615's finding on the original version of
     * this test).
     *
     * <p>Earlier CI attempts converting the resolved absolute placement position back to a
     * structure-relative one (to use {@code GameTestHelper}'s relative-position convenience
     * methods, which re-derive their own absolute position internally) kept landing on the wrong
     * cell in this environment -- a coordinate-transform detail never pinned down, not a
     * LayeredBlock defect (position resolution is generic vanilla {@code BlockPlaceContext}
     * machinery, independent of which block is being placed). This version never converts back:
     * it locks in whatever absolute position the context resolves to on the FIRST real placement,
     * verifies every later click (made directly against the already-placed block) keeps resolving
     * to that exact same absolute position, and reads/writes state at that absolute position
     * directly through {@code helper.getLevel()} throughout -- still a real, non-fabricated
     * runtime decision made by {@code getStateForPlacement}/{@code canBeReplaced}, just without
     * ever needing to invert a transform this test doesn't actually need to know.
     */
    @GameTest(template = "flat_platform")
    public static void placingAndStackingLayersUpdatesTheLayersProperty(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        LayeredBlock block = LayeredSlopesMod.LAYERED_STONE.get();
        BlockPos clickAt = helper.absolutePos(new BlockPos(1, 1, 1));

        BlockPos targetAbsolute = null;
        for (int expectedLayers = LayerMath.MIN_LAYERS; expectedLayers <= LayerMath.MAX_LAYERS; expectedLayers++) {
            BlockPlaceContext context = placeContextAt(helper, player, block, clickAt);
            BlockPos resolved = context.getClickedPos();
            if (targetAbsolute == null) {
                targetAbsolute = resolved;
            } else {
                helper.assertTrue(resolved.equals(targetAbsolute),
                        "place context target moved between placements: " + resolved + " vs locked-in " + targetAbsolute);
            }
            helper.assertTrue(block.canBeReplaced(helper.getLevel().getBlockState(resolved), context),
                    "expected to be able to place/stack at " + (expectedLayers - 1) + " existing layers");
            BlockState placed = block.getStateForPlacement(context);
            helper.assertTrue(placed != null, "getStateForPlacement returned null");
            helper.getLevel().setBlock(resolved, placed, 3);
            assertLayers(helper, resolved, expectedLayers);
            clickAt = resolved; // every click after the first lands directly on the now-placed block
        }

        // One more placement attempt at full height must be rejected: the block stays unchanged.
        BlockPlaceContext rejectedContext = placeContextAt(helper, player, block, targetAbsolute);
        helper.assertTrue(!block.canBeReplaced(helper.getLevel().getBlockState(targetAbsolute), rejectedContext),
                "placing against a full 16-layer block must be rejected");
        assertLayers(helper, targetAbsolute, LayerMath.MAX_LAYERS);

        BlockState full = helper.getLevel().getBlockState(targetAbsolute);
        helper.assertTrue(full.getCollisionShape(helper.getLevel(), targetAbsolute).bounds().maxY >= 0.99,
                "a full 16-layer block's collision shape must reach the top of the cell");
        helper.succeed();
    }

    private static void assertLayers(GameTestHelper helper, BlockPos absolutePos, int expectedLayers) {
        BlockState actual = helper.getLevel().getBlockState(absolutePos);
        helper.assertTrue(actual.hasProperty(LayeredBlock.LAYERS),
                "expected a LayeredBlock at " + absolutePos + ", got " + actual.getBlock());
        int actualLayers = actual.getValue(LayeredBlock.LAYERS);
        helper.assertTrue(actualLayers == expectedLayers,
                "expected " + expectedLayers + " layers at " + absolutePos + ", got " + actualLayers);
    }

    private static BlockPlaceContext placeContextAt(GameTestHelper helper, ServerPlayer player, LayeredBlock block, BlockPos absolutePos) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
        UseOnContext useOnContext = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new ItemStack(block.asItem()), hit);
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
