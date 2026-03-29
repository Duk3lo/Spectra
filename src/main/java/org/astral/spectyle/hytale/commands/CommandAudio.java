package org.astral.spectyle.hytale.commands;

import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import org.astral.spectyle.hytale.commands.command.Play;
import org.astral.spectyle.hytale.commands.command.Reload;
import org.jetbrains.annotations.NotNull;

public class CommandAudio {
    public static void registerAll(@NotNull CommandRegistry registry){
        registry.registerCommand(new Play("play", "Play Sound", false));
        registry.registerCommand(new Reload("reload", "Reload Api", false));
    }
}
