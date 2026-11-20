package com.swiftskyblock.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkyblockSavedData extends SavedData {
    private static final String DATA_NAME = "swiftskyblock_data";

    private boolean islandInitialized = false;
    @Nullable
    private BlockPos defaultHome = null;
    private int nextIslandIndex = 1;
    private final Map<UUID, BlockPos> playerHomes = new HashMap<>();

    public static SkyblockSavedData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(SkyblockSavedData::load, SkyblockSavedData::new, DATA_NAME);
    }

    public boolean isIslandInitialized() {
        return islandInitialized;
    }

    public void setIslandInitialized(boolean initialized) {
        this.islandInitialized = initialized;
        setDirty();
    }

    @Nullable
    public BlockPos getDefaultHome() {
        return defaultHome;
    }

    public void setDefaultHome(BlockPos pos) {
        this.defaultHome = pos.immutable();
        setDirty();
    }

    public BlockPos getHomeFor(UUID uuid) {
        BlockPos custom = playerHomes.get(uuid);
        if (custom != null) {
            return custom;
        }
        return defaultHome;
    }

    public void setHomeFor(UUID uuid, BlockPos pos) {
        playerHomes.put(uuid, pos.immutable());
        setDirty();
    }

    public BlockPos allocateNextIslandCenter() {
        int idx = nextIslandIndex++;
        setDirty();
        return new BlockPos(idx * 500, 64, 0);
    }

    public static SkyblockSavedData load(CompoundTag tag) {
        SkyblockSavedData data = new SkyblockSavedData();
        data.islandInitialized = tag.getBoolean("IslandInitialized");
        if (tag.contains("DefaultHome")) {
            data.defaultHome = BlockPos.of(tag.getLong("DefaultHome"));
        }
        data.nextIslandIndex = Math.max(1, tag.getInt("NextIslandIndex"));
        ListTag list = tag.getList("PlayerHomes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("UUID") && entry.contains("Pos")) {
                data.playerHomes.put(entry.getUUID("UUID"), BlockPos.of(entry.getLong("Pos")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("IslandInitialized", islandInitialized);
        if (defaultHome != null) {
            tag.putLong("DefaultHome", defaultHome.asLong());
        }
        tag.putInt("NextIslandIndex", nextIslandIndex);
        ListTag list = new ListTag();
        for (Map.Entry<UUID, BlockPos> e : playerHomes.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", e.getKey());
            entry.putLong("Pos", e.getValue().asLong());
            list.add(entry);
        }
        tag.put("PlayerHomes", list);
        return tag;
    }
}
