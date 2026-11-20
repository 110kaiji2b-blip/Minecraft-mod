package com.swiftskyblock.blockentity;

import com.swiftskyblock.config.ModConfig;
import com.swiftskyblock.item.TimePocketWatchItem;
import com.swiftskyblock.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AccelerationLanternBlockEntity extends BlockEntity {

    public AccelerationLanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ACCELERATION_LANTERN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AccelerationLanternBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int radius = ModConfig.ACCEL_LANTERN_RADIUS.get();
        int extraTicks = ModConfig.ACCEL_LANTERN_EXTRA_TICKS.get();
        boolean randomCropTick = (level.getGameTime() % 10 == 0);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    cursor.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!serverLevel.isLoaded(cursor)) continue;

                    BlockEntity targetBe = serverLevel.getBlockEntity(cursor);
                    if (targetBe != null && !(targetBe instanceof AccelerationLanternBlockEntity)) {
                        BlockState targetState = serverLevel.getBlockState(cursor);
                        TimePocketWatchItem.tickBlockEntity(serverLevel, cursor.immutable(), targetState, targetBe, extraTicks);
                    } else if (randomCropTick) {
                        BlockState targetState = serverLevel.getBlockState(cursor);
                        if (targetState.isRandomlyTicking() && !(targetState.getBlock() instanceof GrassBlock)) {
                            if (targetState.getBlock() instanceof BonemealableBlock || serverLevel.random.nextInt(3) == 0) {
                                targetState.randomTick(serverLevel, cursor.immutable(), serverLevel.random);
                            }
                        }
                    }
                }
            }
        }
    }
}
