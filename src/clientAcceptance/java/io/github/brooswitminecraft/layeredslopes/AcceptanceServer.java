package io.github.brooswitminecraft.layeredslopes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Only compiled with -PclientAcceptance, never part of a normal release. Prepares a small flat
 * platform with representative layered blocks -- including {@link LayeredSlopesMod#LAYERED_GRASS}
 * next to the spawn point, so the connecting client can observe its real render-time grass tint
 * (see {@link AcceptanceClient}).
 */
@EventBusSubscriber(modid = LayeredSlopesMod.MODID)
public final class AcceptanceServer {
    private static int ticks;

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var players = event.getServer().getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        var player = players.getFirst();
        var level = player.serverLevel();
        if (ticks++ == 0) {
            player.setInvulnerable(true);
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
                }
            }
            level.setBlock(new BlockPos(0, 100, 0),
                    LayeredSlopesMod.LAYERED_DIRT.get().defaultBlockState()
                            .setValue(LayeredBlock.LAYERS, 8), 3);
            level.setBlock(new BlockPos(2, 100, 0),
                    LayeredSlopesMod.LAYERED_GRASS.get().defaultBlockState()
                            .setValue(LayeredBlock.LAYERS, 8), 3);
            level.setBlock(new BlockPos(4, 100, 0),
                    LayeredSlopesMod.LAYERED_STONE.get().defaultBlockState()
                            .setValue(LayeredBlock.LAYERS, 4), 3);
            player.teleportTo(1, 101, 0);
            LayeredSlopesMod.LOGGER.info("LAYERED_ACCEPTANCE_SERVER_READY");
        }
        if (ticks == 100) {
            LayeredSlopesMod.LOGGER.info("LAYERED_ACCEPTANCE_SERVER_PASS");
        }
    }
}
