package com.swiftskyblock.block;

import com.swiftskyblock.blockentity.RapidSieveBlockEntity;
import com.swiftskyblock.config.ModConfig;
import com.swiftskyblock.registry.ModBlockEntities;
import com.swiftskyblock.sieve.SieveLootTable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Rapid Sieve:
 * - Right-click with dirt/gravel/sand/dust/soul sand to sift up to 8 blocks at once instantly.
 * - Sneak + right-click to sift the entire held stack (up to 64) instantly!
 * - Also supports hopper automation (insert siftable blocks from top/sides, outputs below).
 */
public class RapidSieveBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 10, 0, 16, 16, 16),
            Block.box(1, 0, 1, 3, 10, 3),
            Block.box(13, 0, 1, 15, 10, 3),
            Block.box(1, 0, 13, 3, 10, 15),
            Block.box(13, 0, 13, 15, 10, 15)
    );

    public RapidSieveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (!SieveLootTable.isSiftable(held)) {
            if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
                player.displayClientMessage(Component.translatable("message.swiftskyblock.sieve_hint").withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            int batch = player.isShiftKeyDown() ? held.getCount() : Math.min(held.getCount(), ModConfig.SIEVE_BATCH_SIZE.get());
            List<ItemStack> drops = SieveLootTable.rollDrops(held.getItem(), batch, level.random);

            Block blockForParticles = Block.byItem(held.getItem());
            if (!player.getAbilities().instabuild) {
                held.shrink(batch);
            }

            int totalItems = 0;
            for (ItemStack drop : drops) {
                totalItems += drop.getCount();
                if (!player.getInventory().add(drop)) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, drop);
                }
            }

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, blockForParticles.defaultBlockState()),
                        pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5,
                        Math.min(30, 8 + batch / 2), 0.3, 0.15, 0.3, 0.08);
            }
            level.playSound(null, pos, SoundEvents.SAND_STEP, SoundSource.BLOCKS, 0.8f, 1.25f);
            player.displayClientMessage(
                    Component.translatable("message.swiftskyblock.sieve_done", batch, totalItems)
                            .withStyle(ChatFormatting.AQUA),
                    true);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RapidSieveBlockEntity sieve) {
                sieve.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RapidSieveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.RAPID_SIEVE.get(), RapidSieveBlockEntity::serverTick);
    }
}
