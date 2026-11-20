package com.swiftskyblock.item;

import com.swiftskyblock.client.ClientHooks;
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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Mob Summoning Staff:
 * - Sneak + right-click (or right-click in air) to open a searchable Mob Selection screen.
 * - Right-click any block to summon the selected mob right there!
 * - Repairable in an Anvil using Ender Pearls.
 */
public class MobSummoningStaffItem extends Item {
    private static final String TAG_MOB = "SelectedMob";

    public MobSummoningStaffItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static ResourceLocation getSelectedMobId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_MOB)) {
            return ResourceLocation.tryParse(tag.getString(TAG_MOB));
        }
        return null;
    }

    public static void setSelectedMob(ItemStack stack, ResourceLocation id) {
        stack.getOrCreateTag().putString(TAG_MOB, id.toString());
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(Items.ENDER_PEARL) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openMobSummonerScreen(hand, getSelectedMobId(stack)));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        InteractionHand hand = context.getHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        ResourceLocation mobId = getSelectedMobId(stack);
        // Sneak + right-click or unset mob opens the selection screen
        if (player.isShiftKeyDown() || mobId == null) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openMobSummonerScreen(hand, mobId));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        EntityType<?> type = MobSummonRegistry.byId(mobId);
        if (type == null || !MobSummonRegistry.isAllowed(type)) {
            player.displayClientMessage(Component.translatable("message.swiftskyblock.staff_invalid_mob").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        int cost = MobSummonRegistry.getDurabilityCost(type);
        if (!player.getAbilities().instabuild && stack.getMaxDamage() - stack.getDamageValue() < cost) {
            player.displayClientMessage(Component.translatable("message.swiftskyblock.staff_low_durability", cost).withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);

        Entity entity = type.create(serverLevel);
        if (entity == null) {
            return InteractionResult.FAIL;
        }

        entity.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.05, spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWN_EGG, null, null);
        }

        if (serverLevel.addFreshEntity(entity)) {
            if (!player.getAbilities().instabuild) {
                EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.hurtAndBreak(cost, player, p -> p.broadcastBreakEvent(slot));
            }
            player.getCooldowns().addCooldown(this, 10);
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    spawnPos.getX() + 0.5, spawnPos.getY() + 0.8, spawnPos.getZ() + 0.5,
                    24, 0.4, 0.5, 0.4, 0.15);
            serverLevel.playSound(null, spawnPos, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.6f, 1.2f);
            player.displayClientMessage(
                    Component.translatable("message.swiftskyblock.staff_summoned", type.getDescription())
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getSelectedMobId(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation id = getSelectedMobId(stack);
        if (id != null) {
            EntityType<?> type = MobSummonRegistry.byId(id);
            if (type != null) {
                tooltip.add(Component.translatable("tooltip.swiftskyblock.staff.selected", type.getDescription())
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("tooltip.swiftskyblock.staff.cost", MobSummonRegistry.getDurabilityCost(type))
                        .withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.swiftskyblock.staff.none").withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.swiftskyblock.staff.usage1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.swiftskyblock.staff.usage2").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
