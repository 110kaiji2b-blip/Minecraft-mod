package com.swiftskyblock.block;

import com.swiftskyblock.blockentity.OmniSynthesizerBlockEntity;
import com.swiftskyblock.omni.OmniValueRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Omni Synthesizer: converts resources into Void Points (VP) and lets players
 * search and synthesize virtually any survival item in Minecraft!
 */
public class OmniSynthesizerBlock extends BaseEntityBlock {

    public OmniSynthesizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof OmniSynthesizerBlockEntity altar)) {
            return InteractionResult.PASS;
        }

        // Sneak + right-click with an item quickly charges the whole held stack into VP
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !held.isEmpty()) {
            int unitVal = OmniValueRegistry.getSellValue(held);
            if (unitVal > 0) {
                int gained = altar.depositItemStack(held, !player.getAbilities().instabuild);
                if (gained > 0) {
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                                12, 0.3, 0.2, 0.3, 0.05);
                    }
                    level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.3f);
                    player.displayClientMessage(
                            Component.translatable("message.swiftskyblock.omni_charged", gained, altar.getVoidPoints())
                                    .withStyle(ChatFormatting.GOLD),
                            true);
                    return InteractionResult.CONSUME;
                }
            }
        }

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, altar, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OmniSynthesizerBlockEntity(pos, state);
    }
}
