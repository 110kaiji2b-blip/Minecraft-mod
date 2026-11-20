package com.swiftskyblock.block;

import com.swiftskyblock.blockentity.CompostBarrelBlockEntity;
import com.swiftskyblock.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Rapid Compost Barrel:
 * - Composts organic items into Dirt in just 2 seconds!
 * - Collects rainwater quickly or accepts Water/Lava buckets.
 * - Transforms Water + Dust -> Clay, Lava + Water -> Obsidian,
 *   Lava + Dust -> Netherrack, Lava + Sand -> Soul Sand, Lava + Glowstone -> End Stone.
 */
public class CompostBarrelBlock extends BaseEntityBlock {

    public enum BarrelContent implements StringRepresentable {
        EMPTY("empty"),
        COMPOST("compost"),
        DIRT("dirt"),
        WATER("water"),
        LAVA("lava");

        private final String name;
        BarrelContent(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }
    }

    public static final EnumProperty<BarrelContent> CONTENT = EnumProperty.create("content", BarrelContent.class);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public CompostBarrelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CONTENT, BarrelContent.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CompostBarrelBlockEntity barrel) {
            return barrel.handlePlayerUse(player, hand);
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompostBarrelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.COMPOST_BARREL.get(), CompostBarrelBlockEntity::serverTick);
    }
}
