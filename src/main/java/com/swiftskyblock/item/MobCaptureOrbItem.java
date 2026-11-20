package com.swiftskyblock.item;

import com.swiftskyblock.mob.MobSummonRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Mob Capture Orb: right-click any non-boss mob to safely capture it (preserving villager trades,
 * custom names, sheep colors, etc.), and right-click any block to release it!
 */
public class MobCaptureOrbItem extends Item {
    private static final String TAG_ENTITY_DATA = "CapturedEntity";
    private static final String TAG_ENTITY_ID = "CapturedEntityId";
    private static final String TAG_ENTITY_NAME = "CapturedEntityName";

    public MobCaptureOrbItem(Properties properties) {
        super(properties);
    }

    public static boolean hasCapturedMob(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_ENTITY_DATA);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (hasCapturedMob(stack) || !(target instanceof Mob)) {
            return InteractionResult.PASS;
        }
        EntityType<?> type = target.getType();
        if (MobSummonRegistry.isBoss(type) || type == EntityType.ENDER_DRAGON) {
            if (!player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.swiftskyblock.orb_boss_blocked").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }

        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) {
            return InteractionResult.FAIL;
        }

        CompoundTag entityTag = new CompoundTag();
        if (!target.saveAsPassenger(entityTag)) {
            return InteractionResult.FAIL;
        }
        entityTag.remove("Pos");
        entityTag.remove("Motion");
        entityTag.remove("UUID");

        ItemStack filled;
        if (stack.getCount() == 1) {
            filled = stack;
        } else {
            stack.shrink(1);
            filled = new ItemStack(this, 1);
        }

        CompoundTag itemTag = filled.getOrCreateTag();
        itemTag.put(TAG_ENTITY_DATA, entityTag);
        itemTag.putString(TAG_ENTITY_ID, id.toString());
        itemTag.putString(TAG_ENTITY_NAME, target.getDisplayName().getString());

        if (filled != stack) {
            if (!player.getInventory().add(filled)) {
                player.drop(filled, false);
            }
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    16, 0.3, 0.3, 0.3, 0.05);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.7f, 1.3f);
        }
        player.displayClientMessage(
                Component.translatable("message.swiftskyblock.orb_captured", target.getDisplayName())
                        .withStyle(ChatFormatting.GREEN),
                true);

        target.discard();
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (!hasCapturedMob(stack)) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null) return InteractionResult.FAIL;

        CompoundTag entityData = tag.getCompound(TAG_ENTITY_DATA).copy();
        ServerLevel serverLevel = (ServerLevel) level;
        Optional<Entity> opt = EntityType.create(entityData, serverLevel);
        if (opt.isEmpty()) {
            return InteractionResult.FAIL;
        }

        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);

        Entity entity = opt.get();
        entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.05, spawnPos.getZ() + 0.5, context.getRotation(), 0.0f);

        if (serverLevel.addFreshEntity(entity)) {
            tag.remove(TAG_ENTITY_DATA);
            tag.remove(TAG_ENTITY_ID);
            tag.remove(TAG_ENTITY_NAME);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    spawnPos.getX() + 0.5, spawnPos.getY() + 0.6, spawnPos.getZ() + 0.5,
                    16, 0.3, 0.3, 0.3, 0.05);
            serverLevel.playSound(null, spawnPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.2f);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("message.swiftskyblock.orb_released", entity.getDisplayName())
                                .withStyle(ChatFormatting.AQUA),
                        true);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasCapturedMob(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (hasCapturedMob(stack)) {
            String name = stack.getTag() != null ? stack.getTag().getString(TAG_ENTITY_NAME) : "?";
            tooltip.add(Component.translatable("tooltip.swiftskyblock.orb.filled", name).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.swiftskyblock.orb.release_hint").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.swiftskyblock.orb.empty").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
