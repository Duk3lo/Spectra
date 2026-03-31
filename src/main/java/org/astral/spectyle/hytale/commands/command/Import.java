package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.astral.spectyle.hytale.commands.permissions.Permissions;
import org.jetbrains.annotations.NotNull;

public final class Import extends CommandBase {
    private final RequiredArg<String> fileOgg = withRequiredArg("File.ogg", "Example: Name.ogg in folder plugin", ArgTypes.STRING);
    private final RequiredArg<String> nameOgg = withRequiredArg("Name_Song", "Example: File.ogg My_sound", ArgTypes.STRING);

    public Import(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        requirePermission(Permissions.IMPORT);
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        String ogg = fileOgg.get(ctx);
        String name = nameOgg.get(ctx);
        if (ogg.toLowerCase().endsWith("ogg")){

            return;
        }
    }
}
