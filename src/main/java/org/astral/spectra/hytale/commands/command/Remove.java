package org.astral.spectra.hytale.commands.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.astral.spectra.hytale.commands.permissions.Permissions;
import org.astral.spectra.hytale.configuration.ConfigLoader;
import org.astral.spectra.hytale.to_asset.SoundEventJson;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Remove extends CommandBase {

    private final RequiredArg<String> soundName =
            withRequiredArg("Sound_Name", "Example: MySound", ArgTypes.STRING);

    public Remove(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        requirePermission(Permissions.REMOVE);
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        String name = soundName.get(ctx);

        if (name == null || name.isBlank()) {
            ctx.sendMessage(Message.raw("Sound name was not specified."));
            return;
        }

        try {
            String soundEventId = toPascal(name);

            SoundEventJson json = ConfigLoader.getSoundEventJson(soundEventId);
            Path jsonPath = ConfigLoader.getAssetPack().paths().soundEventFile(soundEventId);
            Path audioPath = Path.of(json.absolutePath());

            boolean removedJson = Files.deleteIfExists(jsonPath);
            boolean removedOgg = Files.deleteIfExists(audioPath);

            if (!removedJson && !removedOgg) {
                ctx.sendMessage(Message.raw("No files were removed. The sound may not exist."));
                return;
            }

            ctx.sendMessage(Message.raw("Sound removed successfully: " + soundEventId));

        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Error removing sound: " + e.getMessage()));
        }
    }

    private static @NotNull String toPascal(String name) {
        String cleaned = name == null ? "" : name.replaceAll("[^a-zA-Z0-9_\\-]+", "_");
        String[] parts = cleaned.split("[_\\-]+");

        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase());
            }
        }
        return sb.isEmpty() ? "Sound" : sb.toString();
    }
}