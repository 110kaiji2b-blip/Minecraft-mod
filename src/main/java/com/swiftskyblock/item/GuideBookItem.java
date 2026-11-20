package com.swiftskyblock.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Skyblock Guide item: right-click to display a concise, localized progression & shortcut guide in chat.
 */
public class GuideBookItem extends Item {

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            sendGuide(player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void sendGuide(Player player) {
        player.sendSystemMessage(Component.translatable("guide.swiftskyblock.header").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("guide.swiftskyblock.step1").withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.translatable("guide.swiftskyblock.step2").withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.translatable("guide.swiftskyblock.step3").withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("guide.swiftskyblock.step4").withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.translatable("guide.swiftskyblock.step5").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.translatable("guide.swiftskyblock.commands").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.swiftskyblock.guide.desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
