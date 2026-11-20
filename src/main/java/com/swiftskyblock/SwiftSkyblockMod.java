package com.swiftskyblock;

import com.swiftskyblock.config.ModConfig;
import com.swiftskyblock.network.ModNetworking;
import com.swiftskyblock.registry.ModBlockEntities;
import com.swiftskyblock.registry.ModBlocks;
import com.swiftskyblock.registry.ModCreativeTabs;
import com.swiftskyblock.registry.ModItems;
import com.swiftskyblock.registry.ModMenus;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SwiftSkyblockMod.MOD_ID)
public class SwiftSkyblockMod {
    public static final String MOD_ID = "swiftskyblock";

    public SwiftSkyblockMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);

        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.COMMON_SPEC);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetworking::register);
    }
}
