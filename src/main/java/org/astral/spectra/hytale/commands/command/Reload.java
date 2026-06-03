package org.astral.spectra.hytale.commands.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.astral.spectra.hytale.SpectraPlugin;
import org.astral.spectra.hytale.commands.permissions.Permissions;
import org.jetbrains.annotations.NotNull;

public final class Reload extends CommandBase {
    public static SpectraPlugin instance = SpectraPlugin.getInstance();
    public Reload(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        requirePermission(Permissions.RELOAD_CONFIG);
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        ctx.sendMessage(Message.raw("[Spectyle] Reloading configurations..."));
        instance.reloadConfig();
    }
}
