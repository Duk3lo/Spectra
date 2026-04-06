package org.astral.spectyle.hytale.events.event;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.hytale.events.schedulers.world.AudioBarsBlocks;
import org.jetbrains.annotations.NotNull;

public final class RhythmBlockSystem extends DelayedEntitySystem<EntityStore> {

    public RhythmBlockSystem() {
        super(0.0f);
    }

    @Override
    public void tick(float v, int i,
                     @NotNull ArchetypeChunk<EntityStore> archetypeChunk,
                     @NotNull Store<EntityStore> store,
                     @NotNull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) return;
        World world = player.getWorld();
        if (world == null) return;

        if (AudioAPI.isPaused() || !AudioAPI.isPlaying()) {
            AudioBarsBlocks.stopAndReset(world);
            return;
        }

        Vector3d position = playerRef.getTransform().getPosition();
        Vector3f headRotation = playerRef.getHeadRotation();

        AudioBarsBlocks.drawBlocksFront(
                position,
                headRotation,
                AudioAPI.getAllBars(),
                world
        );
    }

    @Override
    public @NotNull Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}