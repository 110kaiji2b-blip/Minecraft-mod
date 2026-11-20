package com.swiftskyblock.registry;

import com.swiftskyblock.SwiftSkyblockMod;
import com.swiftskyblock.block.AccelerationLanternBlock;
import com.swiftskyblock.block.AngelBlock;
import com.swiftskyblock.block.AutoCobbleGenBlock;
import com.swiftskyblock.block.CompostBarrelBlock;
import com.swiftskyblock.block.DustBlock;
import com.swiftskyblock.block.OmniSynthesizerBlock;
import com.swiftskyblock.block.RapidCrucibleBlock;
import com.swiftskyblock.block.RapidSieveBlock;
import com.swiftskyblock.block.TurboFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SwiftSkyblockMod.MOD_ID);

    public static final RegistryObject<Block> DUST_BLOCK = BLOCKS.register("dust_block",
            () -> new DustBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(0.4f)
                    .sound(SoundType.SAND)));

    public static final RegistryObject<Block> COMPRESSED_COBBLESTONE = BLOCKS.register("compressed_cobblestone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.5f, 8.0f)
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Block> DOUBLE_COMPRESSED_COBBLESTONE = BLOCKS.register("double_compressed_cobblestone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.5f, 12.0f)
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Block> COMPRESSED_GRAVEL = BLOCKS.register("compressed_gravel",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.0f)
                    .sound(SoundType.GRAVEL)));

    public static final RegistryObject<Block> COMPRESSED_SAND = BLOCKS.register("compressed_sand",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(1.0f)
                    .sound(SoundType.SAND)));

    public static final RegistryObject<Block> RAPID_SIEVE = BLOCKS.register("rapid_sieve",
            () -> new RapidSieveBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> COMPOST_BARREL = BLOCKS.register("compost_barrel",
            () -> new CompostBarrelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> COBBLE_GEN_TIER1 = BLOCKS.register("cobble_gen_tier1",
            () -> new AutoCobbleGenBlock(1, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(s -> 6)));

    public static final RegistryObject<Block> COBBLE_GEN_TIER2 = BLOCKS.register("cobble_gen_tier2",
            () -> new AutoCobbleGenBlock(2, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0f, 8.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> 9)));

    public static final RegistryObject<Block> COBBLE_GEN_TIER3 = BLOCKS.register("cobble_gen_tier3",
            () -> new AutoCobbleGenBlock(3, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DIAMOND)
                    .strength(3.5f, 10.0f)
                    .sound(SoundType.METAL)
                    .lightLevel(s -> 12)));

    public static final RegistryObject<Block> TURBO_FURNACE = BLOCKS.register("turbo_furnace",
            () -> new TurboFurnaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 14 : 0)));

    public static final RegistryObject<Block> RAPID_CRUCIBLE = BLOCKS.register("rapid_crucible",
            () -> new RapidCrucibleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(2.0f, 6.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(RapidCrucibleBlock.HAS_LAVA) ? 13 : 0)));

    public static final RegistryObject<Block> ACCELERATION_LANTERN = BLOCKS.register("acceleration_lantern",
            () -> new AccelerationLanternBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(1.5f)
                    .sound(SoundType.LANTERN)
                    .noOcclusion()
                    .lightLevel(s -> 15)));

    public static final RegistryObject<Block> OMNI_SYNTHESIZER = BLOCKS.register("omni_synthesizer",
            () -> new OmniSynthesizerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(4.0f, 12.0f)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(s -> 10)));

    public static final RegistryObject<Block> ANGEL_BLOCK = BLOCKS.register("angel_block",
            () -> new AngelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .instabreak()
                    .sound(SoundType.AMETHYST)
                    .lightLevel(s -> 8)));
}
