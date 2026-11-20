package com.swiftskyblock.item;

import com.swiftskyblock.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Sky Magnet: passively pulls nearby items and XP orbs to the player while in inventory.
 * Right-click to toggle ON/OFF. Prevents items from falling into the void.
 */
public class SkyMagnetItem extends Item {
    private static final String TAG_ENABLED = "MagnetEnabled";

    public SkyMagnetItem(Properties properties) {
        super(properties);
    }

    public static boolean isEnabled(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ENABLED)) {
            return true; // Default ON for immediate convenience
        }
        return tag.getBoolean(TAG_ENABLED);
    }

    public static void setEnabled(ItemStack stack, boolean enabled) {
        stack.getOrCreateTag().putBoolean(TAG_ENABLED, enabled);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean next = !isEnabled(stack);
            setEnabled(stack, next);
            level.playSound(null, player.blockPosition(),
                    next ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.LEVER_CLICK,
                    SoundSource.PLAYERS, 0.5f, next ? 1.3f : 0.8f);
            player.displayClientMessage(
                    Component.translatable(next ? "message.swiftskyblock.magnet_on" : "message.swiftskyblock.magnet_off")
                            .withStyle(next ? ChatFormatting.GREEN : ChatFormatting.RED),
                    true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player) || player.isSpectator()) {
            return;
        }
        if (!isEnabled(stack) || (level.getGameTime() % 2 != 0)) {
            return;
        }

        double range = ModConfig.MAGNET_RANGE.get();
        AABB box = player.getBoundingBox().inflate(range);
        Vec3 target = player.position().add(0, 0.75, 0);

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box,
                item -> item.isAlive() && !item.hasPickUpDelay());
        for (ItemEntity item : items) {
            Vec3 diff = target.subtract(item.position());
            double dist = diff.length();
            if (dist < 1.2) {
                item.setPos(target.x, target.y, target.z);
            } else {
                item.setDeltaMovement(diff.normalize().scale(0.65));
            }
            item.setNoPickUpDelay();
        }

        List<ExperienceOrb> orbs = level.getEntitiesOfClass(ExperienceOrb.class, box, Entity::isAlive);
        for (ExperienceOrb orb : orbs) {
            Vec3 diff = target.subtract(orb.position());
            if (diff.lengthSqr() > 0.25) {
                orb.setDeltaMovement(diff.normalize().scale(0.65));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isEnabled(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        boolean on = isEnabled(stack);
        tooltip.add(Component.translatable(on ? "tooltip.swiftskyblock.magnet.state_on" : "tooltip.swiftskyblock.magnet.state_off")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.swiftskyblock.magnet.desc").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
