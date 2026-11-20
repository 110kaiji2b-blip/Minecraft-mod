package com.swiftskyblock.omni;

import com.swiftskyblock.registry.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Defines Void Point (VP) sell values, buy costs, and allowed catalog items
 * for the Omni Synthesizer so players who progress enough can obtain virtually
 * any survival item in Minecraft without infinite-value exploits.
 */
public class OmniValueRegistry {

    private static final Set<Item> BANNED_ITEMS = Set.of(
            Items.AIR,
            Items.BEDROCK,
            Items.BARRIER,
            Items.COMMAND_BLOCK,
            Items.CHAIN_COMMAND_BLOCK,
            Items.REPEATING_COMMAND_BLOCK,
            Items.COMMAND_BLOCK_MINECART,
            Items.STRUCTURE_BLOCK,
            Items.STRUCTURE_VOID,
            Items.JIGSAW,
            Items.LIGHT,
            Items.DEBUG_STICK,
            Items.KNOWLEDGE_BOOK,
            Items.SPAWNER,
            Items.REINFORCED_DEEPSLATE,
            Items.PETRIFIED_OAK_SLAB
    );

    private static List<Item> cachedCatalog = null;

    public static boolean isAllowedInCatalog(Item item) {
        if (item == null || BANNED_ITEMS.contains(item)) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null;
    }

