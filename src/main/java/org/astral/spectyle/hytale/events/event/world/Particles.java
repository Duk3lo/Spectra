package org.astral.spectyle.hytale.events.event.world;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class Particles {

    private Particles() {
    }

    public static void spawnRhythm(String particleId,
                                   Vector3d position,
                                   ComponentAccessor<EntityStore> componentAccessor) {
        ParticleUtil.spawnParticleEffect(particleId, position, componentAccessor);
    }
}