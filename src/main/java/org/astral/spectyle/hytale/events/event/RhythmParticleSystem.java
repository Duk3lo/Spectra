package org.astral.spectyle.hytale.events.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.hytale.events.schedulers.world.AudioBarsParticles;
import org.jetbrains.annotations.NotNull;

public final class RhythmParticleSystem extends DelayedEntitySystem<EntityStore> {

    public RhythmParticleSystem() {
        super(0.0f);
    }

    @Override
    public void tick(float v, int i,
                     @NotNull ArchetypeChunk<EntityStore> archetypeChunk,
                     @NotNull Store<EntityStore> store,
                     @NotNull CommandBuffer<EntityStore> commandBuffer) {

        if (AudioAPI.isPaused() || !AudioAPI.isPlaying()) return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        Vector3d position = playerRef.getTransform().getPosition();
        Vector3f headRotation = playerRef.getHeadRotation();

        AudioBarsParticles.spawnBarsBehind(
                position,
                headRotation,
                AudioAPI.getAllBars(),
                store
        );
    }

    @Override
    public @NotNull Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}