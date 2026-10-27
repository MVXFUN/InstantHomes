package ru.instant.homes.storage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record HomePoint(String name, double x, double y, double z,
                        ResourceKey<Level> dimension, float yaw, float pitch) {
    public ResourceLocation dimensionId() {
        return dimension.location();
    }
}
