package com.swiftskyblock.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Builder's Wand: makes expanding Skyblock platforms, walls, and bridges effortless!
 * Supports 3 modes (Sneak + Right-click to cycle):
 *  - EXTEND: extends connected matching blocks along the clicked face.
 *  - PLATFORM: places a flat 3x3 (Stone) or 5x5 (Diamond) platform.
 *  - BRIDGE: extends a straight horizontal bridge away from the player (5 or 12 blocks).
 */
public class BuildersWandItem extends Item {
    private static final String TAG_MODE = "WandMode";

    public enum WandMode {
        EXTEND("extend"),
        PLATFORM("platform"),
        BRIDGE("bridge");

        private final String key;
        WandMode(String key) { this.key = key; }
        public String getKey() { return key; }

        public WandMode next() {
            WandMode[] vals = values();
            return vals[(ordinal() + 1) % vals.length];
        }
    }

    private final int maxBlocks;
    private final int platformRadius;
    private final int bridgeLength;

    public BuildersWandItem(int maxBlocks, int platformRadius, int bridgeLength, Properties properties) {
        super(properties);
        this.maxBlocks = maxBlocks;
        this.platformRadius = platformRadius;
        this.bridgeLength = bridgeLength;
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public static WandMode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_MODE)) {
            int ord = tag.getInt(TAG_MODE);
            WandMode[] vals = WandMode.values();
            if (ord >= 0 && ord < vals.length) {
                return vals[ord];
            }
        }
        return WandMode.EXTEND;
    }

    public static void setMode(ItemStack stack, WandMode mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode.ordinal());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                WandMode next = getMode(stack).next();
                setMode(stack, next);
                player.displayClientMessage(
                        Component.translatable("message.swiftskyblock.wand_mode",
                                Component.translatable("wand_mode.swiftskyblock." + next.getKey()))
                                .withStyle(ChatFormatting.AQUA),
                        true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // Extended reach placement up to 7.5 blocks so players don't have to lean over the void!
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(7.5));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            InteractionResult res = tryPlaceWithWand(level, player, stack, hit.getBlockPos(), hit.getDirection());
            if (res.consumesAction()) {
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();

        if (player.isShiftKeyDown()) {
            if (!context.getLevel().isClientSide) {
                WandMode next = getMode(stack).next();
                setMode(stack, next);
                player.displayClientMessage(
                        Component.translatable("message.swiftskyblock.wand_mode",
                                Component.translatable("wand_mode.swiftskyblock." + next.getKey()))
                                .withStyle(ChatFormatting.AQUA),
                        true);
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }

        return tryPlaceWithWand(context.getLevel(), player, stack, context.getClickedPos(), context.getClickedFace());
    }

    private InteractionResult tryPlaceWithWand(Level level, Player player, ItemStack wand, BlockPos clickedPos, Direction face) {
        BlockState targetState = resolveTargetBlockState(level, player, clickedPos);
        if (targetState == null) {
            return InteractionResult.PASS;
        }

        Item blockItem = targetState.getBlock().asItem();
        int available = player.getAbilities().instabuild ? maxBlocks : countItemInInventory(player, blockItem);
        if (available <= 0) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.swiftskyblock.wand_no_blocks", targetState.getBlock().getName())
                                .withStyle(ChatFormatting.RED),
                        true);
            }
            return InteractionResult.FAIL;
        }

        List<BlockPos> positions = calculateTargetPositions(level, player, wand, clickedPos, face, Math.min(maxBlocks, available));
        if (positions.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        int placed = 0;
        for (BlockPos pos : positions) {
            if (!player.getAbilities().instabuild && !consumeOneItem(player, blockItem)) {
                break;
            }
            if (level.setBlock(pos, targetState, 3)) {
                placed++;
            }
        }

        if (placed > 0) {
            SoundType sound = targetState.getSoundType();
            level.playSound(null, clickedPos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.9f);
            if (level instanceof ServerLevel serverLevel) {
                BlockPos sample = positions.get(positions.size() - 1);
                serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, targetState),
                        sample.getX() + 0.5, sample.getY() + 0.5, sample.getZ() + 0.5,
                        Math.min(20, placed * 2), 0.35, 0.25, 0.35, 0.05);
            }
            player.displayClientMessage(
                    Component.translatable("message.swiftskyblock.wand_placed", placed)
                            .withStyle(ChatFormatting.GREEN),
                    true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /**
     * Resolves which block to place: if the player holds a BlockItem in their offhand,
     * uses that block (great for building with a specific material); otherwise matches the clicked block.
     */
    @Nullable
    public static BlockState resolveTargetBlockState(Level level, Player player, BlockPos clickedPos) {
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() instanceof BlockItem offBlockItem) {
            Block b = offBlockItem.getBlock();
            if (!b.defaultBlockState().hasBlockEntity()) {
                return b.defaultBlockState();
            }
        }
        BlockState clicked = level.getBlockState(clickedPos);
        if (clicked.isAir() || clicked.hasBlockEntity() || clicked.getBlock().asItem() == net.minecraft.world.item.Items.AIR) {
            return null;
        }
        return clicked.getBlock().defaultBlockState();
    }

    /**
     * Computes all valid target positions for both placement and client wireframe preview.
     */
    public List<BlockPos> calculateTargetPositions(Level level, Player player, ItemStack wand, BlockPos clickedPos, Direction face, int limit) {
        WandMode mode = getMode(wand);
        List<BlockPos> result = new ArrayList<>();
        if (limit <= 0) return result;

        AABB playerBox = player.getBoundingBox();

        if (mode == WandMode.BRIDGE) {
            Direction horiz = player.getDirection();
            BlockPos start = (face.getAxis().isHorizontal()) ? clickedPos.relative(face) : clickedPos.relative(horiz);
            Direction stepDir = (face.getAxis().isHorizontal()) ? face : horiz;
            int maxLen = Math.min(limit, bridgeLength);
            for (int i = 0; i < maxLen; i++) {
                BlockPos pos = start.relative(stepDir, i);
                if (canPlaceAt(level, pos, playerBox)) {
                    result.add(pos);
                } else {
                    break;
                }
            }
            return result;
        }

        if (mode == WandMode.PLATFORM) {
            BlockPos anchor = clickedPos.relative(face);
            int r = platformRadius;
            for (int dx = -r; dx <= r && result.size() < limit; dx++) {
                for (int dz = -r; dz <= r && result.size() < limit; dz++) {
                    BlockPos pos = anchor.offset(dx, 0, dz);
                    if (canPlaceAt(level, pos, playerBox)) {
                        result.add(pos);
                    }
                }
            }
            return result;
        }

        // Default EXTEND mode: BFS over coplanar matching blocks perpendicular to the clicked face
        Block matchBlock = level.getBlockState(clickedPos).getBlock();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(clickedPos);
        visited.add(clickedPos);

        Direction[] planeDirs = getPerpendicularDirections(face);
        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos source = queue.poll();
            BlockPos placePos = source.relative(face);
            if (!canPlaceAt(level, placePos, playerBox)) {
                continue;
            }
            result.add(placePos);

            for (Direction d : planeDirs) {
                BlockPos next = source.relative(d);
                if (visited.add(next) && level.getBlockState(next).is(matchBlock)) {
                    queue.add(next);
                }
            }
        }
        return result;
    }

    private static boolean canPlaceAt(Level level, BlockPos pos, AABB playerBox) {
        if (!level.isInWorldBounds(pos)) return false;
        BlockState existing = level.getBlockState(pos);
        if (!existing.canBeReplaced()) return false;
        return !playerBox.intersects(new AABB(pos));
    }

    private static Direction[] getPerpendicularDirections(Direction face) {
        return switch (face.getAxis()) {
            case Y -> new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            case X -> new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
            case Z -> new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
        };
    }

    public static int countItemInInventory(Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) total += s.getCount();
        }
        return total;
    }

    private static boolean consumeOneItem(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item) && !s.isEmpty()) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        WandMode mode = getMode(stack);
        tooltip.add(Component.translatable("tooltip.swiftskyblock.wand.mode",
                Component.translatable("wand_mode.swiftskyblock." + mode.getKey())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.swiftskyblock.wand.limit", maxBlocks).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.swiftskyblock.wand.usage1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.swiftskyblock.wand.usage2").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
