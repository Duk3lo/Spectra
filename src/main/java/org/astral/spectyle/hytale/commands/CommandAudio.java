package org.astral.spectyle.hytale.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.astral.spectyle.hytale.commands.command.Play;
import org.astral.spectyle.hytale.commands.command.Reload;
import org.jetbrains.annotations.NotNull;

public final class CommandAudio extends AbstractCommandCollection {
    public CommandAudio(@NotNull String name, @NotNull String description) {
        super(name, description);
        this.addSubCommand(new Play("play", "Play Sound", false));
        this.addSubCommand(new Reload("reload", "Reload Api", false));
    }
}
