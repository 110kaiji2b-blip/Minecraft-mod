package com.swiftskyblock.registry;

import com.swiftskyblock.SwiftSkyblockMod;
import com.swiftskyblock.item.GuideBookItem;
import com.swiftskyblock.item.HammerItem;
import com.swiftskyblock.item.SkyMagnetItem;
import com.swiftskyblock.item.TimePocketWatchItem;
import com.swiftskyblock.item.TooltipBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SwiftSkyblockMod.MOD_ID);

    // Basic resources & utility items
    public static final RegistryObject<Item> STONE_PEBBLE = ITEMS.register("stone_pebble",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SKYBLOCK_GUIDE = ITEMS.register("skyblock_guide",
            () -> new GuideBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> SKY_MAGNET = ITEMS.register("sky_magnet",
            () -> new SkyMagnetItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> TIME_POCKET_WATCH = ITEMS.register("time_pocket_watch",
            () -> new TimePocketWatchItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // Crushing Hammers (generous durability for zero-annoyance play)
    public static final RegistryObject<Item> WOODEN_HAMMER = ITEMS.register("wooden_hammer",
            () -> new HammerItem(Tiers.WOOD, 2.0f, -2.6f, new Item.Properties().durability(160)));

    public static final RegistryObject<Item> STONE_HAMMER = ITEMS.register("stone_hammer",
            () -> new HammerItem(Tiers.STONE, 3.0f, -2.6f, new Item.Properties().durability(360)));

    public static final RegistryObject<Item> IRON_HAMMER = ITEMS.register("iron_hammer",
            () -> new HammerItem(Tiers.IRON, 4.0f, -2.5f, new Item.Properties().durability(800)));

    public static final RegistryObject<Item> DIAMOND_HAMMER = ITEMS.register("diamond_hammer",
            () -> new HammerItem(Tiers.DIAMOND, 5.0f, -2.4f, new Item.Properties().durability(2500)));

    public static final RegistryObject<Item> NETHERITE_HAMMER = ITEMS.register("netherite_hammer",
            () -> new HammerItem(Tiers.NETHERITE, 6.0f, -2.3f, new Item.Properties().durability(4000).fireResistant()));

    // Block Items
    public static final RegistryObject<Item> DUST_BLOCK_ITEM = ITEMS.register("dust_block",
            () -> new TooltipBlockItem(ModBlocks.DUST_BLOCK.get(), new Item.Properties(), "tooltip.swiftskyblock.dust_block"));

    public static final RegistryObject<Item> COMPRESSED_COBBLESTONE_ITEM = ITEMS.register("compressed_cobblestone",
            () -> new TooltipBlockItem(ModBlocks.COMPRESSED_COBBLESTONE.get(), new Item.Properties(), "tooltip.swiftskyblock.compressed_block"));

    public static final RegistryObject<Item> DOUBLE_COMPRESSED_COBBLESTONE_ITEM = ITEMS.register("double_compressed_cobblestone",
            () -> new TooltipBlockItem(ModBlocks.DOUBLE_COMPRESSED_COBBLESTONE.get(), new Item.Properties(), "tooltip.swiftskyblock.double_compressed_block"));

    public static final RegistryObject<Item> COMPRESSED_GRAVEL_ITEM = ITEMS.register("compressed_gravel",
            () -> new TooltipBlockItem(ModBlocks.COMPRESSED_GRAVEL.get(), new Item.Properties(), "tooltip.swiftskyblock.compressed_block"));

    public static final RegistryObject<Item> COMPRESSED_SAND_ITEM = ITEMS.register("compressed_sand",
            () -> new TooltipBlockItem(ModBlocks.COMPRESSED_SAND.get(), new Item.Properties(), "tooltip.swiftskyblock.compressed_block"));

    public static final RegistryObject<Item> RAPID_SIEVE_ITEM = ITEMS.register("rapid_sieve",
            () -> new TooltipBlockItem(ModBlocks.RAPID_SIEVE.get(), new Item.Properties(), "tooltip.swiftskyblock.rapid_sieve"));

    public static final RegistryObject<Item> COMPOST_BARREL_ITEM = ITEMS.register("compost_barrel",
            () -> new TooltipBlockItem(ModBlocks.COMPOST_BARREL.get(), new Item.Properties(), "tooltip.swiftskyblock.compost_barrel"));

    public static final RegistryObject<Item> COBBLE_GEN_TIER1_ITEM = ITEMS.register("cobble_gen_tier1",
            () -> new TooltipBlockItem(ModBlocks.COBBLE_GEN_TIER1.get(), new Item.Properties(), "tooltip.swiftskyblock.cobble_gen_tier1"));

    public static final RegistryObject<Item> COBBLE_GEN_TIER2_ITEM = ITEMS.register("cobble_gen_tier2",
            () -> new TooltipBlockItem(ModBlocks.COBBLE_GEN_TIER2.get(), new Item.Properties(), "tooltip.swiftskyblock.cobble_gen_tier2"));

    public static final RegistryObject<Item> COBBLE_GEN_TIER3_ITEM = ITEMS.register("cobble_gen_tier3",
            () -> new TooltipBlockItem(ModBlocks.COBBLE_GEN_TIER3.get(), new Item.Properties().rarity(Rarity.UNCOMMON), "tooltip.swiftskyblock.cobble_gen_tier3"));

    public static final RegistryObject<Item> TURBO_FURNACE_ITEM = ITEMS.register("turbo_furnace",
            () -> new TooltipBlockItem(ModBlocks.TURBO_FURNACE.get(), new Item.Properties(), "tooltip.swiftskyblock.turbo_furnace"));

    public static final RegistryObject<Item> RAPID_CRUCIBLE_ITEM = ITEMS.register("rapid_crucible",
            () -> new TooltipBlockItem(ModBlocks.RAPID_CRUCIBLE.get(), new Item.Properties(), "tooltip.swiftskyblock.rapid_crucible"));

    public static final RegistryObject<Item> ACCELERATION_LANTERN_ITEM = ITEMS.register("acceleration_lantern",
            () -> new TooltipBlockItem(ModBlocks.ACCELERATION_LANTERN.get(), new Item.Properties().rarity(Rarity.RARE), "tooltip.swiftskyblock.acceleration_lantern"));
}
