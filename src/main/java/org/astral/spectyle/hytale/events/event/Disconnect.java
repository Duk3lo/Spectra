package org.astral.spectyle.hytale.events.event;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.hytale.events.visuals.VisualizerManager;
import org.astral.spectyle.hytale.events.visuals.world.AudioBarsBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class Disconnect {
    public static void register(@NotNull EventRegistry registry, HytaleLogger logger, AudioEngine engine){
        registry.registerGlobal(PlayerDisconnectEvent.class, event -> {
            World tempWorld = null;
            try {
                UUID uuid = event.getPlayerRef().getWorldUuid();
                if (uuid == null)return;
                tempWorld = Universe.get().getWorld(uuid);
            } catch (Exception ignored) {}

            final World world = tempWorld;

            HytaleServer.SCHEDULED_EXECUTOR.schedule(()->{
                int allPlayers = Universe.get().getPlayerCount();
                if (allPlayers < 1) {
                    if (AudioAPI.isPlaying()){
                        logger.atInfo().log("There are no players, the music is stopping...");
                        engine.stopSong();
                    }
                    if (world != null) {
                        for (VisualizerManager.VisualizerData data : VisualizerManager.getAllGlobalData()) {
                            if (data.getType().equals("blocks")) {
                                AudioBarsBlocks.stopAndReset(world, data);
                            }
                        }
                    }
                    VisualizerManager.stopAllGlobally();
                }
            }, 1L, TimeUnit.SECONDS);
        });
    }
}