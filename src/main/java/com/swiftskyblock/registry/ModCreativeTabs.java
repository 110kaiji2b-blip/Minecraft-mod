package com.swiftskyblock.registry;

import com.swiftskyblock.SwiftSkyblockMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SwiftSkyblockMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SWIFTSKYBLOCK_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.swiftskyblock"))
                    .icon(() -> new ItemStack(ModBlocks.RAPID_SIEVE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.SKYBLOCK_GUIDE.get());
                        output.accept(ModItems.SKY_MAGNET.get());
                        output.accept(ModItems.TIME_POCKET_WATCH.get());
                        output.accept(ModItems.STONE_PEBBLE.get());
                        output.accept(ModItems.WOODEN_HAMMER.get());
                        output.accept(ModItems.STONE_HAMMER.get());
                        output.accept(ModItems.IRON_HAMMER.get());
                        output.accept(ModItems.DIAMOND_HAMMER.get());
                        output.accept(ModItems.NETHERITE_HAMMER.get());
                        output.accept(ModItems.RAPID_SIEVE_ITEM.get());
                        output.accept(ModItems.COMPOST_BARREL_ITEM.get());
                        output.accept(ModItems.COBBLE_GEN_TIER1_ITEM.get());
                        output.accept(ModItems.COBBLE_GEN_TIER2_ITEM.get());
                        output.accept(ModItems.COBBLE_GEN_TIER3_ITEM.get());
                        output.accept(ModItems.TURBO_FURNACE_ITEM.get());
                        output.accept(ModItems.RAPID_CRUCIBLE_ITEM.get());
                        output.accept(ModItems.ACCELERATION_LANTERN_ITEM.get());
                        output.accept(ModItems.OMNI_SYNTHESIZER_ITEM.get());
                        output.accept(ModItems.DUST_BLOCK_ITEM.get());
                        output.accept(ModItems.COMPRESSED_COBBLESTONE_ITEM.get());
                        output.accept(ModItems.DOUBLE_COMPRESSED_COBBLESTONE_ITEM.get());
                        output.accept(ModItems.COMPRESSED_GRAVEL_ITEM.get());
                        output.accept(ModItems.COMPRESSED_SAND_ITEM.get());
                    })
                    .build());
}
