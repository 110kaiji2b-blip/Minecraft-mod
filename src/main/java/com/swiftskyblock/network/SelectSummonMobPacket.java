package com.swiftskyblock.network;

import com.swiftskyblock.item.MobSummoningStaffItem;
import com.swiftskyblock.mob.MobSummonRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client-to-server packet that updates the selected mob stored on the player's Mob Summoning Staff.
 */
public class SelectSummonMobPacket {
    private final boolean mainHand;
    private final ResourceLocation entityId;

    public SelectSummonMobPacket(InteractionHand hand, ResourceLocation entityId) {
        this(hand == InteractionHand.MAIN_HAND, entityId);
    }

    private SelectSummonMobPacket(boolean mainHand, ResourceLocation entityId) {
        this.mainHand = mainHand;
        this.entityId = entityId;
    }

    public static void encode(SelectSummonMobPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.mainHand);
        buf.writeResourceLocation(msg.entityId);
    }

    public static SelectSummonMobPacket decode(FriendlyByteBuf buf) {
        return new SelectSummonMobPacket(buf.readBoolean(), buf.readResourceLocation());
    }

    public static void handle(SelectSummonMobPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            InteractionHand hand = msg.mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof MobSummoningStaffItem)) {
                return;
            }

            EntityType<?> type = MobSummonRegistry.byId(msg.entityId);
            if (type == null || !MobSummonRegistry.isAllowed(type)) {
                return;
            }

            MobSummoningStaffItem.setSelectedMob(stack, msg.entityId);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 1.3f);
            player.displayClientMessage(
                    Component.translatable("message.swiftskyblock.staff_mob_selected", type.getDescription())
                            .withStyle(ChatFormatting.AQUA),
                    true);
        });
        ctx.setPacketHandled(true);
    }
}
