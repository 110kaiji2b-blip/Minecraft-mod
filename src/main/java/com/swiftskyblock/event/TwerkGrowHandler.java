package com.swiftskyblock.event;

import com.swiftskyblock.SwiftSkyblockMod;
import com.swiftskyblock.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rapidly accelerates nearby saplings, crops, sugar cane, cactus, bamboo, and nether wart
 * when the player sneaks repeatedly (twerk-to-grow) or sprints nearby.
 */
@Mod.EventBusSubscriber(modid = SwiftSkyblockMod.MOD_ID)
public class TwerkGrowHandler {
    private static final Map<UUID, Boolean> LAST_SNEAK_STATE = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SNEAK_STATE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!ModConfig.TWERK_GROW_ENABLED.get() || player.isSpectator()) {
            return;
        }

        UUID uuid = player.getUUID();
        boolean sneaking = player.isShiftKeyDown();
        Boolean prev = LAST_SNEAK_STATE.put(uuid, sneaking);

        boolean toggledSneak = (prev != null && prev != sneaking);
        boolean sprintTrigger = ModConfig.SPRINT_GROW_ENABLED.get()
                && player.isSprinting()
                && (player.level().getGameTime() % 10 == 0);

        if (!toggledSneak && !sprintTrigger) {
            return;
        }

        ServerLevel level = player.serverLevel();
        int radius = ModConfig.TWERK_GROW_RADIUS.get();
        int attempts = toggledSneak ? ModConfig.TWERK_GROW_STRENGTH.get() : 1;
        BlockPos center = player.blockPosition();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) continue;

                    BlockPos immutable = cursor.immutable();
                    if (tryGrowBlock(level, immutable, state, attempts)) {
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                immutable.getX() + 0.5, immutable.getY() + 0.6, immutable.getZ() + 0.5,
                                4, 0.3, 0.3, 0.3, 0.02);
                    }
                }
            }
        }
    }

    private static boolean tryGrowBlock(ServerLevel level, BlockPos pos, BlockState state, int attempts) {
        Block block = state.getBlock();

        // Avoid spamming tall grass on grass blocks
        if (block instanceof GrassBlock || block instanceof NyliumBlock || block instanceof TallGrassBlock) {
            return false;
        }

        if (block instanceof BonemealableBlock bonemealable) {
            boolean grew = false;
            for (int i = 0; i < attempts; i++) {
                BlockState current = level.getBlockState(pos);
                if (current.getBlock() != block) {
                    // For example, a sapling already grew into a full tree!
                    return true;
                }
                if (bonemealable.isValidBonemealTarget(level, pos, current, false)) {
                    bonemealable.performBonemeal(level, level.random, pos, current);
                    grew = true;
                } else {
                    break;
                }
            }
            return grew;
        }

        if (block instanceof SugarCaneBlock || block instanceof CactusBlock) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir()) {
                int height = 1;
                BlockPos check = pos.below();
                while (level.getBlockState(check).is(block) && height < 4) {
                    height++;
                    check = check.below();
                }
                if (height < 3 && level.random.nextFloat() < 0.35f * attempts) {
                    level.setBlock(above, block.defaultBlockState(), 3);
                    return true;
                }
            }
            return false;
        }

        if (block instanceof BambooStalkBlock) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).isAir() && level.random.nextFloat() < 0.3f * attempts) {
                state.randomTick(level, pos, level.random);
                return true;
            }
            return false;
        }

        if (block instanceof NetherWartBlock) {
            int age = state.getValue(NetherWartBlock.AGE);
            if (age < 3) {
                int nextAge = Math.min(3, age + 1);
                level.setBlock(pos, state.setValue(NetherWartBlock.AGE, nextAge), 2);
                return true;
            }
        }

        return false;
    }
}
