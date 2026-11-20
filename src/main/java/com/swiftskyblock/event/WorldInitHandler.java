package com.swiftskyblock.event;

import com.swiftskyblock.SwiftSkyblockMod;
import com.swiftskyblock.world.IslandGenerator;
import com.swiftskyblock.world.SkyblockCommand;
import com.swiftskyblock.world.SkyblockSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SwiftSkyblockMod.MOD_ID)
public class WorldInitHandler {
    private static final String TAG_SPAWNED = "swiftskyblock_spawned";

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SkyblockCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel overworld = player.server.overworld();
        SkyblockSavedData data = SkyblockSavedData.get(overworld);

        // Automatically detect void/Skyblock worlds on first login and build the starter island
        if (!data.isIslandInitialized()) {
            boolean voidAtOrigin = IslandGenerator.isVoidColumn(overworld, 0, 0)
                    && IslandGenerator.isVoidColumn(overworld, 8, 8);
            if (voidAtOrigin) {
                BlockPos spawn = IslandGenerator.generateStarterIsland(overworld, new BlockPos(0, 64, 0));
                data.setIslandInitialized(true);
                data.setDefaultHome(spawn);
                overworld.setDefaultSpawnPos(spawn, 0.0f);
            }
        }

        if (data.isIslandInitialized() && !player.getPersistentData().getBoolean(TAG_SPAWNED)) {
            BlockPos home = data.getHomeFor(player.getUUID());
            if (home == null) {
                home = data.getDefaultHome();
            }
            if (home != null) {
                data.setHomeFor(player.getUUID(), home);
                player.setRespawnPosition(overworld.dimension(), home, 0.0f, true, false);
                IslandGenerator.teleportSafely(player, overworld, home);
                player.getPersistentData().putBoolean(TAG_SPAWNED, true);
                player.sendSystemMessage(Component.translatable("message.swiftskyblock.welcome")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                player.sendSystemMessage(Component.translatable("message.swiftskyblock.welcome_tip")
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