    public static List<Item> getCatalogItems() {
        if (cachedCatalog == null) {
            List<Item> list = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (isAllowedInCatalog(item)) {
                    list.add(item);
                }
            }
            // Sort nicely: vanilla items first (by cost then ID), modded items after
            list.sort(Comparator
                    .comparing((Item i) -> {
                        ResourceLocation k = ForgeRegistries.ITEMS.getKey(i);
                        return k != null && "minecraft".equals(k.getNamespace()) ? 0 : 1;
                    })
                    .thenComparingInt(OmniValueRegistry::getBuyCost)
                    .thenComparing(i -> String.valueOf(ForgeRegistries.ITEMS.getKey(i))));
            cachedCatalog = List.copyOf(list);
        }
        return cachedCatalog;
    }

    /**
     * Returns how many Void Points (VP) are gained by sacrificing 1 of the given ItemStack.
     */
    public static int getSellValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (BANNED_ITEMS.contains(item)) return 0;

        // Compressed Skyblock blocks
        if (item == ModBlocks.COMPRESSED_COBBLESTONE.get().asItem()
                || item == ModBlocks.COMPRESSED_GRAVEL.get().asItem()
                || item == ModBlocks.COMPRESSED_SAND.get().asItem()) {
            return 9;
        }
        if (item == ModBlocks.DOUBLE_COMPRESSED_COBBLESTONE.get().asItem()) {
            return 81;
        }

        // Common bulk resources
        if (item == Items.COBBLESTONE || item == Items.STONE || item == Items.DIRT || item == Items.GRAVEL
                || item == Items.SAND || item == Items.RED_SAND || item == Items.NETHERRACK
                || item == Items.COBBLED_DEEPSLATE || item == Items.DEEPSLATE
                || item == ModBlocks.DUST_BLOCK.get().asItem()) {
            return 1;
        }

        // Valuable minerals & resources
        if (item == Items.COAL || item == Items.CHARCOAL || item == Items.RAW_COPPER || item == Items.COPPER_INGOT) return 8;
        if (item == Items.COAL_BLOCK || item == Items.COPPER_BLOCK) return 72;
        if (item == Items.REDSTONE || item == Items.LAPIS_LAZULI || item == Items.QUARTZ || item == Items.GLOWSTONE_DUST) return 12;
        if (item == Items.REDSTONE_BLOCK || item == Items.LAPIS_BLOCK) return 108;
        if (item == Items.RAW_IRON || item == Items.IRON_INGOT) return 25;
        if (item == Items.IRON_BLOCK) return 225;
        if (item == Items.RAW_GOLD || item == Items.GOLD_INGOT) return 60;
        if (item == Items.GOLD_BLOCK) return 540;
        if (item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN || item == Items.ENDER_PEARL || item == Items.BLAZE_ROD) return 40;
        if (item == Items.EMERALD) return 150;
        if (item == Items.EMERALD_BLOCK) return 1350;
        if (item == Items.DIAMOND) return 300;
        if (item == Items.DIAMOND_BLOCK) return 2700;
        if (item == Items.ANCIENT_DEBRIS || item == Items.NETHERITE_SCRAP) return 800;
        if (item == Items.NETHERITE_INGOT) return 3500;
        if (item == Items.NETHERITE_BLOCK) return 31500;
        if (item == Items.NETHER_STAR) return 10000;

        // Default based on rarity
        Rarity rarity = stack.getRarity();
        if (rarity == Rarity.EPIC) return 1000;
        if (rarity == Rarity.RARE) return 250;
        if (rarity == Rarity.UNCOMMON) return 50;
        return 3;
    }

    /**
     * Returns the Void Point (VP) cost required to synthesize 1 of the given Item.
     * Always strictly greater than sell value (at least 2x) to prevent infinite VP loops.
     */
    public static int getBuyCost(Item item) {
        ItemStack sample = new ItemStack(item);
        int sell = getSellValue(sample);
        int baseCost;

        if (item == Items.NETHER_STAR || item == Items.DRAGON_EGG) {
            baseCost = 15000;
        } else if (item == Items.ELYTRA || item == Items.BEACON || item == Items.ENCHANTED_GOLDEN_APPLE
                || item == Items.END_PORTAL_FRAME) {
            baseCost = 6000;
        } else if (item == Items.TRIDENT || item == Items.TOTEM_OF_UNDYING || item == Items.HEART_OF_THE_SEA
                || item == Items.CONDUIT || item == Items.SHULKER_SHELL || item == Items.SHULKER_BOX
                || item == Items.SPONGE || item == Items.WET_SPONGE || item == Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE
                || item == Items.WITHER_SKELETON_SKULL || item == Items.DRAGON_HEAD) {
            baseCost = 2500;
        } else if (item instanceof SpawnEggItem) {
            if (item == Items.VILLAGER_SPAWN_EGG || item == Items.SNIFFER_SPAWN_EGG
                    || item == Items.ALLAY_SPAWN_EGG || item == Items.MOOSHROOM_SPAWN_EGG) {
                baseCost = 2000;
            } else {
                baseCost = 1200;
            }
        } else {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            String path = id != null ? id.getPath() : "";
            if (path.contains("smithing_template") || path.contains("pottery_sherd")
                    || path.contains("music_disc") || path.contains("banner_pattern")
                    || path.endsWith("_head") || path.endsWith("_skull")
                    || item == Items.ECHO_SHARD || item == Items.RECOVERY_COMPASS
                    || item == Items.GOAT_HORN || item == Items.SNIFFER_EGG) {
                baseCost = 800;
            } else if (path.contains("netherite")) {
                baseCost = 5000;
            } else if (path.contains("diamond")) {
                baseCost = 700;
            } else if (path.contains("shulker_box")) {
                baseCost = 3000;
            } else if (sample.is(ItemTags.SAPLINGS) || sample.is(ItemTags.FLOWERS) || sample.is(ItemTags.LOGS)) {
                baseCost = 15;
            } else {
                Rarity rarity = sample.getRarity();
                if (rarity == Rarity.EPIC) {
                    baseCost = 5000;
                } else if (rarity == Rarity.RARE) {
                    baseCost = 1500;
                } else if (rarity == Rarity.UNCOMMON) {
                    baseCost = 350;
                } else {
                    baseCost = 20;
                }
            }
        }

        // Strict anti-exploit guarantee: buy cost is always at least 2x sell value
        return Math.max(baseCost, Math.max(10, sell * 2));
    }
}
