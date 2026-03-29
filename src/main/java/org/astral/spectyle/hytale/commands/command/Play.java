package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class Play extends AbstractCommand {
    public Play(@Nullable String name, @Nullable String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
    }

    @Override
    protected @Nullable CompletableFuture<Void> execute(@NotNull CommandContext commandContext) {
        return null;
    }
}
