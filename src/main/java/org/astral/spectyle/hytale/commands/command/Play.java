package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.astral.spectyle.hytale.commands.permissions.Permissions;
import org.jetbrains.annotations.NotNull;

public final class Play extends CommandBase {
    public Play(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        requirePermission(Permissions.PLAY);
    }

    @Override
    protected void executeSync(@NotNull CommandContext commandContext) {

    }
}
