package com.swiftskyblock.blockentity;

import com.swiftskyblock.config.ModConfig;
import com.swiftskyblock.registry.ModBlockEntities;
import com.swiftskyblock.sieve.SieveLootTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Automated backend for RapidSieveBlock: accepts siftable blocks via hopper/pipes,
 * sifts them quickly, and pushes drops into any inventory directly below (or stores in output buffer).
 */
public class RapidSieveBlockEntity extends BlockEntity {
    private static final int INPUT_SLOTS = 1;
    private static final int OUTPUT_SLOTS = 9;

    private final ItemStackHandler inputHandler = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return SieveLootTable.isSiftable(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ItemStackHandler outputHandler = new ItemStackHandler(OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final LazyOptional<IItemHandler> inputCap = LazyOptional.of(() -> inputHandler);
    private final LazyOptional<IItemHandler> outputCap = LazyOptional.of(() -> outputHandler);

    private int tickCounter = 0;

    public RapidSieveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAPID_SIEVE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RapidSieveBlockEntity be) {
        be.tickCounter++;
        int interval = ModConfig.SIEVE_AUTO_INTERVAL_TICKS.get();
        if (be.tickCounter < interval) {
            return;
        }
        be.tickCounter = 0;

        // First try to push buffered outputs into an inventory below
        be.pushOutputsBelow(level, pos);

        ItemStack input = be.inputHandler.getStackInSlot(0);
        if (input.isEmpty() || !SieveLootTable.isSiftable(input)) {
            return;
        }
        if (!be.hasOutputSpace()) {
            return;
        }

        ItemStack one = be.inputHandler.extractItem(0, 1, false);
        if (one.isEmpty()) {
            return;
        }

        List<ItemStack> drops = SieveLootTable.rollDrops(one.getItem(), 1, level.random);
        for (ItemStack drop : drops) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(be.outputHandler, drop, false);
            if (!remainder.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, remainder);
            }
        }
        be.pushOutputsBelow(level, pos);
        be.setChanged();
    }

    private boolean hasOutputSpace() {
        for (int i = 0; i < outputHandler.getSlots(); i++) {
            if (outputHandler.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void pushOutputsBelow(Level level, BlockPos pos) {
        BlockEntity below = level.getBlockEntity(pos.below());
        if (below == null) return;

        below.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).ifPresent(target -> {
            for (int i = 0; i < outputHandler.getSlots(); i++) {
                ItemStack stack = outputHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack remaining = ItemHandlerHelper.insertItemStacked(target, stack.copy(), false);
                    outputHandler.setStackInSlot(i, remaining);
                }
            }
        });
    }

    public void dropContents() {
        if (level == null) return;
        for (int i = 0; i < inputHandler.getSlots(); i++) {
            ItemStack s = inputHandler.getStackInSlot(i);
            if (!s.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, s);
        }
        for (int i = 0; i < outputHandler.getSlots(); i++) {
            ItemStack s = outputHandler.getStackInSlot(i);
            if (!s.isEmpty()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, s);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Input", inputHandler.serializeNBT());
        tag.put("Output", outputHandler.serializeNBT());
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Input")) inputHandler.deserializeNBT(tag.getCompound("Input"));
        if (tag.contains("Output")) outputHandler.deserializeNBT(tag.getCompound("Output"));
        tickCounter = tag.getInt("TickCounter");
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.DOWN) {
                return outputCap.cast();
            }
            return inputCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputCap.invalidate();
        outputCap.invalidate();
    }
}
