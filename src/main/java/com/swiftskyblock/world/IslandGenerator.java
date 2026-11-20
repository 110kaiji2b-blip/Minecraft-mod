package com.swiftskyblock.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Generates a comfortable starter Skyblock island with an Oak tree, bedrock safety anchor,
 * and starter chest equipped with zero-wait essentials.
 */
public class IslandGenerator {

    /**
     * Generates a starter island centered at {@code center} (top grass layer at center.getY()).
     * Returns the safe player spawn position on top of the island.
     */
    public static BlockPos generateStarterIsland(ServerLevel level, BlockPos center) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        // Main 5x5 platform (3 layers deep with tapered bottom)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                // Skip outer corners on top for a natural island feel? Keep full 5x5 so it's spacious & comfy!
                level.setBlock(new BlockPos(cx + dx, cy, cz + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(new BlockPos(cx + dx, cy - 1, cz + dz), Blocks.DIRT.defaultBlockState(), 3);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    level.setBlock(new BlockPos(cx + dx, cy - 2, cz + dz), Blocks.DIRT.defaultBlockState(), 3);
                }
            }
        }

        // Classic L-extension (+3 in X, 3 wide in Z) for easy Cobblestone Generator setup
        for (int dx = 3; dx <= 4; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(new BlockPos(cx + dx, cy, cz + dz), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                level.setBlock(new BlockPos(cx + dx, cy - 1, cz + dz), Blocks.DIRT.defaultBlockState(), 3);
            }
        }

        // Bedrock anchor underneath center so the player can never accidentally destroy the last block
        level.setBlock(new BlockPos(cx, cy - 3, cz), Blocks.BEDROCK.defaultBlockState(), 3);
        level.setBlock(new BlockPos(cx, cy - 2, cz), Blocks.COBBLESTONE.defaultBlockState(), 3);

        // Clear air above platform
        for (int dx = -2; dx <= 4; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 1; dy <= 7; dy++) {
                    level.setBlock(new BlockPos(cx + dx, cy + dy, cz + dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // Starter Oak Tree at (cx + 3, cy + 1, cz)
        int tx = cx + 3;
        int ty = cy + 1;
        int tz = cz;
        for (int y = 0; y < 5; y++) {
            level.setBlock(new BlockPos(tx, ty + y, tz), Blocks.OAK_LOG.defaultBlockState(), 3);
        }
        // Leaves canopy
        for (int dy = 2; dy <= 3; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    BlockPos lp = new BlockPos(tx + dx, ty + dy, tz + dz);
                    if (level.getBlockState(lp).isAir()) {
                        level.setBlock(lp, Blocks.OAK_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
        for (int dy = 4; dy <= 5; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dy == 5 && Math.abs(dx) == 1 && Math.abs(dz) == 1) continue;
                    BlockPos lp = new BlockPos(tx + dx, ty + dy, tz + dz);
                    if (level.getBlockState(lp).isAir()) {
                        level.setBlock(lp, Blocks.OAK_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }

        // Starter Chest at (cx - 1, cy + 1, cz - 1) facing SOUTH (classic minimal Skyblock start)
        BlockPos chestPos = new BlockPos(cx - 1, cy + 1, cz - 1);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 3);
        BlockEntity be = level.getBlockEntity(chestPos);
        if (be instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.ICE, 1));
            chest.setItem(1, new ItemStack(Items.LAVA_BUCKET, 1));
            chest.setChanged();
        }

        return new BlockPos(cx, cy + 1, cz);
    }

    /**
     * Checks if the column near (x, z) is completely empty (void world detection).
     */
    public static boolean isVoidColumn(ServerLevel level, int x, int z) {
        int minY = level.getMinBuildHeight();
        int maxY = Math.min(level.getMaxBuildHeight(), 160);
        for (int y = minY; y < maxY; y++) {
            if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Safely teleports a player to the target home position, ensuring solid footing and clear headroom.
     */
    public static void teleportSafely(ServerPlayer player, ServerLevel targetLevel, BlockPos pos) {
        // Ensure safe floor beneath
        BlockPos below = pos.below();
        BlockState belowState = targetLevel.getBlockState(below);
        if (belowState.isAir() || !belowState.blocksMotion()) {
            targetLevel.setBlock(below, Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
        // Clear space for player body if obstructed
        if (targetLevel.getBlockState(pos).blocksMotion()) {
            targetLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        if (targetLevel.getBlockState(pos.above()).blocksMotion()) {
            targetLevel.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        }

        player.fallDistance = 0.0f;
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(targetLevel, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.fallDistance = 0.0f;
    }
}
