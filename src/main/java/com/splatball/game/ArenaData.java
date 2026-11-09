package com.splatball.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;

/** Remembers where the arena was built so it survives restarts. */
public class ArenaData extends SavedData {
    @Nullable
    public BlockPos origin;
    /** The station that created the arena (same dimension), or null when it was built by command. */
    @Nullable
    public BlockPos home;
    public ResourceKey<Level> dimension = Level.OVERWORLD;

    public static ArenaData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ArenaData::load, ArenaData::new, "splatball_arena");
    }

    public void set(ResourceKey<Level> dimension, BlockPos origin, @Nullable BlockPos home) {
        this.dimension = dimension;
        this.origin = origin.immutable();
        this.home = home == null ? null : home.immutable();
        setDirty();
    }

    private static ArenaData load(CompoundTag tag) {
        ArenaData data = new ArenaData();
        if (tag.contains("Origin")) {
            data.origin = NbtUtils.readBlockPos(tag.getCompound("Origin"));
            data.dimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(tag.getString("Dimension")));
            if (tag.contains("Home")) data.home = NbtUtils.readBlockPos(tag.getCompound("Home"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (origin != null) {
            tag.put("Origin", NbtUtils.writeBlockPos(origin));
            tag.putString("Dimension", dimension.location().toString());
            if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        }
        return tag;
    }
}
