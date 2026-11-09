package io.github.brooswitminecraft.layeredslopes;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Entry point for Layered Slopes. Registers the mod with no gameplay logic;
 * add your own event listeners, registries, and config here.
 */
@Mod(LayeredSlopesMod.MODID)
public class LayeredSlopesMod {
    public static final String MODID = "layeredslopes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LayeredSlopesMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Layered Slopes loaded");
    }
}
