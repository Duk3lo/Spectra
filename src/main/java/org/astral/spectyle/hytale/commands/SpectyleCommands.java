package org.astral.spectyle.hytale.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.astral.spectyle.hytale.commands.command.Adjust;
import org.astral.spectyle.hytale.commands.command.Import;
import org.astral.spectyle.hytale.commands.command.Play;
import org.astral.spectyle.hytale.commands.command.Reload;
import org.jetbrains.annotations.NotNull;

public final class SpectyleCommands extends AbstractCommandCollection {
    public SpectyleCommands(@NotNull String name, @NotNull String description) {
        super(name, description);
        addSubCommand(new Import("import", "import audio from Folder", false));
        addSubCommand(new Play("play", "Play Sound", false));
        addSubCommand(new Reload("reload", "Reload Api", false));
        addSubCommand(new Adjust("adjust", "Sync the audio by shifting time (ms)", false));
    }
}
