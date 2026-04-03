package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.astral.spectyle.hytale.configuration.ConfigLoader;
import org.astral.spectyle.hytale.to_asset.AssetPackBuilder;
import org.astral.spectyle.hytale.to_asset.SoundEventJson;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ListSounds extends CommandBase {

    public ListSounds(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        try {
            AssetPackBuilder builder = ConfigLoader.getAssetPack();
            Path soundEventsRoot = builder.paths().soundEventDir();

            if (!Files.exists(soundEventsRoot)) {
                ctx.sendMessage(Message.raw("SoundEvents directory does not exist."));
                return;
            }

            List<SoundEntry> validSounds = new ArrayList<>();

            try (var walk = Files.walk(soundEventsRoot)) {
                walk.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .forEach(jsonPath -> {
                            String fileName = jsonPath.getFileName().toString();
                            String soundEventId = fileName.substring(0, fileName.length() - 5);

                            try {
                                int index = SoundEvent.getAssetMap().getIndex(soundEventId);
                                if (index < 0) {
                                    return;
                                }

                                SoundEventJson json = ConfigLoader.getSoundEventJson(soundEventId);
                                Path audioPath = Path.of(json.absolutePath());

                                if (!Files.exists(audioPath)) {
                                    return;
                                }

                                validSounds.add(new SoundEntry(
                                        soundEventId,
                                        index,
                                        audioPath.getFileName().toString()
                                ));

                            } catch (Exception ignored) {
                            }
                        });
            }

            validSounds.sort(Comparator.comparingInt(SoundEntry::index));

            if (validSounds.isEmpty()) {
                ctx.sendMessage(Message.raw("No valid sounds found."));
                return;
            }

            StringBuilder message = new StringBuilder("Valid sounds:\n");

            for (int i = 0; i < validSounds.size(); i++) {
                SoundEntry sound = validSounds.get(i);

                message.append(i + 1)
                        .append(". ")
                        .append(sound.soundEventId())
                        .append(" -> ")
                        .append(sound.fileName())
                        .append(" [index=")
                        .append(sound.index())
                        .append("]");

                if (i + 1 < validSounds.size()) {
                    message.append("\n");
                }
            }

            ctx.sendMessage(Message.raw(message.toString()));

        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Error while listing sounds: " + e.getMessage()));
        }
    }

    private record SoundEntry(String soundEventId, int index, String fileName) {}
}