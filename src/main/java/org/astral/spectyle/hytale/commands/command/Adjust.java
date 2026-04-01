package org.astral.spectyle.hytale.commands.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.hytale.SpectylePlugin;
import org.astral.spectyle.hytale.commands.permissions.Permissions;
import org.jetbrains.annotations.NotNull;

public final class Adjust extends CommandBase {
    private final RequiredArg<Integer> offsetArg = withRequiredArg("ms", "Milliseconds to offset (e.g., 500 or -500)", ArgTypes.INTEGER);
    private final AudioEngine engine = SpectylePlugin.getInstance().getAudioEngine();

    public Adjust(String name, String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        requirePermission(Permissions.PLAY);
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!AudioAPI.isPlaying()) {
            ctx.sendMessage(Message.raw("The audio engine is not currently playing."));
            return;
        }

        int offsetMs = offsetArg.get(ctx);
        long currentTimeMs = (long) (AudioAPI.getCurrentTime() * 1000);
        long targetTimeMs = currentTimeMs + offsetMs;
        engine.seekTo(targetTimeMs);
        String action = offsetMs >= 0 ? "Fast-forwarded" : "Rewound";
        ctx.sendMessage(Message.raw(String.format("[Spectyle] %s §f%dms. Current Time: %s",
                action, Math.abs(offsetMs), AudioAPI.getCurrentTimeFormatted())));
    }
}