package com.swiftskyblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.swiftskyblock.SwiftSkyblockMod;
import com.swiftskyblock.item.BuildersWandItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Renders a crisp cyan wireframe preview of every block the Builder's Wand is about to place.
 */
@Mod.EventBusSubscriber(modid = SwiftSkyblockMod.MOD_ID, value = Dist.CLIENT)
public class BuildersWandPreviewRenderer {

    @SubscribeEvent
    public static void onBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        ItemStack held = player.getMainHandItem();
        BuildersWandItem wand;
        if (held.getItem() instanceof BuildersWandItem w1) {
            wand = w1;
        } else {
            held = player.getOffhandItem();
            if (held.getItem() instanceof BuildersWandItem w2) {
                wand = w2;
            } else {
                return;
            }
        }

        BlockHitResult target = event.getTarget();
        BlockPos clickedPos = target.getBlockPos();
        BlockState targetState = BuildersWandItem.resolveTargetBlockState(level, player, clickedPos);
        if (targetState == null) return;

        int available = player.getAbilities().instabuild
                ? wand.getMaxBlocks()
                : BuildersWandItem.countItemInInventory(player, targetState.getBlock().asItem());
        if (available <= 0) return;

        List<BlockPos> positions = wand.calculateTargetPositions(
                level, player, held, clickedPos, target.getDirection(), Math.min(wand.getMaxBlocks(), available));
        if (positions.isEmpty()) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        for (BlockPos pos : positions) {
            LevelRenderer.renderLineBox(poseStack, consumer, new AABB(pos).inflate(0.002), 0.25f, 0.92f, 1.0f, 0.85f);
        }
        poseStack.popPose();
    }
}
