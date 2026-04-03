package org.astral.spectyle.hytale.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.astral.spectyle.hytale.commands.command.*;
import org.jetbrains.annotations.NotNull;

public final class SpectyleCommands extends AbstractCommandCollection {
    public SpectyleCommands(@NotNull String name, @NotNull String description) {
        super(name, description);
        addSubCommand(new ListSounds("list", "List all valid sounds", false));
        addSubCommand(new Import("import", "Import audio from Folder", false));
        addSubCommand(new Remove("remove", "Remove a sound", false));
        addSubCommand(new Play("play", "Play Sound", false));
        addSubCommand(new Reload("reload", "Reload Api", false));
        addSubCommand(new Adjust("adjust", "Sync the audio by shifting time (ms)", false));

    }
}
