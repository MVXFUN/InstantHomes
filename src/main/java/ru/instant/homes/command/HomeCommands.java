package ru.instant.homes.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import ru.instant.homes.storage.HomePoint;
import ru.instant.homes.storage.HomeStorage;
import ru.instant.homes.teleport.TeleportHandler;

public final class HomeCommands {
    private HomeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // This branch may merge with vanilla /tp; vanilla's permission gate must be checked in-game.
        dispatcher.register(Commands.literal("tp")
            .requires(s -> s.getEntity() instanceof ServerPlayer)
            .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            double x = DoubleArgumentType.getDouble(ctx, "x");
                            double y = DoubleArgumentType.getDouble(ctx, "y");
                            double z = DoubleArgumentType.getDouble(ctx, "z");
                            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                                ctx.getSource().sendFailure(Component.literal("Укажи корректные координаты."));
                                return 0;
                            }
                            TeleportHandler.teleportInCurrentDimension(player, x, y, z);
                            ctx.getSource().sendSuccess(() -> Component.literal("Телепортация выполнена."), false);
                            return 1;
                        })))));

        dispatcher.register(Commands.literal("sethome")
            .requires(s -> s.getEntity() instanceof ServerPlayer)
            .executes(ctx -> setHome(ctx.getSource(), "main"))
            .then(Commands.argument("название", StringArgumentType.word())
                .executes(ctx -> setHome(ctx.getSource(), StringArgumentType.getString(ctx, "название")))));

        dispatcher.register(Commands.literal("home")
            .requires(s -> s.getEntity() instanceof ServerPlayer)
            .executes(ctx -> goHome(ctx.getSource(), "main"))
            .then(Commands.argument("название", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    HomeStorage.get(ctx.getSource().getLevel()).names(), builder))
                .executes(ctx -> goHome(ctx.getSource(), StringArgumentType.getString(ctx, "название")))));

        dispatcher.register(Commands.literal("delhome")
            .requires(s -> s.getEntity() instanceof ServerPlayer)
            .then(Commands.argument("название", StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                    HomeStorage.get(ctx.getSource().getLevel()).names(), builder))
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, "название");
                    if (name.equalsIgnoreCase("main")) {
                        ctx.getSource().sendFailure(Component.literal("Основной дом нельзя удалить командой /delhome."));
                        return 0;
                    }
                    if (!HomeStorage.get(ctx.getSource().getLevel()).deleteHome(name)) {
                        ctx.getSource().sendFailure(Component.literal("Дом не найден."));
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() -> Component.literal("Дом удалён."), false);
                    return 1;
                })));
    }

    private static int setHome(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HomeStorage.get(player.serverLevel()).putHome(name, player.serverLevel(),
            player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal("Дом установлен: " + name), false);
        return 1;
    }

    private static int goHome(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HomePoint point = HomeStorage.get(player.serverLevel()).getHome(name).orElse(null);
        if (point == null) {
            source.sendFailure(Component.literal(name.equalsIgnoreCase("main")
                ? "Сначала установи дом командой /sethome."
                : "Дом не найден."));
            return 0;
        }
        if (!TeleportHandler.teleport(player, point)) {
            source.sendFailure(Component.literal("Не удалось найти измерение дома."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Телепортация выполнена."), false);
        return 1;
    }
}
