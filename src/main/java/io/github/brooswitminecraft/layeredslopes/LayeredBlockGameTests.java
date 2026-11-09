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
     * Placement and stacking through the REAL player-interaction path (not a direct state
     * mutation): {@link BlockItem#useOn} is exactly what a right-click dispatches to. Every click
     * targets the cell at {@code pos} directly, face UP: on an air cell that's always replaceable
     * (so {@code BlockPlaceContext} keeps the target at the clicked position instead of shifting
     * to a neighbor), and once a layered block occupies it, {@code canBeReplaced}'s own
     * {@code context.replacingClickedOnBlock()}/{@code getClickedFace()} checks take over --
     * exactly the "click straight down onto a growing snow-layer-style stack" interaction a player
     * actually performs. Growing by one layer each time up to a full block, where a further click
     * must be rejected (self-review 52615: a prior version of this test set {@code LAYERS} directly
     * via {@code setBlock} and never actually called the placement path).
     */
    @GameTest(template = "flat_platform")
    public static void placingAndStackingLayersUpdatesTheLayersProperty(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockItem item = (BlockItem) LayeredSlopesMod.LAYERED_STONE.get().asItem();
        BlockPos absolutePos = helper.absolutePos(pos);

        for (int expectedLayers = LayerMath.MIN_LAYERS; expectedLayers <= LayerMath.MAX_LAYERS; expectedLayers++) {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
            UseOnContext context = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                    new ItemStack(item), hit);
            InteractionResult result = item.useOn(context);
            helper.assertTrue(result == InteractionResult.CONSUME,
                    "right-clicking to place/stack at " + (expectedLayers - 1) + " existing layers should succeed, got " + result);
            helper.assertBlockProperty(pos, LayeredBlock.LAYERS, expectedLayers);
        }

        // One more click at full height must be rejected: the block stays unchanged.
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false);
        InteractionResult rejected = item.useOn(
                new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, new ItemStack(item), hit));
        helper.assertTrue(rejected != InteractionResult.CONSUME,
                "placing against a full 16-layer block must be rejected, got " + rejected);
        helper.assertBlockProperty(pos, LayeredBlock.LAYERS, LayerMath.MAX_LAYERS);

        BlockState full = helper.getBlockState(pos);
        helper.assertTrue(full.getCollisionShape(helper.getLevel(), pos).bounds().maxY >= 0.99,
                "a full 16-layer block's collision shape must reach the top of the cell");
        helper.succeed();
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
