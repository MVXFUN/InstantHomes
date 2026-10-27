package ru.instant.homes.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Stored in the world's overworld SavedData so homes can point into any dimension. */
public final class HomeStorage extends SavedData {
    private static final String DATA_NAME = "instanthomes_data";
    private final Map<String, HomePoint> homes = new LinkedHashMap<>();

    public static HomeStorage get(ServerLevel anyLevelInWorld) {
        ServerLevel overworld = anyLevelInWorld.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(HomeStorage::new, HomeStorage::load), DATA_NAME);
    }

    public Optional<HomePoint> getHome(String name) {
        return Optional.ofNullable(homes.get(normalize(name)));
    }

    public void putHome(String name, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        String key = normalize(name);
        homes.put(key, new HomePoint(key, x, y, z, level.dimension(), yaw, pitch));
        setDirty();
    }

    public boolean deleteHome(String name) {
        boolean removed = homes.remove(normalize(name)) != null;
        if (removed) setDirty();
        return removed;
    }

    public java.util.List<String> names() {
        return homes.keySet().stream().filter(n -> !n.equals("main")).toList();
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static HomeStorage load(CompoundTag tag, HolderLookup.Provider registries) {
        HomeStorage storage = new HomeStorage();
        ListTag list = tag.getList("homes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag h = list.getCompound(i);
            String name = h.getString("name");
            ResourceLocation id = ResourceLocation.tryParse(h.getString("dimension"));
            if (name.isBlank() || id == null) continue;
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, id);
            storage.homes.put(normalize(name), new HomePoint(normalize(name), h.getDouble("x"),
                h.getDouble("y"), h.getDouble("z"), dimension, h.getFloat("yaw"), h.getFloat("pitch")));
        }
        return storage;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (HomePoint point : homes.values()) {
            CompoundTag h = new CompoundTag();
            h.putString("name", point.name());
            h.putDouble("x", point.x()); h.putDouble("y", point.y()); h.putDouble("z", point.z());
            h.putString("dimension", point.dimensionId().toString());
            h.putFloat("yaw", point.yaw()); h.putFloat("pitch", point.pitch());
            list.add(h);
        }
        tag.put("homes", list);
        return tag;
    }
}
