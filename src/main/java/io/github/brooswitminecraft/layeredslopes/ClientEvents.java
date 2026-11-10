package io.github.brooswitminecraft.layeredslopes;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.GrassColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client-only: biome-tint the grass on layered grass, exactly like a vanilla grass block. */
@EventBusSubscriber(modid = LayeredSlopesMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> level != null && pos != null
                        ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor(),
                LayeredSlopesMod.LAYERED_GRASS.get());
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> GrassColor.getDefaultColor(), LayeredSlopesMod.LAYERED_GRASS.get());
    }
}
