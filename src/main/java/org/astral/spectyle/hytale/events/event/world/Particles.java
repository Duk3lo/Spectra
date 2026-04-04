package org.astral.spectyle.hytale.events.event.world;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public final class Particles {

    private static final String KICK_PARTICLE = "Battleaxe_Signature_Whirlwind";
    private static final String SNARE_PARTICLE = "Fire_Pit";
    private static final String HAT_PARTICLE = "Fireflies_GS";

    private Particles() {
    }

    public static void spawnKickShow(@NotNull Vector3d center,
                                     ComponentAccessor<EntityStore> accessor) {
        ParticleUtil.spawnParticleEffect(
                KICK_PARTICLE,
                new Vector3d(
                        center.getX(),
                        center.getY() + 0.2,
                        center.getZ()
                ),
                accessor
        );
    }

    public static void spawnSnareShow(@NotNull Vector3d center,
                                      ComponentAccessor<EntityStore> accessor) {
        ParticleUtil.spawnParticleEffect(
                SNARE_PARTICLE,
                new Vector3d(
                        center.getX() + random(-0.2, 0.2),
                        center.getY() + 0.4,
                        center.getZ() + random(-0.2, 0.2)
                ),
                accessor
        );
    }

    public static void spawnHatShow(@NotNull Vector3d center,
                                    ComponentAccessor<EntityStore> accessor) {
        ParticleUtil.spawnParticleEffect(
                HAT_PARTICLE,
                new Vector3d(
                        center.getX() + random(-0.1, 0.1),
                        center.getY() + 1.0,
                        center.getZ() + random(-0.1, 0.1)
                ),
                accessor
        );
    }

    private static double random(double min, double max) {
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
}