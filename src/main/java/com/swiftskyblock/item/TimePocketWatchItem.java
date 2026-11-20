package com.swiftskyblock.item;

import com.swiftskyblock.blockentity.AccelerationLanternBlockEntity;
import com.swiftskyblock.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Time Pocket Watch: right-click any machine, furnace, crucible, crop, or sapling
 * to instantly fast-forward its ticks and eliminate waiting.
 */
public class TimePocketWatchItem extends Item {

    public TimePocketWatchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        int ticks = ModConfig.POCKET_WATCH_TICKS.get();
        boolean accelerated = false;

        BlockEntity be = serverLevel.getBlockEntity(pos);
        if (be != null && !(be instanceof AccelerationLanternBlockEntity)) {
            accelerated = tickBlockEntity(serverLevel, pos, state, be, ticks);
        }

        if (!accelerated && (state.isRandomlyTicking() || state.getBlock() instanceof BonemealableBlock)) {
            for (int i = 0; i < Math.min(20, ticks / 5); i++) {
                BlockState current = serverLevel.getBlockState(pos);
                if (current.getBlock() instanceof BonemealableBlock bonemealable
                        && bonemealable.isValidBonemealTarget(serverLevel, pos, current, false)) {
                    bonemealable.performBonemeal(serverLevel, serverLevel.random, pos, current);
                    accelerated = true;
                } else if (current.isRandomlyTicking()) {
                    current.randomTick(serverLevel, pos, serverLevel.random);
                    accelerated = true;
                }
            }
        }

        if (accelerated) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    16, 0.35, 0.35, 0.35, 0.05);
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.4f);
            if (player != null) {
                player.getCooldowns().addCooldown(this, 5);
                player.displayClientMessage(
                        Component.translatable("message.swiftskyblock.time_accelerated", ticks)
                                .withStyle(ChatFormatting.GOLD),
                        true);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @SuppressWarnings("unchecked")
    public static <T extends BlockEntity> boolean tickBlockEntity(ServerLevel level, BlockPos pos, BlockState state, T be, int count) {
        BlockEntityTicker<T> ticker = state.getTicker(level, (BlockEntityType<T>) be.getType());
        if (ticker == null) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            BlockState current = level.getBlockState(pos);
            if (be.isRemoved() || current.getBlock() != state.getBlock()) {
                break;
            }
            ticker.tick(level, pos, current, be);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.swiftskyblock.pocket_watch.line1").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.swiftskyblock.pocket_watch.line2").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
