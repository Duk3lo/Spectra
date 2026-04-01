package org.astral.spectyle.hytale.events.event;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class Disconnect {
    public static void register(@NotNull EventRegistry registry, HytaleLogger logger, AudioEngine engine){
        registry.registerGlobal(PlayerDisconnectEvent.class, event -> {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(()->{
                int allPlayers = Universe.get().getPlayerCount();
                if (allPlayers < 1) {
                    if (AudioAPI.isPlaying()){
                        logger.atInfo().log("There are no players, the music is stopping...");
                        engine.stopSong();
                    }
                }
            }, 1L, TimeUnit.SECONDS);
        });
    }
}
