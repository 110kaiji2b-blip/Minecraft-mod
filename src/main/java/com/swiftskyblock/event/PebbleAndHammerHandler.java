package com.swiftskyblock.event;

import com.swiftskyblock.SwiftSkyblockMod;
import com.swiftskyblock.item.HammerItem;
import com.swiftskyblock.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles:
 * 1) Sneak + right-clicking dirt/grass with an empty hand to gather Stone Pebbles (never get softlocked!).
 * 2) Helper for Hammer block crushing drops with Fortune support.
 */
@Mod.EventBusSubscriber(modid = SwiftSkyblockMod.MOD_ID)
public class PebbleAndHammerHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!event.getEntity().isShiftKeyDown() || !event.getItemStack().isEmpty()) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT || block == Blocks.PODZOL) {
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                int count = 1 + (serverLevel.random.nextFloat() < 0.35f ? 1 : 0);
                ItemStack pebble = new ItemStack(ModItems.STONE_PEBBLE.get(), count);
                if (!event.getEntity().getInventory().add(pebble)) {
                    Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, pebble);
                }
                serverLevel.playSound(null, pos, SoundEvents.GRAVEL_STEP, SoundSource.BLOCKS, 0.6f, 1.3f);
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                        6, 0.2, 0.1, 0.2, 0.05);
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /**
     * Crushes a single block using a Hammer, spawning crushed drops directly into the player's inventory
     * (or at their feet to avoid void loss).
     */
    public static boolean crushBlockWithHammer(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, ItemStack tool) {
        ItemStack crushed = HammerItem.getCrushedResult(state);
        if (crushed.isEmpty()) {
            return false;
        }

        int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, tool);
        if (fortune > 0 && level.random.nextFloat() < 0.25f * fortune) {
            crushed.grow(1);
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.25, 0.25, 0.25, 0.05);

        if (!player.getAbilities().instabuild) {
            if (!player.getInventory().add(crushed)) {
                Containers.dropItemStack(level, player.getX(), player.getY() + 0.3, player.getZ(), crushed);
            }
            tool.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }
}
