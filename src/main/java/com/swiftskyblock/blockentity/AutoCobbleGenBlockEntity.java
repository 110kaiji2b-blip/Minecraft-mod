package com.swiftskyblock.blockentity;

import com.swiftskyblock.block.AutoCobbleGenBlock;
import com.swiftskyblock.registry.ModBlockEntities;
import com.swiftskyblock.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AutoCobbleGenBlockEntity extends BlockEntity {
    private int storedCobble = 0;
    private int tickCounter = 0;

    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> new IItemHandler() {
        @Override
        public int getSlots() { return 1; }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return storedCobble > 0 ? new ItemStack(Items.COBBLESTONE, Math.min(64, storedCobble)) : ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return stack; // Output only
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (storedCobble <= 0 || amount <= 0) return ItemStack.EMPTY;
            int take = Math.min(Math.min(64, amount), storedCobble);
            if (!simulate) {
                storedCobble -= take;
                setChanged();
            }
            return new ItemStack(Items.COBBLESTONE, take);
        }

        @Override
        public int getSlotLimit(int slot) { return 64; }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) { return false; }
    });

    public AutoCobbleGenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_COBBLE_GEN.get(), pos, state);
    }

    private int getTier() {
        if (getBlockState().getBlock() instanceof AutoCobbleGenBlock gen) {
            return gen.getTier();
        }
        return 1;
    }

    private int getMaxStorage() {
        return switch (getTier()) {
            case 2 -> 2048;
            case 3 -> 8192;
            default -> 512;
        };
    }

    private int getIntervalTicks() {
        return switch (getTier()) {
            case 2 -> 5;  // 2 per 5 ticks = 8/sec
            case 3 -> 5;  // 8 per 5 ticks = 32/sec
            default -> 10; // 1 per 10 ticks = 2/sec
        };
    }

    private int getBatchAmount() {
        return switch (getTier()) {
            case 2 -> 2;
            case 3 -> 8;
            default -> 1;
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoCobbleGenBlockEntity be) {
        be.tickCounter++;
        if (be.tickCounter < be.getIntervalTicks()) {
            return;
        }
        be.tickCounter = 0;

        int max = be.getMaxStorage();
        if (be.storedCobble < max) {
            be.storedCobble = Math.min(max, be.storedCobble + be.getBatchAmount());
            be.setChanged();
        }

        // Automatically push into inventory above (e.g., Chest or Hopper)
        if (be.storedCobble > 0) {
            be.pushToNeighbor(level, pos.above(), Direction.DOWN);
        }
    }

    private void pushToNeighbor(Level level, BlockPos neighborPos, Direction face) {
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor == null) return;

        neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, face).ifPresent(handler -> {
            int toPush = Math.min(64, storedCobble);
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(Items.COBBLESTONE, toPush), false);
            int inserted = toPush - remaining.getCount();
            if (inserted > 0) {
                storedCobble -= inserted;
                setChanged();
            }
        });
    }

    public void extractToPlayer(Player player, boolean compressed) {
        if (level == null) return;
        if (compressed) {
            int compressedCount = Math.min(64, storedCobble / 9);
            if (compressedCount > 0) {
                storedCobble -= compressedCount * 9;
                setChanged();
                giveOrDrop(player, new ItemStack(ModBlocks.COMPRESSED_COBBLESTONE.get(), compressedCount));
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.1f);
            }
        } else {
            int take = Math.min(64, storedCobble);
            if (take > 0) {
                storedCobble -= take;
                setChanged();
                giveOrDrop(player, new ItemStack(Items.COBBLESTONE, take));
                level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.2f);
            }
        }
        player.displayClientMessage(
                Component.translatable("message.swiftskyblock.cobble_gen_status", storedCobble, getMaxStorage())
                        .withStyle(ChatFormatting.AQUA),
                true);
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && level != null) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5, stack);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("StoredCobble", storedCobble);
        tag.putInt("TickCounter", tickCounter);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storedCobble = tag.getInt("StoredCobble");
        tickCounter = tag.getInt("TickCounter");
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
