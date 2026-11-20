package com.swiftskyblock.world;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.swiftskyblock.item.GuideBookItem;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Provides /skyblock (and alias /sb) commands so players can create an island in any world,
 * return home, set a custom home, or view the progression guide at any time.
 */
public class SkyblockCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildCommand("skyblock"));
        dispatcher.register(buildCommand("sb"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand(String root) {
        return Commands.literal(root)
                .then(Commands.literal("create").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel overworld = player.server.overworld();
                    SkyblockSavedData data = SkyblockSavedData.get(overworld);

                    BlockPos center;
                    if (!data.isIslandInitialized() && IslandGenerator.isVoidColumn(overworld, 0, 0)) {
                        center = new BlockPos(0, 64, 0);
                        data.setIslandInitialized(true);
                    } else {
                        center = data.allocateNextIslandCenter();
                        // Raise above terrain if used in a normal world
                        if (!IslandGenerator.isVoidColumn(overworld, center.getX(), center.getZ())) {
                            center = new BlockPos(center.getX(), 180, center.getZ());
                        }
                    }

                    BlockPos spawn = IslandGenerator.generateStarterIsland(overworld, center);
                    data.setHomeFor(player.getUUID(), spawn);
                    if (data.getDefaultHome() == null) {
                        data.setDefaultHome(spawn);
                        overworld.setDefaultSpawnPos(spawn, 0.0f);
                    }
                    player.setRespawnPosition(overworld.dimension(), spawn, 0.0f, true, false);
                    IslandGenerator.teleportSafely(player, overworld, spawn);
                    player.sendSystemMessage(Component.translatable("message.swiftskyblock.island_created",
                            spawn.getX(), spawn.getY(), spawn.getZ()).withStyle(ChatFormatting.GREEN));
                    return 1;
                }))
                .then(Commands.literal("home").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel overworld = player.server.overworld();
                    SkyblockSavedData data = SkyblockSavedData.get(overworld);

                    BlockPos home = data.getHomeFor(player.getUUID());
                    if (home == null) {
                        home = overworld.getSharedSpawnPos();
                    }
                    IslandGenerator.teleportSafely(player, overworld, home);
                    player.sendSystemMessage(Component.translatable("message.swiftskyblock.teleported_home")
                            .withStyle(ChatFormatting.AQUA));
                    return 1;
                }))
                .then(Commands.literal("sethome").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel overworld = player.server.overworld();
                    if (player.level() != overworld) {
                        player.sendSystemMessage(Component.translatable("message.swiftskyblock.sethome_overworld_only")
                                .withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    BlockPos pos = player.blockPosition();
                    SkyblockSavedData.get(overworld).setHomeFor(player.getUUID(), pos);
                    player.setRespawnPosition(overworld.dimension(), pos, player.getYRot(), true, false);
                    player.sendSystemMessage(Component.translatable("message.swiftskyblock.home_set",
                            pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN));
                    return 1;
                }))
                .then(Commands.literal("guide").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    GuideBookItem.sendGuide(player);
                    return 1;
                }));
    }
}
