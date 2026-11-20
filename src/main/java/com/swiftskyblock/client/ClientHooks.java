package com.swiftskyblock.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import javax.annotation.Nullable;

/**
 * Client-only screen launcher separated from common Item code so dedicated servers never load client classes.
 */
public class ClientHooks {
    public static void openMobSummonerScreen(InteractionHand hand, @Nullable ResourceLocation currentMobId) {
        Minecraft.getInstance().setScreen(new MobSummonerScreen(hand, currentMobId));
    }
}
