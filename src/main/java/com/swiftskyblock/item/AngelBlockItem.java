package com.swiftskyblock.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Allows placing the Angel Block directly in mid-air with no supporting block needed!
 */
public class AngelBlockItem extends BlockItem {

    public AngelBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 targetVec = player.getEyePosition().add(player.getLookAngle().scale(3.0));
        BlockPos pos = BlockPos.containing(targetVec);

        if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getBoundingBox().intersects(new AABB(pos))) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            if (level.setBlock(pos, this.getBlock().defaultBlockState(), 3)) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.swiftskyblock.angel_block.line1").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.swiftskyblock.angel_block.line2").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
