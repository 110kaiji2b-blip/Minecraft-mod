package com.swiftskyblock.blockentity;

import com.swiftskyblock.block.CompostBarrelBlock;
import com.swiftskyblock.block.CompostBarrelBlock.BarrelContent;
import com.swiftskyblock.registry.ModBlockEntities;
import com.swiftskyblock.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CompostBarrelBlockEntity extends BlockEntity {
    public static final int MAX_COMPOST = 8;
    public static final int COMPOST_TIME_TICKS = 40; // 2 seconds!

    private BarrelContent content = BarrelContent.EMPTY;
    private int compostFill = 0;
    private int compostProgress = 0;
    private int rainProgress = 0;

    private final LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> new IItemHandler() {
        @Override
        public int getSlots() { return 2; } // 0 = input organic, 1 = output dirt

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot == 1 && content == BarrelContent.DIRT) {
                return new ItemStack(Items.DIRT);
            }
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (slot != 0 || !isCompostable(stack)) return stack;
            if (content != BarrelContent.EMPTY && content != BarrelContent.COMPOST) return stack;
            if (compostFill >= MAX_COMPOST) return stack;

            int need = MAX_COMPOST - compostFill;
            int accept = Math.min(need, stack.getCount());
            if (!simulate) {
                compostFill += accept;
                content = BarrelContent.COMPOST;
                syncBlockState();
                setChanged();
            }
            if (accept == stack.getCount()) return ItemStack.EMPTY;
            ItemStack rem = stack.copy();
            rem.shrink(accept);
            return rem;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 1 && content == BarrelContent.DIRT && amount > 0) {
                if (!simulate) {
                    reset();
                }
                return new ItemStack(Items.DIRT, 1);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) { return 64; }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return slot == 0 && isCompostable(stack);
        }
    });

    public CompostBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPOST_BARREL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CompostBarrelBlockEntity be) {
        // Fast composting when full
        if (be.content == BarrelContent.COMPOST && be.compostFill >= MAX_COMPOST) {
            be.compostProgress++;
            if (be.compostProgress >= COMPOST_TIME_TICKS) {
                be.content = BarrelContent.DIRT;
                be.compostFill = 0;
                be.compostProgress = 0;
                be.syncBlockState();
                be.setChanged();
                level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 0.7f, 1.1f);
            }
        }

        // Auto-push ready dirt to inventory below
        if (be.content == BarrelContent.DIRT && level.getGameTime() % 10 == 0) {
            BlockEntity below = level.getBlockEntity(pos.below());
            if (below != null) {
                below.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).ifPresent(handler -> {
                    ItemStack rem = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(Items.DIRT), false);
                    if (rem.isEmpty()) {
                        be.reset();
                    }
                });
            }
        }

        // Collect rainwater rapidly when empty and exposed to sky
        if (be.content == BarrelContent.EMPTY && level.isRainingAt(pos.above())) {
            be.rainProgress++;
            if (be.rainProgress >= 80) { // 4 seconds of rain = full water bucket!
                be.rainProgress = 0;
                be.content = BarrelContent.WATER;
                be.syncBlockState();
                be.setChanged();
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6f, 1.1f);
            }
        }
    }

    public InteractionResult handlePlayerUse(Player player, InteractionHand hand) {
        if (level == null) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);

        // 1. Collect finished Dirt
        if (content == BarrelContent.DIRT) {
            giveOrDrop(player, new ItemStack(Items.DIRT));
            reset();
            level.playSound(null, worldPosition, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 0.8f, 1.0f);
            return InteractionResult.CONSUME;
        }

        // 2. Water reactions
        if (content == BarrelContent.WATER) {
            if (held.is(Items.BUCKET)) {
                replaceHeldBucket(player, hand, held, new ItemStack(Items.WATER_BUCKET));
                reset();
                level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            }
            if (held.is(ModBlocks.DUST_BLOCK.get().asItem())) {
                consumeItem(player, held, 1);
                giveOrDrop(player, new ItemStack(Items.CLAY));
                reset();
                level.playSound(null, worldPosition, SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
                player.displayClientMessage(Component.translatable("message.swiftskyblock.barrel_clay").withStyle(ChatFormatting.AQUA), true);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.BONE_MEAL)) {
                consumeItem(player, held, 1);
                giveOrDrop(player, new ItemStack(Items.ICE));
                reset();
                level.playSound(null, worldPosition, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
                return InteractionResult.CONSUME;
            }
            if (held.is(ItemTags.LEAVES)) {
                consumeItem(player, held, 1);
                giveOrDrop(player, new ItemStack(Items.MOSS_BLOCK));
                reset();
                level.playSound(null, worldPosition, SoundEvents.MOSS_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.RED_MUSHROOM) || held.is(Items.BROWN_MUSHROOM)) {
                consumeItem(player, held, 1);
                giveOrDrop(player, new ItemStack(Items.MYCELIUM));
                reset();
                level.playSound(null, worldPosition, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.LAVA_BUCKET)) {
                replaceHeldBucket(player, hand, held, new ItemStack(Items.BUCKET));
                giveOrDrop(player, new ItemStack(Items.OBSIDIAN));
                reset();
                level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            }
        }

        // 3. Lava reactions
        if (content == BarrelContent.LAVA) {
            if (held.is(Items.BUCKET)) {
                replaceHeldBucket(player, hand, held, new ItemStack(Items.LAVA_BUCKET));
                reset();
                level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.WATER_BUCKET)) {
                replaceHeldBucket(player, hand, held, new ItemStack(Items.BUCKET));
                giveOrDrop(player, new ItemStack(Items.OBSIDIAN));
                reset();
                level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.COBBLESTONE) || held.is(Items.STONE)) {
                consumeItem(player, held, 1);
                giveOrDrop(player, new ItemStack(Items.COBBLED_DEEPSLATE, 2));
                reset();
                level.playSound(null, worldPosition, SoundEvents.DEEPSLATE_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.OBSIDIAN)) {
                consumeItem(player, held, 1);
                giveOrDrop(player, new ItemStack(Items.CRYING_OBSIDIAN));
                reset();
                level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 0.7f, 1.1f);
                return InteractionResult.CONSUME;
            }
            if (held.is(ModBlocks.DUST_BLOCK.get().asItem()) || held.is(Items.REDSTONE)) {
                consumeItem(player, held, 1);
                giveOrDrop(player, new ItemStack(Items.NETHERRACK));
                reset();
                level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.1f);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.SAND) || held.is(Items.RED_SAND)) {
                consumeItem(player, held, 1);
                giveOrDrop(player, new ItemStack(Items.SOUL_SAND));
                reset();
                level.playSound(null, worldPosition, SoundEvents.SOUL_SAND_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.GLOWSTONE_DUST)) {
                consumeItem(player, held, 1);
                giveOrDrop(player, new ItemStack(Items.END_STONE));
                reset();
                level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.3f);
                return InteractionResult.CONSUME;
            }
        }

        // 4. Empty barrel accepts buckets
        if (content == BarrelContent.EMPTY) {
            if (held.is(Items.WATER_BUCKET)) {
                replaceHeldBucket(player, hand, held, new ItemStack(Items.BUCKET));
                content = BarrelContent.WATER;
                syncBlockState();
                setChanged();
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            }
            if (held.is(Items.LAVA_BUCKET)) {
                replaceHeldBucket(player, hand, held, new ItemStack(Items.BUCKET));
                content = BarrelContent.LAVA;
                syncBlockState();
                setChanged();
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 0.8f, 1.0f);
                return InteractionResult.CONSUME;
            }
        }

        // 5. Composting organic matter (sneak inserts up to needed amount at once!)
        if ((content == BarrelContent.EMPTY || content == BarrelContent.COMPOST) && compostFill < MAX_COMPOST && isCompostable(held)) {
            int need = MAX_COMPOST - compostFill;
            int toAdd = player.isShiftKeyDown() ? Math.min(need, held.getCount()) : 1;
            consumeItem(player, held, toAdd);
            compostFill += toAdd;
            content = BarrelContent.COMPOST;
            syncBlockState();
            setChanged();
            level.playSound(null, worldPosition, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.7f, 1.0f + compostFill * 0.05f);
            player.displayClientMessage(
                    Component.translatable("message.swiftskyblock.barrel_compost", compostFill, MAX_COMPOST)
                            .withStyle(ChatFormatting.GREEN),
                    true);
            return InteractionResult.CONSUME;
        }

        if (hand == InteractionHand.MAIN_HAND) {
            player.displayClientMessage(Component.translatable("message.swiftskyblock.barrel_status_" + content.getSerializedName(), compostFill, MAX_COMPOST)
                    .withStyle(ChatFormatting.YELLOW), true);
        }
        return InteractionResult.CONSUME;
    }

    public static boolean isCompostable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (ComposterBlock.COMPOSTABLES.containsKey(item)) return true;
        return stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.LEAVES) || stack.is(ItemTags.FLOWERS)
                || item == Items.ROTTEN_FLESH || item == Items.SPIDER_EYE || item == Items.STRING
                || item == Items.BAMBOO || item == Items.SUGAR_CANE;
    }

    private void consumeItem(Player player, ItemStack stack, int count) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(count);
        }
    }

    private void replaceHeldBucket(Player player, InteractionHand hand, ItemStack held, ItemStack replacement) {
        if (!player.getAbilities().instabuild) {
            if (held.getCount() == 1) {
                player.setItemInHand(hand, replacement);
            } else {
                held.shrink(1);
                giveOrDrop(player, replacement);
            }
        }
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack) && level != null) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack);
        }
    }

    private void reset() {
        content = BarrelContent.EMPTY;
        compostFill = 0;
        compostProgress = 0;
        rainProgress = 0;
        syncBlockState();
        setChanged();
    }

    private void syncBlockState() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            if (state.getBlock() instanceof CompostBarrelBlock && state.getValue(CompostBarrelBlock.CONTENT) != content) {
                level.setBlock(worldPosition, state.setValue(CompostBarrelBlock.CONTENT, content), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Content", content.getSerializedName());
        tag.putInt("CompostFill", compostFill);
        tag.putInt("CompostProgress", compostProgress);
        tag.putInt("RainProgress", rainProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        String name = tag.getString("Content");
        content = BarrelContent.EMPTY;
        for (BarrelContent c : BarrelContent.values()) {
            if (c.getSerializedName().equals(name)) {
                content = c;
                break;
            }
        }
        compostFill = tag.getInt("CompostFill");
        compostProgress = tag.getInt("CompostProgress");
        rainProgress = tag.getInt("RainProgress");
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
