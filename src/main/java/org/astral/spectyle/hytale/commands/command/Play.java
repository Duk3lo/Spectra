package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.hytale.SpectylePlugin;
import org.astral.spectyle.hytale.commands.permissions.Permissions;
import org.astral.spectyle.hytale.configuration.ConfigLoader;
import org.astral.spectyle.hytale.to_asset.SoundEventJson;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public final class Play extends AbstractPlayerCommand {

    private final RequiredArg<String> sound = withRequiredArg("sound", "play sound Example: play Sound_Name", ArgTypes.STRING);
    private final SpectylePlugin plugin = SpectylePlugin.getInstance();
    private final AudioEngine engine = plugin.getAudioEngine();
    private final AudioConfig config = plugin.getAudioConfig();

    public Play(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        requirePermission(Permissions.PLAY);
    }

    @Override
    protected void execute(@NotNull CommandContext commandContext, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        String name = sound.get(commandContext);
        if (name == null || name.isBlank()) {
            commandContext.sendMessage(Message.raw("Not specified..."));
            return;
        }
        int index = SoundEvent.getAssetMap().getIndex(name);
        if (index < 0) {
            commandContext.sendMessage(Message.raw("Sound " + name + " not Found in server"));
            return;
        }
        try {
            SoundEventJson json = ConfigLoader.getSoundEventJson(name);
            Path audioPath = Path.of(json.absolutePath());
            commandContext.sendMessage(Message.raw(audioPath.toString()));
            if (!Files.exists(audioPath)) {
                commandContext.sendMessage(Message.raw("Audio file not found: " + audioPath));
                return;
            }

            engine.playSong(audioPath, ()-> {
                for (PlayerRef player : Universe.get().getPlayers()) {
                    SoundUtil.playSoundEvent2dToPlayer(player, index, SoundCategory.SFX);
                }
            }, config.getGeneral().getDelayedTaskInMs(), TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            commandContext.sendMessage(Message.raw("Error reading sound JSON: " + e.getMessage()));
        }
    }
}
