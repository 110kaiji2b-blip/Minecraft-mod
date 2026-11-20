package com.swiftskyblock.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfig {
    public static final ForgeConfigSpec COMMON_SPEC;

    // Twerk Grow
    public static final ForgeConfigSpec.BooleanValue TWERK_GROW_ENABLED;
    public static final ForgeConfigSpec.IntValue TWERK_GROW_RADIUS;
    public static final ForgeConfigSpec.IntValue TWERK_GROW_STRENGTH;
    public static final ForgeConfigSpec.BooleanValue SPRINT_GROW_ENABLED;

    // Vein Miner
    public static final ForgeConfigSpec.BooleanValue VEIN_MINER_ENABLED;
    public static final ForgeConfigSpec.IntValue VEIN_MINER_MAX_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue VEIN_MINER_AUTO_LEAVES;

    // Void Safety
    public static final ForgeConfigSpec.BooleanValue VOID_SAFETY_ENABLED;
    public static final ForgeConfigSpec.IntValue VOID_SAFETY_Y;
    public static final ForgeConfigSpec.BooleanValue VOID_SAFETY_DAMAGE;

    // Sky Magnet
    public static final ForgeConfigSpec.DoubleValue MAGNET_RANGE;

    // Machines & Speedups
    public static final ForgeConfigSpec.IntValue TURBO_FURNACE_SPEED;
    public static final ForgeConfigSpec.IntValue ACCEL_LANTERN_RADIUS;
    public static final ForgeConfigSpec.IntValue ACCEL_LANTERN_EXTRA_TICKS;
    public static final ForgeConfigSpec.IntValue POCKET_WATCH_TICKS;
    public static final ForgeConfigSpec.IntValue SIEVE_BATCH_SIZE;
    public static final ForgeConfigSpec.IntValue SIEVE_AUTO_INTERVAL_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Swift Skyblock Configuration / スイフト・スカイブロック設定").push("general");

        builder.push("twerk_grow");
        TWERK_GROW_ENABLED = builder
                .comment("Enable rapid plant & sapling growth by sneaking repeatedly.")
                .define("enabled", true);
        TWERK_GROW_RADIUS = builder
                .comment("Horizontal radius around the player for sneak growth.")
                .defineInRange("radius", 5, 1, 12);
        TWERK_GROW_STRENGTH = builder
                .comment("Number of growth attempts applied per sneak toggle.")
                .defineInRange("strength", 3, 1, 10);
        SPRINT_GROW_ENABLED = builder
                .comment("Also gently accelerate nearby crops & saplings while sprinting.")
                .define("sprintGrowEnabled", true);
        builder.pop();

        builder.push("vein_miner");
        VEIN_MINER_ENABLED = builder
                .comment("Enable sneak-to-vein-mine for logs, ores, leaves, and hammerable blocks.")
                .define("enabled", true);
        VEIN_MINER_MAX_BLOCKS = builder
                .comment("Maximum number of blocks broken in one vein-mine action.")
                .defineInRange("maxBlocks", 64, 4, 256);
        VEIN_MINER_AUTO_LEAVES = builder
                .comment("Automatically clear nearby connected leaves when vein-mining a tree log.")
                .define("autoClearLeaves", true);
        builder.pop();

        builder.push("void_safety");
        VOID_SAFETY_ENABLED = builder
                .comment("Teleport players safely back to their island spawn if they fall into the void.")
                .define("enabled", true);
        VOID_SAFETY_Y = builder
                .comment("Y coordinate threshold below which Void Safety triggers.")
                .defineInRange("triggerY", -64, -128, 0);
        VOID_SAFETY_DAMAGE = builder
                .comment("Whether falling into the void deals small non-lethal damage (2 hearts) on rescue.")
                .define("dealNonLethalDamage", false);
        builder.pop();

        builder.push("magnet");
        MAGNET_RANGE = builder
                .comment("Pickup radius of the Sky Magnet item.")
                .defineInRange("range", 12.0, 2.0, 32.0);
        builder.pop();

        builder.push("machines");
        TURBO_FURNACE_SPEED = builder
                .comment("Speed multiplier of the Turbo Furnace compared to a vanilla furnace.")
                .defineInRange("turboFurnaceSpeed", 8, 2, 32);
        ACCEL_LANTERN_RADIUS = builder
                .comment("Block radius affected by the Acceleration Lantern.")
                .defineInRange("accelerationLanternRadius", 4, 1, 8);
        ACCEL_LANTERN_EXTRA_TICKS = builder
                .comment("Extra ticks applied per game tick by the Acceleration Lantern (3 = 4x total speed).")
                .defineInRange("accelerationLanternExtraTicks", 3, 1, 15);
        POCKET_WATCH_TICKS = builder
                .comment("Number of ticks advanced instantly when using the Time Pocket Watch.")
                .defineInRange("pocketWatchTicks", 100, 10, 1000);
        SIEVE_BATCH_SIZE = builder
                .comment("Number of blocks processed on a normal right-click on the Rapid Sieve (sneak right-click processes whole stack).")
                .defineInRange("sieveBatchSize", 8, 1, 64);
        SIEVE_AUTO_INTERVAL_TICKS = builder
                .comment("Tick interval between automatic sieve operations when fed by hopper.")
                .defineInRange("sieveAutoIntervalTicks", 5, 1, 40);
        builder.pop();

        builder.pop();
        COMMON_SPEC = builder.build();
    }
}
