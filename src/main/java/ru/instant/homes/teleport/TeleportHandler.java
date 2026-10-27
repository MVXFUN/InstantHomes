package ru.instant.homes.teleport;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import ru.instant.homes.storage.HomePoint;

public final class TeleportHandler {
    private TeleportHandler() {}

    public static boolean teleport(ServerPlayer player, HomePoint point) {
        ServerLevel destination = player.getServer().getLevel(point.dimension());
        if (destination == null) return false;
        player.teleportTo(destination, point.x(), point.y(), point.z(),
            java.util.Set.of(), point.yaw(), point.pitch());
        return true;
    }

    public static void teleportInCurrentDimension(ServerPlayer player, double x, double y, double z) {
        player.teleportTo((ServerLevel) player.level(), x, y, z,
            java.util.Set.of(), player.getYRot(), player.getXRot());
    }
}
