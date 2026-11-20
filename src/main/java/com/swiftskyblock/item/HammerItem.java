package com.swiftskyblock.item;

import com.swiftskyblock.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Crushing Hammer: mines stone, gravel, sand, dust, and compressed blocks quickly
 * and crushes them down one tier (Cobblestone -> Gravel -> Sand -> Dust).
 */
public class HammerItem extends DiggerItem {

    public HammerItem(Tier tier, float attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(attackDamageModifier, attackSpeedModifier, tier, BlockTags.MINEABLE_WITH_PICKAXE, properties);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (isCrushable(state) || state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return this.speed * 1.4f;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (isCrushable(state)) {
            return true;
        }
        return super.isCorrectToolForDrops(stack, state);
    }

    public static boolean isCrushable(BlockState state) {
        return !getCrushedResult(state).isEmpty();
    }

    public static ItemStack getCrushedResult(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.COBBLESTONE || block == Blocks.STONE || block == Blocks.COBBLED_DEEPSLATE
                || block == Blocks.DEEPSLATE || block == Blocks.ANDESITE || block == Blocks.DIORITE || block == Blocks.GRANITE) {
            return new ItemStack(Blocks.GRAVEL, 1);
        }
        if (block == Blocks.GRAVEL) {
            return new ItemStack(Blocks.SAND, 1);
        }
        if (block == Blocks.SAND || block == Blocks.RED_SAND) {
            return new ItemStack(ModBlocks.DUST_BLOCK.get(), 1);
        }
        if (block == Blocks.NETHERRACK) {
            return new ItemStack(Blocks.SOUL_SAND, 1);
        }
        // Compressed crushing: 1 break = 9x output!
        if (block == ModBlocks.COMPRESSED_COBBLESTONE.get()) {
            return new ItemStack(Blocks.GRAVEL, 9);
        }
        if (block == ModBlocks.DOUBLE_COMPRESSED_COBBLESTONE.get()) {
            return new ItemStack(ModBlocks.COMPRESSED_GRAVEL.get(), 9);
        }
        if (block == ModBlocks.COMPRESSED_GRAVEL.get()) {
            return new ItemStack(Blocks.SAND, 9);
        }
        if (block == ModBlocks.COMPRESSED_SAND.get()) {
            return new ItemStack(ModBlocks.DUST_BLOCK.get(), 9);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.swiftskyblock.hammer.line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.swiftskyblock.hammer.line2").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
