package org.astral.spectyle.hytale.events.event;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.hytale.commands.command.Play;
import org.astral.spectyle.hytale.events.visuals.VisualizerManager;
import org.astral.spectyle.hytale.events.visuals.world.AudioBarsBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class Disconnect {
    public static void register(@NotNull EventRegistry registry, HytaleLogger logger, AudioEngine engine){
        registry.registerGlobal(PlayerDisconnectEvent.class, event -> {
            UUID disconnectedPlayerUuid = event.getPlayerRef().getUuid();
            Play.getPlayersListen().remove(disconnectedPlayerUuid);
            if (Play.getPlayersListen().isEmpty()) {
                if (AudioAPI.isPlaying()){
                    logger.atInfo().log("The last listener disconnected, the music is stopping...");
                    engine.stopSong();
                }
                try {
                    UUID worldUuid = event.getPlayerRef().getWorldUuid();
                    if (worldUuid != null) {
                        World world = Universe.get().getWorld(worldUuid);
                        if (world != null) {
                            for (VisualizerManager.VisualizerData data : VisualizerManager.getAllGlobalData()) {
                                if (data.getType().equals("blocks")) {
                                    AudioBarsBlocks.stopAndReset(world, data);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
                VisualizerManager.stopAllGlobally();
            }
        });
    }
}