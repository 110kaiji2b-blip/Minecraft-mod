package com.swiftskyblock.menu;

import com.swiftskyblock.blockentity.OmniSynthesizerBlockEntity;
import com.swiftskyblock.omni.OmniValueRegistry;
import com.swiftskyblock.registry.ModBlocks;
import com.swiftskyblock.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public class OmniSynthesizerMenu extends AbstractContainerMenu {
    public static final int CHARGE_SLOT_X = 16;
    public static final int CHARGE_SLOT_Y = 126;
    public static final int INV_START_X = 16;
    public static final int INV_START_Y = 152;
    public static final int HOTBAR_Y = 210;

    private final BlockPos blockPos;
    @Nullable
    private final OmniSynthesizerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Client constructor (via Forge IForgeMenuType)
    public OmniSynthesizerMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, resolveBlockEntity(playerInv.player.level(), extraData.readBlockPos()), new SimpleContainerData(2));
    }

    private static OmniSynthesizerBlockEntity resolveBlockEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof OmniSynthesizerBlockEntity altar ? altar : null;
    }

    // Server constructor
    public OmniSynthesizerMenu(int containerId, Inventory playerInv, @Nullable OmniSynthesizerBlockEntity be, ContainerData data) {
        super(ModMenus.OMNI_SYNTHESIZER.get(), containerId);
        this.blockEntity = be;
        this.blockPos = be != null ? be.getBlockPos() : BlockPos.ZERO;
        this.level = playerInv.player.level();
        this.data = data;

        // Slot 0: Instant VP charge slot
        Container chargeContainer = new SimpleContainer(1) {
            @Override
            public void setItem(int slot, ItemStack stack) {
                if (!stack.isEmpty() && !level.isClientSide && blockEntity != null) {
                    int gained = blockEntity.depositItemStack(stack, true);
                    if (gained > 0 && stack.isEmpty()) {
                        super.setItem(slot, ItemStack.EMPTY);
                        return;
                    }
                }
                super.setItem(slot, stack);
            }
        };

        this.addSlot(new Slot(chargeContainer, 0, CHARGE_SLOT_X, CHARGE_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return OmniValueRegistry.getSellValue(stack) > 0;
            }
        });

        // Player Inventory (3 rows x 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, INV_START_X + col * 18, INV_START_Y + row * 18));
            }
        }

        // Hotbar (1 row x 9)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, INV_START_X + col * 18, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public int getVoidPoints() {
        int low = data.get(0) & 0xFFFF;
        int high = data.get(1) & 0xFFFF;
        return low | (high << 16);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        if (index == 0) {
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Shift-clicking an item from player inventory deposits it directly into VP!
            if (OmniValueRegistry.getSellValue(stack) > 0) {
                if (!level.isClientSide && blockEntity != null) {
                    blockEntity.depositItemStack(stack, true);
                    slot.setChanged();
                }
                return ItemStack.EMPTY;
            }
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockPos), player, ModBlocks.OMNI_SYNTHESIZER.get());
    }
}
