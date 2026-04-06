package org.astral.spectyle.hytale.events.schedulers;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.world.World;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.hytale.events.schedulers.world.AudioBarsBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class RhythmBlocksScheduler {

    public static void executeBars(@NotNull String id,
                            @NotNull World world,
                            @NotNull Vector3d position,
                            @NotNull Vector3f headRotation) {

        world.execute(() -> {
            ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
                if (AudioAPI.isPaused() || !AudioAPI.isPlaying()) {
                    AudioBarsBlocks.stopAndReset(world);
                    Register.removeScheduler(id);
                    return;
                }

                AudioBarsBlocks.drawBlocksFront(
                        position,
                        headRotation,
                        AudioAPI.getAllBars(),
                        world
                );
            }, 0L, 100L, TimeUnit.MILLISECONDS);

            Register.register(id, future);
        });
    }

    public static void stop(@NotNull String id) {
        Register.removeScheduler(id);
    }
}