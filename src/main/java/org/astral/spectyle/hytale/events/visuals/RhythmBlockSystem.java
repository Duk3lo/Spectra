package org.astral.spectyle.hytale.events.visuals;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.DelayedEntitySystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.hytale.events.visuals.world.AudioBarsBlocks;
import org.astral.spectyle.hytale.events.visuals.world.AudioBarsParticles;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public final class RhythmBlockSystem extends DelayedEntitySystem<EntityStore> {



    public RhythmBlockSystem() {
        super(0.5f);
    }

    @Override
    public void tick(float v, int i,
                     @NotNull ArchetypeChunk<EntityStore> archetypeChunk,
                     @NotNull Store<EntityStore> store,
                     @NotNull CommandBuffer<EntityStore> commandBuffer) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Player player = store.getComponent(ref, Player.getComponentType());

        if (player == null) return;
        World world = player.getWorld();
        if (world == null) return;

        Collection<VisualizerManager.VisualizerData> visualizers = VisualizerManager.getAllGlobalData();
        if (visualizers.isEmpty()) return;

        if (!AudioAPI.isPlaying()) {
            for (VisualizerManager.VisualizerData data : visualizers) {
                if (data.getType().equals("blocks")) {
                    AudioBarsBlocks.stopAndReset(world, data);
                }
            }
            VisualizerManager.stopAllGlobally();
            return;
        }

        if (AudioAPI.isPaused()) {
            return;
        }

        for (VisualizerManager.VisualizerData data : visualizers) {
            if (data.getType().equals("blocks")) {
                AudioBarsBlocks.drawBlocksFront(
                        data,
                        AudioAPI.getAllBars(),
                        world
                );
            } else if (data.getType().equals("particles")) {
                AudioBarsParticles.spawnBarsBehind(
                        data,
                        AudioAPI.getAllBars(),
                        store
                );
            }
        }
    }

    @Override
    public @NotNull Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}