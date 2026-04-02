package org.astral.spectyle.hytale.events.event.world;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public final class Particles {

    private static final String[] RHYTHM_PARTICLES = {
            "Block_Break_Sand",
            "Block_Break_Stone",
            "Block_Break_Grass",
            "sparkle",
            "smoke",
            "dust"
    };

    private Particles() {
    }

    public static void spawnRhythmRandom(@NotNull Vector3d position,
                                         ComponentAccessor<EntityStore> componentAccessor) {
        String particleId = RHYTHM_PARTICLES[
                ThreadLocalRandom.current().nextInt(RHYTHM_PARTICLES.length)
                ];

        Vector3d randomPos = new Vector3d(
                position.getX() + ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                position.getY() + ThreadLocalRandom.current().nextDouble(0.0, 1.0),
                position.getZ() + ThreadLocalRandom.current().nextDouble(-0.5, 0.5)
        );

        ParticleUtil.spawnParticleEffect(particleId, randomPos, componentAccessor);
    }
}