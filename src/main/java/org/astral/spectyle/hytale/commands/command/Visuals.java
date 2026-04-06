package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.jetbrains.annotations.NotNull;

public final class Visuals extends CommandBase {
    private final RequiredArg<String> animation = withRequiredArg("name", "Example: Blocks, Particles", ArgTypes.STRING);


    public Visuals(@NotNull String name, @NotNull String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
    }


    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        String name = animation.get(ctx);
        if (name == null || name.isBlank()){
            ctx.sendMessage(Message.raw("Not Specified"));
            return;
        }

        if (name.equalsIgnoreCase("BLocks")){

        }
    }
}
