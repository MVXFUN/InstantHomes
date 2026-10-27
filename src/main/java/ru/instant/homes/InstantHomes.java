package ru.instant.homes;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import ru.instant.homes.command.HomeCommands;

@Mod(InstantHomes.MOD_ID)
public final class InstantHomes {
    public static final String MOD_ID = "instanthomes";

    public InstantHomes() {
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        HomeCommands.register(event.getDispatcher());
    }
}
