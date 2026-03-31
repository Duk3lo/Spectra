package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.astral.spectyle.hytale.SpectylePlugin;
import org.astral.spectyle.hytale.commands.permissions.Permissions;
import org.jetbrains.annotations.NotNull;

public final class Reload extends CommandBase {
    public static SpectylePlugin instance = SpectylePlugin.getInstance();
    public Reload(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        requirePermission(Permissions.RELOAD_CONFIG);
    }


    @Override
    protected void executeSync(@NotNull CommandContext commandContext) {
        instance.reloadConfig();
        instance.getLogger().atInfo().log("Reloading Configurations!");
    }
}
