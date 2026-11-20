package com.swiftskyblock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Angel Block: can be placed floating in mid-air and returns straight to the player's inventory when broken!
 */
public class AngelBlock extends Block {

    public AngelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.getAbilities().instabuild) {
            ItemStack stack = new ItemStack(this.asItem());
            if (!player.getInventory().add(stack)) {
                popResource(level, pos, stack);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
