package com.swiftskyblock.sieve;

import com.swiftskyblock.registry.ModBlocks;
import com.swiftskyblock.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Defines generous, low-wait sieve loot tables so players can progress smoothly without tedious grinding.
 */
public class SieveLootTable {

    public record LootEntry(Supplier<Item> item, float chance, int minCount, int maxCount) {}

    private static final List<LootEntry> DIRT_DROPS = List.of(
            new LootEntry(ModItems.STONE_PEBBLE, 0.90f, 1, 3),
            new LootEntry(() -> Items.WHEAT_SEEDS, 0.35f, 1, 2),
            new LootEntry(() -> Items.OAK_SAPLING, 0.18f, 1, 1),
            new LootEntry(() -> Items.BIRCH_SAPLING, 0.10f, 1, 1),
            new LootEntry(() -> Items.SPRUCE_SAPLING, 0.10f, 1, 1),
            new LootEntry(() -> Items.JUNGLE_SAPLING, 0.08f, 1, 1),
            new LootEntry(() -> Items.ACACIA_SAPLING, 0.08f, 1, 1),
            new LootEntry(() -> Items.DARK_OAK_SAPLING, 0.08f, 1, 1),
            new LootEntry(() -> Items.CHERRY_SAPLING, 0.08f, 1, 1),
            new LootEntry(() -> Items.PUMPKIN_SEEDS, 0.12f, 1, 1),
            new LootEntry(() -> Items.MELON_SEEDS, 0.12f, 1, 1),
            new LootEntry(() -> Items.BEETROOT_SEEDS, 0.12f, 1, 1),
            new LootEntry(() -> Items.CARROT, 0.10f, 1, 1),
            new LootEntry(() -> Items.POTATO, 0.10f, 1, 1),
            new LootEntry(() -> Items.SUGAR_CANE, 0.12f, 1, 1),
            new LootEntry(() -> Items.BAMBOO, 0.10f, 1, 1),
            new LootEntry(() -> Items.CACTUS, 0.08f, 1, 1),
            new LootEntry(() -> Items.STRING, 0.20f, 1, 2),
            new LootEntry(() -> Items.BONE_MEAL, 0.25f, 1, 2)
    );

    private static final List<LootEntry> GRAVEL_DROPS = List.of(
            new LootEntry(() -> Items.RAW_IRON, 0.42f, 1, 2),
            new LootEntry(() -> Items.COAL, 0.38f, 1, 2),
            new LootEntry(() -> Items.RAW_COPPER, 0.30f, 1, 2),
            new LootEntry(() -> Items.FLINT, 0.25f, 1, 1),
            new LootEntry(() -> Items.RAW_GOLD, 0.18f, 1, 1),
            new LootEntry(() -> Items.LAPIS_LAZULI, 0.15f, 1, 3),
            new LootEntry(() -> Items.DIAMOND, 0.06f, 1, 1),
            new LootEntry(() -> Items.EMERALD, 0.04f, 1, 1)
    );

    private static final List<LootEntry> SAND_DROPS = List.of(
            new LootEntry(() -> Items.RAW_IRON, 0.28f, 1, 1),
            new LootEntry(() -> Items.RAW_GOLD, 0.22f, 1, 1),
            new LootEntry(() -> Items.REDSTONE, 0.30f, 1, 2),
            new LootEntry(() -> Items.QUARTZ, 0.22f, 1, 2),
            new LootEntry(() -> Items.COCOA_BEANS, 0.10f, 1, 1),
            new LootEntry(() -> Items.KELP, 0.12f, 1, 1),
            new LootEntry(() -> Items.PRISMARINE_SHARD, 0.08f, 1, 1),
            new LootEntry(() -> Items.DIAMOND, 0.05f, 1, 1)
    );

