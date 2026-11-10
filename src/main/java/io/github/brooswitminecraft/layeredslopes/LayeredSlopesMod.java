package io.github.brooswitminecraft.layeredslopes;

import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entry point and registry for the layered block family, extracted from Dynamic Terrain
 * (MINECRAFT-499). Registry names keep the pre-split {@code dynamicterrain} namespace
 * (not {@value #MODID}) so existing saved worlds, data packs and tags that reference e.g.
 * {@code dynamicterrain:layered_dirt} keep resolving unchanged once this mod provides it
 * instead of Dynamic Terrain -- same compatibility convention Courier Craft established
 * for the Dynamic Vehicles split (MINECRAFT-493).
 */
@Mod(LayeredSlopesMod.MODID)
public class LayeredSlopesMod {
    public static final String MODID = "layeredslopes";
    /** Legacy registry namespace preserved for save/data-pack compatibility; see class doc. */
    public static final String LEGACY_NAMESPACE = "dynamicterrain";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LEGACY_NAMESPACE);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LEGACY_NAMESPACE);

    public static final DeferredBlock<LayeredBlock> LAYERED_DIRT = registerLayered("layered_dirt", Blocks.DIRT);
    public static final DeferredBlock<LayeredBlock> LAYERED_SAND = registerLayered("layered_sand", Blocks.SAND);
    public static final DeferredBlock<LayeredBlock> LAYERED_GRAVEL = registerLayered("layered_gravel", Blocks.GRAVEL);

    public static final DeferredBlock<LayeredBlock> LAYERED_STONE = registerLayered("layered_stone", Blocks.STONE);
    public static final DeferredBlock<LayeredBlock> LAYERED_COBBLESTONE = registerLayered("layered_cobblestone", Blocks.COBBLESTONE);
    public static final DeferredBlock<LayeredBlock> LAYERED_SMOOTH_STONE = registerLayered("layered_smooth_stone", Blocks.SMOOTH_STONE);
    public static final DeferredBlock<LayeredBlock> LAYERED_DEEPSLATE = registerLayered("layered_deepslate", Blocks.DEEPSLATE);
    public static final DeferredBlock<LayeredBlock> LAYERED_COBBLED_DEEPSLATE = registerLayered("layered_cobbled_deepslate", Blocks.COBBLED_DEEPSLATE);
    public static final DeferredBlock<LayeredBlock> LAYERED_TERRACOTTA = registerLayered("layered_terracotta", Blocks.TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_RED_TERRACOTTA = registerLayered("layered_red_terracotta", Blocks.RED_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_ORANGE_TERRACOTTA = registerLayered("layered_orange_terracotta", Blocks.ORANGE_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_YELLOW_TERRACOTTA = registerLayered("layered_yellow_terracotta", Blocks.YELLOW_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_BROWN_TERRACOTTA = registerLayered("layered_brown_terracotta", Blocks.BROWN_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_WHITE_TERRACOTTA = registerLayered("layered_white_terracotta", Blocks.WHITE_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_LIGHT_GRAY_TERRACOTTA = registerLayered("layered_light_gray_terracotta", Blocks.LIGHT_GRAY_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_MAGENTA_TERRACOTTA = registerLayered("layered_magenta_terracotta", Blocks.MAGENTA_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_LIGHT_BLUE_TERRACOTTA = registerLayered("layered_light_blue_terracotta", Blocks.LIGHT_BLUE_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_LIME_TERRACOTTA = registerLayered("layered_lime_terracotta", Blocks.LIME_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_PINK_TERRACOTTA = registerLayered("layered_pink_terracotta", Blocks.PINK_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_GRAY_TERRACOTTA = registerLayered("layered_gray_terracotta", Blocks.GRAY_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_CYAN_TERRACOTTA = registerLayered("layered_cyan_terracotta", Blocks.CYAN_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_PURPLE_TERRACOTTA = registerLayered("layered_purple_terracotta", Blocks.PURPLE_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_BLUE_TERRACOTTA = registerLayered("layered_blue_terracotta", Blocks.BLUE_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_GREEN_TERRACOTTA = registerLayered("layered_green_terracotta", Blocks.GREEN_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_BLACK_TERRACOTTA = registerLayered("layered_black_terracotta", Blocks.BLACK_TERRACOTTA);
    public static final DeferredBlock<LayeredBlock> LAYERED_RED_SAND = registerLayered("layered_red_sand", Blocks.RED_SAND);
    public static final DeferredBlock<LayeredBlock> LAYERED_ICE = registerLayered("layered_ice", Blocks.ICE);
    public static final DeferredBlock<LayeredBlock> LAYERED_PACKED_ICE = registerLayered("layered_packed_ice", Blocks.PACKED_ICE);
    public static final DeferredBlock<LayeredBlock> LAYERED_SNOW_BLOCK = registerLayered("layered_snow_block", Blocks.SNOW_BLOCK);
    public static final DeferredBlock<LayeredBlock> LAYERED_COARSE_DIRT = registerLayered("layered_coarse_dirt", Blocks.COARSE_DIRT);
    public static final DeferredBlock<LayeredBlock> LAYERED_PODZOL = registerLayered("layered_podzol", Blocks.PODZOL);
    public static final DeferredBlock<LayeredBlock> LAYERED_MYCELIUM = registerLayered("layered_mycelium", Blocks.MYCELIUM);
    public static final DeferredBlock<LayeredBlock> LAYERED_MUD = registerLayered("layered_mud", Blocks.MUD);
    public static final DeferredBlock<LayeredBlock> LAYERED_CLAY = registerLayered("layered_clay", Blocks.CLAY);
    public static final DeferredBlock<LayeredBlock> LAYERED_SANDSTONE = registerLayered("layered_sandstone", Blocks.SANDSTONE);
    public static final DeferredBlock<LayeredBlock> LAYERED_RED_SANDSTONE = registerLayered("layered_red_sandstone", Blocks.RED_SANDSTONE);
    public static final DeferredBlock<LayeredBlock> LAYERED_TUFF = registerLayered("layered_tuff", Blocks.TUFF);
    public static final DeferredBlock<LayeredBlock> LAYERED_ANDESITE = registerLayered("layered_andesite", Blocks.ANDESITE);
    public static final DeferredBlock<LayeredBlock> LAYERED_GRANITE = registerLayered("layered_granite", Blocks.GRANITE);
    public static final DeferredBlock<LayeredBlock> LAYERED_DIORITE = registerLayered("layered_diorite", Blocks.DIORITE);
    public static final DeferredBlock<LayeredBlock> LAYERED_NETHERRACK = registerLayered("layered_netherrack", Blocks.NETHERRACK);
    public static final DeferredBlock<LayeredBlock> LAYERED_BLACKSTONE = registerLayered("layered_blackstone", Blocks.BLACKSTONE);

    public static final DeferredBlock<LayeredBlock> LAYERED_GRASS = BLOCKS.registerBlock("layered_grass",
            LayeredGrassBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).noOcclusion());

    private static final List<DeferredItem<BlockItem>> LAYERED_ITEMS = List.of(
            ITEMS.registerSimpleBlockItem(LAYERED_DIRT),
            ITEMS.registerSimpleBlockItem(LAYERED_GRASS),
            ITEMS.registerSimpleBlockItem(LAYERED_STONE),
            ITEMS.registerSimpleBlockItem(LAYERED_COBBLESTONE),
            ITEMS.registerSimpleBlockItem(LAYERED_SMOOTH_STONE),
            ITEMS.registerSimpleBlockItem(LAYERED_DEEPSLATE),
            ITEMS.registerSimpleBlockItem(LAYERED_COBBLED_DEEPSLATE),
            ITEMS.registerSimpleBlockItem(LAYERED_SAND),
            ITEMS.registerSimpleBlockItem(LAYERED_GRAVEL),
            ITEMS.registerSimpleBlockItem(LAYERED_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_RED_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_ORANGE_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_YELLOW_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_BROWN_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_WHITE_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_LIGHT_GRAY_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_MAGENTA_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_LIGHT_BLUE_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_LIME_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_PINK_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_GRAY_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_CYAN_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_PURPLE_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_BLUE_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_GREEN_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_BLACK_TERRACOTTA),
            ITEMS.registerSimpleBlockItem(LAYERED_RED_SAND),
            ITEMS.registerSimpleBlockItem(LAYERED_ICE),
            ITEMS.registerSimpleBlockItem(LAYERED_PACKED_ICE),
            ITEMS.registerSimpleBlockItem(LAYERED_SNOW_BLOCK),
            ITEMS.registerSimpleBlockItem(LAYERED_COARSE_DIRT),
            ITEMS.registerSimpleBlockItem(LAYERED_PODZOL),
            ITEMS.registerSimpleBlockItem(LAYERED_MYCELIUM),
            ITEMS.registerSimpleBlockItem(LAYERED_MUD),
            ITEMS.registerSimpleBlockItem(LAYERED_CLAY),
            ITEMS.registerSimpleBlockItem(LAYERED_SANDSTONE),
            ITEMS.registerSimpleBlockItem(LAYERED_RED_SANDSTONE),
            ITEMS.registerSimpleBlockItem(LAYERED_TUFF),
            ITEMS.registerSimpleBlockItem(LAYERED_ANDESITE),
            ITEMS.registerSimpleBlockItem(LAYERED_GRANITE),
            ITEMS.registerSimpleBlockItem(LAYERED_DIORITE),
            ITEMS.registerSimpleBlockItem(LAYERED_NETHERRACK),
            ITEMS.registerSimpleBlockItem(LAYERED_BLACKSTONE));

    private static DeferredBlock<LayeredBlock> registerLayered(String name, Block base) {
        return BLOCKS.registerBlock(name, LayeredBlock::new,
                BlockBehaviour.Properties.ofFullCopy(base).noOcclusion());
    }

    public LayeredSlopesMod(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addCreative);
        LOGGER.info("Layered Slopes loaded");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            LAYERED_ITEMS.forEach(event::accept);
        }
    }
}
