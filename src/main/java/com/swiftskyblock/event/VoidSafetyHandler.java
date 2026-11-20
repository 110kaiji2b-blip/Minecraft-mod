package com.swiftskyblock.event;

import com.swiftskyblock.SwiftSkyblockMod;
import com.swiftskyblock.config.ModConfig;
import com.swiftskyblock.world.IslandGenerator;
import com.swiftskyblock.world.SkyblockSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Prevents frustrating void deaths and item loss in Skyblock by safely teleporting
 * players back to their island home if they fall into the void.
 */
@Mod.EventBusSubscriber(modid = SwiftSkyblockMod.MOD_ID)
public class VoidSafetyHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!ModConfig.VOID_SAFETY_ENABLED.get() || player.isSpectator() || player.getAbilities().instabuild) {
            return;
        }

        if (player.getY() < ModConfig.VOID_SAFETY_Y.get()) {
            ServerLevel overworld = player.server.overworld();
            SkyblockSavedData data = SkyblockSavedData.get(overworld);
            BlockPos home = data.getHomeFor(player.getUUID());
            if (home == null) {
                home = overworld.getSharedSpawnPos();
            }

            IslandGenerator.teleportSafely(player, overworld, home);

            if (ModConfig.VOID_SAFETY_DAMAGE.get() && player.getHealth() > 4.0f) {
                player.setHealth(Math.max(2.0f, player.getHealth() - 4.0f));
            }

            overworld.playSound(null, home, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.1f);
            player.displayClientMessage(
                    Component.translatable("message.swiftskyblock.void_saved").withStyle(ChatFormatting.GREEN),
                    true);
        }
    }
}
