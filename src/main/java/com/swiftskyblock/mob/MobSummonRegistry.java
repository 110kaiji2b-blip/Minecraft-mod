package com.swiftskyblock.mob;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Provides the curated list of summonable mobs and their durability costs for the Mob Summoning Staff.
 */
public class MobSummonRegistry {

    private static final Set<EntityType<?>> EXTRA_ALLOWED = Set.of(
            EntityType.IRON_GOLEM,
            EntityType.SNOW_GOLEM,
            EntityType.WITHER
    );

    private static final Set<EntityType<?>> BANNED = Set.of(
            EntityType.PLAYER,
            EntityType.ENDER_DRAGON,
            EntityType.GIANT
    );

    private static final Set<EntityType<?>> BOSS_MOBS = Set.of(
            EntityType.WITHER,
            EntityType.WARDEN,
            EntityType.ELDER_GUARDIAN,
            EntityType.RAVAGER
    );

    private static final Set<EntityType<?>> RARE_MOBS = Set.of(
            EntityType.VILLAGER,
            EntityType.WANDERING_TRADER,
            EntityType.BLAZE,
            EntityType.WITHER_SKELETON,
            EntityType.GHAST,
            EntityType.SHULKER,
            EntityType.ENDERMAN,
            EntityType.PHANTOM,
            EntityType.GUARDIAN,
            EntityType.EVOKER,
            EntityType.ALLAY,
            EntityType.SNIFFER,
            EntityType.MOOSHROOM,
            EntityType.AXOLOTL,
            EntityType.IRON_GOLEM
    );

    private static List<EntityType<?>> cachedMobs = null;

    public static boolean isAllowed(EntityType<?> type) {
        if (type == null || BANNED.contains(type)) {
            return false;
        }
        if (EXTRA_ALLOWED.contains(type)) {
            return true;
        }
        return SpawnEggItem.byId(type) != null;
    }

    public static List<EntityType<?>> getSummonableMobs() {
        if (cachedMobs == null) {
            List<EntityType<?>> list = new ArrayList<>();
            for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
                if (isAllowed(type)) {
                    list.add(type);
                }
            }
            list.sort(Comparator
                    .comparingInt(MobSummonRegistry::getCategoryOrder)
                    .thenComparingInt(MobSummonRegistry::getDurabilityCost)
                    .thenComparing(t -> String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(t))));
            cachedMobs = List.copyOf(list);
        }
        return cachedMobs;
    }

    public static int getCategoryOrder(EntityType<?> type) {
        if (BOSS_MOBS.contains(type)) return 3;
        MobCategory cat = type.getCategory();
        if (cat == MobCategory.CREATURE || cat == MobCategory.AMBIENT
                || cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT
                || cat == MobCategory.UNDERGROUND_WATER_CREATURE || cat == MobCategory.AXOLOTLS) {
            return 0; // Passive / Friendly
        }
        if (cat == MobCategory.MONSTER) {
            return 2; // Hostile
        }
        return 1; // Neutral / Utility (Villager, Golems, etc.)
    }

    public static int getDurabilityCost(EntityType<?> type) {
        if (BOSS_MOBS.contains(type)) return 20;
        if (RARE_MOBS.contains(type)) return 3;
        return 1;
    }

    public static boolean isBoss(EntityType<?> type) {
        return BOSS_MOBS.contains(type);
    }

    public static EntityType<?> byId(ResourceLocation id) {
        if (id == null) return null;
        return ForgeRegistries.ENTITY_TYPES.getValue(id);
    }
}
