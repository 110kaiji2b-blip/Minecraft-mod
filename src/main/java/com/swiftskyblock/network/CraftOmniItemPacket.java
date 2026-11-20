package com.swiftskyblock.network;

import com.swiftskyblock.blockentity.OmniSynthesizerBlockEntity;
import com.swiftskyblock.omni.OmniValueRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * Client-to-server packet sent when a player clicks an item in the Omni Synthesizer catalog.
 */
public class CraftOmniItemPacket {
    private final BlockPos pos;
    private final ResourceLocation itemId;
    private final int count;

    public CraftOmniItemPacket(BlockPos pos, ResourceLocation itemId, int count) {
        this.pos = pos;
        this.itemId = itemId;
        this.count = count;
    }

    public static void encode(CraftOmniItemPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeResourceLocation(msg.itemId);
        buf.writeVarInt(msg.count);
    }

    public static CraftOmniItemPacket decode(FriendlyByteBuf buf) {
        return new CraftOmniItemPacket(buf.readBlockPos(), buf.readResourceLocation(), buf.readVarInt());
    }

    public static void handle(CraftOmniItemPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.distanceToSqr(msg.pos.getX() + 0.5, msg.pos.getY() + 0.5, msg.pos.getZ() + 0.5) > 64.0) {
                return;
            }

            BlockEntity be = player.level().getBlockEntity(msg.pos);
            if (!(be instanceof OmniSynthesizerBlockEntity altar)) {
                return;
            }

            Item item = ForgeRegistries.ITEMS.getValue(msg.itemId);
            if (item == null || !OmniValueRegistry.isAllowedInCatalog(item)) {
                return;
            }

            int unitCost = OmniValueRegistry.getBuyCost(item);
            int maxStack = Math.max(1, item.getMaxStackSize());
            int requested = Math.max(1, Math.min(msg.count, maxStack));
            int affordable = altar.getVoidPoints() / unitCost;
            int actualCount = Math.min(requested, affordable);
            if (actualCount <= 0) {
                return;
            }

            long totalCost = (long) unitCost * actualCount;
            if (totalCost > altar.getVoidPoints()) {
                return;
            }

            if (altar.consumeVoidPoints((int) totalCost)) {
                ItemStack created = new ItemStack(item, actualCount);
                if (!player.getInventory().add(created)) {
                    Containers.dropItemStack(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), created);
                }
                player.level().playSound(null, msg.pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5f, 1.35f);
            }
        });
        ctx.setPacketHandled(true);
    }
}
