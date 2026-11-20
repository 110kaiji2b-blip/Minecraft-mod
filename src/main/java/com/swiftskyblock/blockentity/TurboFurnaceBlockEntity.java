package com.swiftskyblock.blockentity;

import com.swiftskyblock.config.ModConfig;
import com.swiftskyblock.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TurboFurnaceBlockEntity extends AbstractFurnaceBlockEntity {

    public TurboFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBO_FURNACE.get(), pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.swiftskyblock.turbo_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new FurnaceMenu(containerId, inventory, this, this.dataAccess);
    }

    @Override
    protected int getBurnDuration(ItemStack fuel) {
        return super.getBurnDuration(fuel) * 2;
    }

    /**
     * Advances smelting progress at N times vanilla speed (default 8x) while burning fuel gently.
     */
    public static void turboServerTick(Level level, BlockPos pos, BlockState state, TurboFurnaceBlockEntity entity) {
        AbstractFurnaceBlockEntity.serverTick(level, pos, state, entity);

        int speed = ModConfig.TURBO_FURNACE_SPEED.get();
        if (speed <= 1) {
            return;
        }

        int litTime = entity.dataAccess.get(0);
        int progress = entity.dataAccess.get(2);
        int totalTime = entity.dataAccess.get(3);

        if (litTime > 0 && totalTime > 0 && progress > 0) {
            int boosted = Math.min(totalTime - 1, progress + (speed - 1));
            if (boosted > progress) {
                entity.dataAccess.set(2, boosted);
                if (boosted >= totalTime - 1) {
                    // Finish smelting on this tick without double-consuming litTime
                    int savedLit = entity.dataAccess.get(0);
                    AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), entity);
                    if (entity.dataAccess.get(0) < savedLit) {
                        entity.dataAccess.set(0, savedLit);
                    }
                }
            }
        }
    }
}
