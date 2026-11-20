package com.swiftskyblock.event;

import com.swiftskyblock.SwiftSkyblockMod;
import com.swiftskyblock.config.ModConfig;
import com.swiftskyblock.item.HammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Handles both single-block Hammer crushing and Sneak Vein-Mining (logs, leaves, ores,
 * and hammerable blocks). All vein-mined drops go directly to the player to prevent void loss!
 */
@Mod.EventBusSubscriber(modid = SwiftSkyblockMod.MOD_ID)
public class VeinMinerHandler {
    private static final Set<UUID> ACTIVE_MINERS = new HashSet<>();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (ACTIVE_MINERS.contains(player.getUUID())) {
            return;
        }

        BlockPos origin = event.getPos();
        BlockState originState = event.getState();
        ItemStack tool = player.getMainHandItem();
        boolean isHammer = tool.getItem() instanceof HammerItem;
        boolean crushable = isHammer && HammerItem.isCrushable(originState);

        // Non-sneak hammer crushing for a single block
        if (!player.isShiftKeyDown() || !ModConfig.VEIN_MINER_ENABLED.get()) {
            if (crushable && !player.getAbilities().instabuild) {
                event.setCanceled(true);
                PebbleAndHammerHandler.crushBlockWithHammer(level, player, origin, originState, tool);
            }
            return;
        }

        // Check if this block qualifies for sneak vein-mining
        if (!isVeinMineable(originState, isHammer)) {
            if (crushable && !player.getAbilities().instabuild) {
                event.setCanceled(true);
                PebbleAndHammerHandler.crushBlockWithHammer(level, player, origin, originState, tool);
            }
            return;
        }

        // Perform safe BFS vein-mine
        event.setCanceled(true);
        ACTIVE_MINERS.add(player.getUUID());
        try {
            int maxBlocks = ModConfig.VEIN_MINER_MAX_BLOCKS.get();
            Block targetBlock = originState.getBlock();
            BlockPos standingOn = player.blockPosition().below();

            Queue<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            Set<BlockPos> brokenLogs = new HashSet<>();

            queue.add(origin);
            visited.add(origin);
            int brokenCount = 0;

            while (!queue.isEmpty() && brokenCount < maxBlocks) {
                BlockPos current = queue.poll();
                // Safety: do not break the exact block directly beneath the player's feet unless it's the origin
                if (!current.equals(origin) && current.equals(standingOn)) {
                    continue;
                }
                // Tool durability safety: stop at 1 durability remaining so valuable tools don't shatter unexpectedly
                if (!player.getAbilities().instabuild && tool.isDamageableItem()
                        && tool.getMaxDamage() - tool.getDamageValue() <= 1 && brokenCount > 0) {
                    break;
                }

                BlockState currentState = level.getBlockState(current);
                if (currentState.getBlock() != targetBlock) {
                    continue;
                }

                if (crushable) {
                    PebbleAndHammerHandler.crushBlockWithHammer(level, player, current, currentState, tool);
                } else {
                    breakAndDeliver(level, player, current, currentState, tool);
                }

                if (currentState.is(BlockTags.LOGS)) {
                    brokenLogs.add(current);
                }
                brokenCount++;

                // Search 3x3x3 neighborhood
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            BlockPos next = current.offset(dx, dy, dz);
                            if (visited.add(next) && level.getBlockState(next).getBlock() == targetBlock) {
                                queue.add(next);
                            }
                        }
                    }
                }
            }

            // Optional instant leaf cleanup when felling a tree (zero waiting for leaves to decay!)
            if (ModConfig.VEIN_MINER_AUTO_LEAVES.get() && !brokenLogs.isEmpty()) {
                clearConnectedLeaves(level, player, brokenLogs, tool);
            }
        } finally {
            ACTIVE_MINERS.remove(player.getUUID());
        }
    }

    private static boolean isVeinMineable(BlockState state, boolean isHammer) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Tags.Blocks.ORES)) {
            return true;
        }
        return isHammer && HammerItem.isCrushable(state);
    }

    private static void breakAndDeliver(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, ItemStack tool) {
        if (!player.getAbilities().instabuild) {
            List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, tool);
            for (ItemStack drop : drops) {
                if (!player.getInventory().add(drop)) {
                    Containers.dropItemStack(level, player.getX(), player.getY() + 0.3, player.getZ(), drop);
                }
            }
            if (tool.isDamageableItem() && !state.is(BlockTags.LEAVES)) {
                tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static void clearConnectedLeaves(ServerLevel level, ServerPlayer player, Set<BlockPos> logs, ItemStack tool) {
        Queue<BlockPos> leafQueue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>(logs);

        for (BlockPos log : logs) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = log.offset(dx, dy, dz);
                        if (visited.add(n) && level.getBlockState(n).is(BlockTags.LEAVES)) {
                            leafQueue.add(n);
                        }
                    }
                }
            }
        }

        int cleared = 0;
        while (!leafQueue.isEmpty() && cleared < 96) {
            BlockPos pos = leafQueue.poll();
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LEAVES)) continue;

            breakAndDeliver(level, player, pos, state, tool);
            cleared++;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos n = pos.offset(dx, dy, dz);
                        if (visited.add(n) && level.getBlockState(n).is(BlockTags.LEAVES)) {
                            leafQueue.add(n);
                        }
                    }
                }
            }
        }
    }
}
