package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.astral.spectyle.hytale.MainPlugin;
import org.astral.spectyle.hytale.commands.permissions.Permissions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.CompletableFuture;

public class Reload extends AbstractCommand {
    public static MainPlugin instance = MainPlugin.getInstance();
    public Reload(@Nullable String name, @Nullable String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        requirePermission(Permissions.RELOAD_CONFIG);
    }

    @Override
    protected @Nullable CompletableFuture<Void> execute(@NotNull CommandContext commandContext) {
        instance.reloadConfig();
        instance.getLogger().atInfo().log("Reloading Configurations!");
        return CompletableFuture.completedFuture(null);
    }
}
