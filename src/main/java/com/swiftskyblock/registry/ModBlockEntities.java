package com.swiftskyblock.registry;

import com.swiftskyblock.SwiftSkyblockMod;
import com.swiftskyblock.blockentity.AccelerationLanternBlockEntity;
import com.swiftskyblock.blockentity.AutoCobbleGenBlockEntity;
import com.swiftskyblock.blockentity.CompostBarrelBlockEntity;
import com.swiftskyblock.blockentity.RapidCrucibleBlockEntity;
import com.swiftskyblock.blockentity.RapidSieveBlockEntity;
import com.swiftskyblock.blockentity.TurboFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SwiftSkyblockMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<RapidSieveBlockEntity>> RAPID_SIEVE =
            BLOCK_ENTITIES.register("rapid_sieve", () ->
                    BlockEntityType.Builder.of(RapidSieveBlockEntity::new, ModBlocks.RAPID_SIEVE.get()).build(null));

    public static final RegistryObject<BlockEntityType<CompostBarrelBlockEntity>> COMPOST_BARREL =
            BLOCK_ENTITIES.register("compost_barrel", () ->
                    BlockEntityType.Builder.of(CompostBarrelBlockEntity::new, ModBlocks.COMPOST_BARREL.get()).build(null));

    public static final RegistryObject<BlockEntityType<AutoCobbleGenBlockEntity>> AUTO_COBBLE_GEN =
            BLOCK_ENTITIES.register("auto_cobble_gen", () ->
                    BlockEntityType.Builder.of(AutoCobbleGenBlockEntity::new,
                            ModBlocks.COBBLE_GEN_TIER1.get(),
                            ModBlocks.COBBLE_GEN_TIER2.get(),
                            ModBlocks.COBBLE_GEN_TIER3.get()).build(null));

    public static final RegistryObject<BlockEntityType<TurboFurnaceBlockEntity>> TURBO_FURNACE =
            BLOCK_ENTITIES.register("turbo_furnace", () ->
                    BlockEntityType.Builder.of(TurboFurnaceBlockEntity::new, ModBlocks.TURBO_FURNACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<RapidCrucibleBlockEntity>> RAPID_CRUCIBLE =
            BLOCK_ENTITIES.register("rapid_crucible", () ->
                    BlockEntityType.Builder.of(RapidCrucibleBlockEntity::new, ModBlocks.RAPID_CRUCIBLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<AccelerationLanternBlockEntity>> ACCELERATION_LANTERN =
            BLOCK_ENTITIES.register("acceleration_lantern", () ->
                    BlockEntityType.Builder.of(AccelerationLanternBlockEntity::new, ModBlocks.ACCELERATION_LANTERN.get()).build(null));
}
