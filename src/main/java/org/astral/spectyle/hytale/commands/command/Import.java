package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.astral.spectyle.hytale.commands.permissions.Permissions;
import org.astral.spectyle.hytale.configuration.ConfigLoader;
import org.astral.spectyle.hytale.to_asset.AssetPackBuilder;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Import extends CommandBase {
    private final RequiredArg<String> fileOgg = withRequiredArg("File.ogg", "Example: Name.ogg in plugin folder", ArgTypes.STRING);
    private final RequiredArg<String> nameOgg = withRequiredArg("Sound_Name", "Example: File.ogg My_sound", ArgTypes.STRING);

    public Import(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        requirePermission(Permissions.IMPORT);
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        String ogg = fileOgg.get(ctx);
        String name = nameOgg.get(ctx);

        if (!ogg.toLowerCase().endsWith(".ogg")) {
            ctx.sendMessage(Message.raw("The file must end with .ogg"));
            return;
        }

        Path source = ConfigLoader.getSoundPath().resolve(ogg).normalize();
        if (!Files.exists(source)) {
            ctx.sendMessage(Message.raw("File not found: " + source));
            return;
        }
        try {
            AssetPackBuilder builder = ConfigLoader.getAssetPack();
            AssetPackBuilder.BuiltCustomSound built = builder.buildCustomSound(source, name, ogg);
            ctx.sendMessage(Message.raw("Sound imported successfully: " + built.soundEventId() + ". Reload the server to use the sound."));
        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Error importing sound: " + e.getMessage()));
        }
    }
}