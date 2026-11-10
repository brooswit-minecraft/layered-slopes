package io.github.brooswitminecraft.layeredslopes;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GrassColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Only compiled with -PclientAcceptance, never part of a normal release. Separate test JVM from
 * {@link AcceptanceServer}; proves two things a server-only GameTest cannot: the real client
 * actually classloads with Layered Slopes (reaching "joined the game" at all is that proof), and
 * {@link ClientEvents}' grass-tint color handler actually fires at render time against a real
 * block/level/pos rather than falling back to the no-level default branch.
 */
@EventBusSubscriber(modid = LayeredSlopesMod.MODID, value = Dist.CLIENT)
public final class AcceptanceClient {
    private static int ticks;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        // Give chunks/biomes time to load around the player before sampling the tint.
        if (ticks != 60) return;
        try {
            var pos = new BlockPos(2, 100, 0);
            var state = mc.level.getBlockState(pos);
            if (state.getBlock() != LayeredSlopesMod.LAYERED_GRASS.get()) {
                throw new IllegalStateException(
                        "LAYERED_GRASS never resolved client-side at the prepared position: " + state);
            }
            int color = mc.getBlockColors().getColor(state, mc.level, pos, 0);
            int defaultColor = GrassColor.getDefaultColor();
            if (color == defaultColor) {
                throw new IllegalStateException(
                        "grass tint handler returned the no-level default color (0x"
                                + Integer.toHexString(defaultColor) + ") instead of a real biome color");
            }
            int green = (color >> 8) & 0xFF, red = (color >> 16) & 0xFF, blue = color & 0xFF;
            if (green <= red && green <= blue) {
                throw new IllegalStateException(
                        "resolved tint is not green-dominant, unlikely to be a real grass color: 0x"
                                + Integer.toHexString(color));
            }
            var dirtState = mc.level.getBlockState(new BlockPos(0, 100, 0));
            if (dirtState.getBlock() != LayeredSlopesMod.LAYERED_DIRT.get()) {
                throw new IllegalStateException(
                        "LAYERED_DIRT never resolved client-side at the prepared position: " + dirtState);
            }
            var stoneState = mc.level.getBlockState(new BlockPos(4, 100, 0));
            if (stoneState.getBlock() != LayeredSlopesMod.LAYERED_STONE.get()) {
                throw new IllegalStateException(
                        "LAYERED_STONE never resolved client-side at the prepared position: " + stoneState);
            }
            Files.writeString(Path.of("client-acceptance-result.json"),
                    "{\"passed\":true,\"client_classloading\":true,\"grass_tint_rendered\":true,"
                            + "\"tint_color\":\"0x" + Integer.toHexString(color) + "\"}");
            LayeredSlopesMod.LOGGER.info("LAYERED_ACCEPTANCE_PASS color=0x{}", Integer.toHexString(color));
            mc.stop();
        } catch (Exception failure) {
            LayeredSlopesMod.LOGGER.error("LAYERED_ACCEPTANCE_FAIL", failure);
            try {
                Files.writeString(Path.of("client-acceptance-failure.txt"), failure.toString());
            } catch (Exception ignored) {
                // best-effort evidence only
            }
            mc.stop();
        }
    }
}
