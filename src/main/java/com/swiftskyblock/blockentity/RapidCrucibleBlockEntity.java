package com.swiftskyblock.blockentity;

import com.swiftskyblock.block.RapidCrucibleBlock;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RapidCrucibleBlockEntity extends BlockEntity {
    public static final int MAX_LAVA_MB = 4000;
    public static final int MAX_SOLID_MB = 4000;
    public static final int MB_PER_COBBLE = 250; // 4 cobble = 1 lava bucket!

    private int solidAmountMb = 0;

    private final FluidTank lavaTank = new FluidTank(MAX_LAVA_MB, fs -> fs.getFluid() == Fluids.LAVA) {
        @Override
        protected void onContentsChanged() {
            syncBlockState();
            setChanged();
        }
    };

    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> lavaTank);
    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> new IItemHandler() {
        @Override
        public int getSlots() { return 1; }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            int val = getMeltableMb(stack.getItem());
            if (val <= 0) return stack;
            int space = MAX_SOLID_MB - solidAmountMb;
            int canAccept = Math.min(stack.getCount(), space / val);
            if (canAccept <= 0) return stack;
            if (!simulate) {
                solidAmountMb += canAccept * val;
                setChanged();
            }
            if (canAccept == stack.getCount()) return ItemStack.EMPTY;
            ItemStack rem = stack.copy();
            rem.shrink(canAccept);
            return rem;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }

        @Override
        public int getSlotLimit(int slot) { return 64; }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) { return getMeltableMb(stack.getItem()) > 0; }
    });

    public RapidCrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RAPID_CRUCIBLE.get(), pos, state);
    }

    public static int getMeltableMb(Item item) {
        if (item == Items.COBBLESTONE || item == Items.STONE || item == Items.GRAVEL
                || item == Items.COBBLED_DEEPSLATE || item == Items.NETHERRACK) {
            return MB_PER_COBBLE;
        }
        if (item == ModBlocks.COMPRESSED_COBBLESTONE.get().asItem()) {
            return MB_PER_COBBLE * 9;
        }
        return 0;
    }

    public static int getHeatRateMbPerTick(BlockState belowState) {
        Block b = belowState.getBlock();
        if (b == Blocks.LAVA || b == Blocks.LAVA_CAULDRON) return 100; // 0.5 sec per bucket!
        if (b == Blocks.GLOWSTONE || b == Blocks.SHROOMLIGHT) return 75;
        if (b == Blocks.MAGMA_BLOCK || b == Blocks.CAMPFIRE || b == Blocks.SOUL_CAMPFIRE
                || b == Blocks.FIRE || b == Blocks.SOUL_FIRE) return 50; // 1 sec per bucket!
        if (b == Blocks.TORCH || b == Blocks.WALL_TORCH || b == Blocks.SOUL_TORCH || b == Blocks.SOUL_WALL_TORCH
                || b == Blocks.LANTERN || b == Blocks.SOUL_LANTERN || b == ModBlocks.ACCELERATION_LANTERN.get()) {
            return 25; // 2 sec per bucket!
        }
        return 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RapidCrucibleBlockEntity be) {
        if (be.solidAmountMb <= 0 || be.lavaTank.getFluidAmount() >= MAX_LAVA_MB) {
            return;
        }
        int rate = getHeatRateMbPerTick(level.getBlockState(pos.below()));
        if (rate <= 0) {
            return;
        }
        int melt = Math.min(rate, Math.min(be.solidAmountMb, MAX_LAVA_MB - be.lavaTank.getFluidAmount()));
        if (melt > 0) {
            be.solidAmountMb -= melt;
            be.lavaTank.fill(new FluidStack(Fluids.LAVA, melt), IFluidHandler.FluidAction.EXECUTE);
            be.setChanged();
        }
    }

    public InteractionResult handlePlayerUse(Player player, InteractionHand hand) {
        if (level == null) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);

        // Extract lava with empty bucket
        if (held.is(Items.BUCKET) && lavaTank.getFluidAmount() >= 1000) {
            lavaTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            if (!player.getAbilities().instabuild) {
                if (held.getCount() == 1) {
                    player.setItemInHand(hand, new ItemStack(Items.LAVA_BUCKET));
                } else {
                    held.shrink(1);
                    if (!player.getInventory().add(new ItemStack(Items.LAVA_BUCKET))) {
                        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, new ItemStack(Items.LAVA_BUCKET));
                    }
                }
            }
            level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.8f, 1.0f);
            showStatus(player);
            return InteractionResult.CONSUME;
        }

        // Insert meltable stone/cobble (sneak = batch insert!)
        int val = getMeltableMb(held.getItem());
        if (val > 0 && solidAmountMb + val <= MAX_SOLID_MB) {
            int maxAdd = (MAX_SOLID_MB - solidAmountMb) / val;
            int toAdd = player.isShiftKeyDown() ? Math.min(maxAdd, held.getCount()) : 1;
            if (toAdd > 0) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(toAdd);
                }
                solidAmountMb += toAdd * val;
                setChanged();
                level.playSound(null, worldPosition, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.7f, 1.1f);
                showStatus(player);
                return InteractionResult.CONSUME;
            }
        }

        if (hand == InteractionHand.MAIN_HAND) {
            showStatus(player);
        }
        return InteractionResult.CONSUME;
    }

    private void showStatus(Player player) {
        int heat = level != null ? getHeatRateMbPerTick(level.getBlockState(worldPosition.below())) : 0;
        if (heat <= 0) {
            player.displayClientMessage(
                    Component.translatable("message.swiftskyblock.crucible_no_heat", lavaTank.getFluidAmount(), solidAmountMb)
                            .withStyle(ChatFormatting.RED),
                    true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.swiftskyblock.crucible_status", lavaTank.getFluidAmount(), MAX_LAVA_MB, solidAmountMb)
                            .withStyle(ChatFormatting.GOLD),
                    true);
        }
    }

    private void syncBlockState() {
        if (level != null && !level.isClientSide) {
            boolean hasLava = lavaTank.getFluidAmount() > 0;
            BlockState state = getBlockState();
            if (state.getBlock() instanceof RapidCrucibleBlock && state.getValue(RapidCrucibleBlock.HAS_LAVA) != hasLava) {
                level.setBlock(worldPosition, state.setValue(RapidCrucibleBlock.HAS_LAVA, hasLava), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("SolidMb", solidAmountMb);
        lavaTank.writeToNBT(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        solidAmountMb = tag.getInt("SolidMb");
        lavaTank.readFromNBT(tag);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidCap.invalidate();
        itemCap.invalidate();
    }
}