    private static final List<LootEntry> DUST_DROPS = List.of(
            new LootEntry(() -> Items.REDSTONE, 0.42f, 1, 3),
            new LootEntry(() -> Items.GLOWSTONE_DUST, 0.30f, 1, 2),
            new LootEntry(() -> Items.BONE_MEAL, 0.35f, 1, 2),
            new LootEntry(() -> Items.GUNPOWDER, 0.22f, 1, 2),
            new LootEntry(() -> Items.SLIME_BALL, 0.12f, 1, 1),
            new LootEntry(() -> Items.BLAZE_POWDER, 0.10f, 1, 1),
            new LootEntry(() -> Items.ENDER_PEARL, 0.06f, 1, 1),
            new LootEntry(() -> Items.AMETHYST_SHARD, 0.08f, 1, 1)
    );

    private static final List<LootEntry> SOUL_SAND_DROPS = List.of(
            new LootEntry(() -> Items.QUARTZ, 0.55f, 1, 3),
            new LootEntry(() -> Items.NETHER_WART, 0.25f, 1, 2),
            new LootEntry(() -> Items.GLOWSTONE_DUST, 0.30f, 1, 2),
            new LootEntry(() -> Items.BLAZE_POWDER, 0.18f, 1, 2),
            new LootEntry(() -> Items.GHAST_TEAR, 0.08f, 1, 1),
            new LootEntry(() -> Items.NETHERITE_SCRAP, 0.025f, 1, 1)
    );

    public static boolean isSiftable(ItemStack stack) {
        return getMultiplier(stack) > 0;
    }

    /**
     * Returns how many base rolls a single item represents (e.g. 9 for compressed blocks, 1 for regular blocks, 0 if invalid).
     */
    public static int getMultiplier(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (item == ModBlocks.COMPRESSED_GRAVEL.get().asItem() || item == ModBlocks.COMPRESSED_SAND.get().asItem()) {
            return 9;
        }
        return getEntries(item) != null ? 1 : 0;
    }

    private static List<LootEntry> getEntries(Item item) {
        Block block = Block.byItem(item);
        if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.ROOTED_DIRT) {
            return DIRT_DROPS;
        }
        if (block == Blocks.GRAVEL || block == ModBlocks.COMPRESSED_GRAVEL.get()) {
            return GRAVEL_DROPS;
        }
        if (block == Blocks.SAND || block == Blocks.RED_SAND || block == ModBlocks.COMPRESSED_SAND.get()) {
            return SAND_DROPS;
        }
        if (block == ModBlocks.DUST_BLOCK.get()) {
            return DUST_DROPS;
        }
        if (block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL) {
            return SOUL_SAND_DROPS;
        }
        return null;
    }

    /**
     * Rolls the sieve table {@code count} times for the given item and consolidates drops into clean ItemStacks.
     */
    public static List<ItemStack> rollDrops(Item inputItem, int count, RandomSource random) {
        List<LootEntry> entries = getEntries(inputItem);
        if (entries == null || count <= 0) {
            return List.of();
        }
        int mult = (inputItem == ModBlocks.COMPRESSED_GRAVEL.get().asItem() || inputItem == ModBlocks.COMPRESSED_SAND.get().asItem()) ? 9 : 1;
        int totalRolls = count * mult;

        Map<Item, Integer> totals = new LinkedHashMap<>();
        for (int i = 0; i < totalRolls; i++) {
            for (LootEntry entry : entries) {
                if (random.nextFloat() < entry.chance()) {
                    int qty = entry.minCount();
                    if (entry.maxCount() > entry.minCount()) {
                        qty += random.nextInt(entry.maxCount() - entry.minCount() + 1);
                    }
                    Item dropItem = entry.item().get();
                    totals.merge(dropItem, qty, Integer::sum);
                }
            }
        }

        // Guarantee at least 1 helpful item if RNG somehow missed on small batches
        if (totals.isEmpty() && !entries.isEmpty()) {
            totals.put(entries.get(0).item().get(), 1);
        }

        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : totals.entrySet()) {
            Item item = e.getKey();
            int remaining = e.getValue();
            int maxStack = item.getMaxStackSize();
            while (remaining > 0) {
                int take = Math.min(remaining, maxStack);
                result.add(new ItemStack(item, take));
                remaining -= take;
            }
        }
        return result;
    }
}
