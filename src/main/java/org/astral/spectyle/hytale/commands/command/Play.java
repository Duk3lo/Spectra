package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.hytale.SpectylePlugin;
import org.astral.spectyle.hytale.commands.permissions.Permissions;
import org.astral.spectyle.hytale.configuration.ConfigLoader;
import org.astral.spectyle.hytale.to_asset.SoundEventJson;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Play extends CommandBase {

    private final RequiredArg<String> sound = withRequiredArg("sound", "play sound Example: play Sound_Name", ArgTypes.STRING);
    private final SpectylePlugin plugin = SpectylePlugin.getInstance();
    private final AudioEngine engine = plugin.getAudioEngine();

    public Play(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        requirePermission(Permissions.PLAY);
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        int players = Universe.get().getPlayerCount();
        if (players <= 0) {
            ctx.sendMessage(Message.raw("There are no players in the world to reproduce"));
            return;
        }
        if (AudioAPI.isPlaying()){
            ctx.sendMessage(Message.raw("The engine is already Playing"));
            return;
        }
        String name = sound.get(ctx);
        if (name == null || name.isBlank()) {
            ctx.sendMessage(Message.raw("Not specified..."));
            return;
        }
        int index = SoundEvent.getAssetMap().getIndex(name);
        if (index < 0) {
            ctx.sendMessage(Message.raw("Sound " + name + " not Found in server"));
            return;
        }
        try {
            SoundEventJson json = ConfigLoader.getSoundEventJson(name);
            Path audioPath = Path.of(json.absolutePath());
            ctx.sendMessage(Message.raw(audioPath.toString()));
            if (!Files.exists(audioPath)) {
                ctx.sendMessage(Message.raw("Audio file not found: " + audioPath));
                return;
            }
            engine.playSong(audioPath);
            for (PlayerRef playerRef : Universe.get().getPlayers()){
                SoundUtil.playSoundEvent2dToPlayer(playerRef, index, SoundCategory.SFX);
            }
        } catch (Exception e) {
            ctx.sendMessage(Message.raw("Error reading sound JSON: " + e.getMessage()));
        }
    }

}
