package com.swiftskyblock.blockentity;

import com.swiftskyblock.menu.OmniSynthesizerMenu;
import com.swiftskyblock.omni.OmniValueRegistry;
import com.swiftskyblock.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OmniSynthesizerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_VP = 1_000_000_000;

    private int voidPoints = 0;

    // Split 32-bit int into two 16-bit slots so Minecraft's short-based ContainerData never overflows
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> voidPoints & 0xFFFF;
                case 1 -> (voidPoints >>> 16) & 0xFFFF;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                voidPoints = (voidPoints & 0xFFFF0000) | (value & 0xFFFF);
            } else if (index == 1) {
                voidPoints = (voidPoints & 0x0000FFFF) | ((value & 0xFFFF) << 16);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    // Hopper/pipe input automatically converts inserted items into Void Points!
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> new IItemHandler() {
        @Override
        public int getSlots() { return 1; }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || voidPoints >= MAX_VP) return stack;
            int unitVal = OmniValueRegistry.getSellValue(stack);
            if (unitVal <= 0) return stack;

            long totalGain = (long) unitVal * stack.getCount();
            if (!simulate) {
                addVoidPoints((int) Math.min(Integer.MAX_VALUE, totalGain));
            }
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }

        @Override
        public int getSlotLimit(int slot) { return 64; }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return OmniValueRegistry.getSellValue(stack) > 0;
        }
    });

    public OmniSynthesizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OMNI_SYNTHESIZER.get(), pos, state);
    }

    public int getVoidPoints() {
        return voidPoints;
    }

    public void addVoidPoints(int amount) {
        if (amount <= 0) return;
        long next = (long) voidPoints + amount;
        voidPoints = (int) Math.min(MAX_VP, next);
        setChanged();
    }

    public boolean consumeVoidPoints(int cost) {
        if (cost < 0 || voidPoints < cost) return false;
        voidPoints -= cost;
        setChanged();
        return true;
    }

    public int depositItemStack(ItemStack stack, boolean shrinkStack) {
        int unitVal = OmniValueRegistry.getSellValue(stack);
        if (unitVal <= 0 || stack.isEmpty() || voidPoints >= MAX_VP) return 0;
        long gainedLong = (long) unitVal * stack.getCount();
        int gained = (int) Math.min(MAX_VP - voidPoints, gainedLong);
        if (gained > 0) {
            addVoidPoints(gained);
            if (shrinkStack) {
                stack.setCount(0);
            }
        }
        return gained;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.swiftskyblock.omni_synthesizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new OmniSynthesizerMenu(containerId, playerInv, this, dataAccess);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("VoidPoints", voidPoints);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        voidPoints = Math.max(0, Math.min(MAX_VP, tag.getInt("VoidPoints")));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }
}
