package io.github.brooswitminecraft.layeredslopes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
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

    /** Placement, stacking and shape/collision: layers grows by one per placement, up to a full block. */
    @GameTest(template = "flat_platform")
    public static void placingAndStackingLayersUpdatesTheLayersProperty(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockState first = LayeredSlopesMod.LAYERED_STONE.get().defaultBlockState();
        helper.setBlock(pos, first);
        helper.assertBlockProperty(pos, LayeredBlock.LAYERS, LayerMath.MIN_LAYERS);

        BlockState current = first;
        for (int layers = LayerMath.MIN_LAYERS; layers < LayerMath.MAX_LAYERS; layers++) {
            helper.assertTrue(LayerMath.canAddLayer(layers),
                    "LayerMath should allow adding another layer at " + layers + " layers");
            current = current.setValue(LayeredBlock.LAYERS, LayerMath.addLayer(layers));
            helper.setBlock(pos, current);
            helper.assertBlockProperty(pos, LayeredBlock.LAYERS, layers + 1);
        }

        BlockState full = helper.getBlockState(pos);
        helper.assertTrue(!LayerMath.canAddLayer(full.getValue(LayeredBlock.LAYERS)),
                "a full-height layered block must reject further layers");
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
        helper.destroyBlock(pos);

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
