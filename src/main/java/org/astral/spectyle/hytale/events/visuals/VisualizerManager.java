package org.astral.spectyle.hytale.events.visuals;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import org.astral.spectyle.hytale.configuration.VisualsConfig.VisualPreset;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VisualizerManager {

    private static final Map<String, VisualizerData> activeVisualizers = new ConcurrentHashMap<>();

    private VisualizerManager() {}

    public static @NotNull String start(String requestedName, VisualPreset preset, Vector3d pos, Vector3f rot) {
        String finalName = requestedName;

        if (finalName == null || finalName.isBlank()) {
            int count = 1;
            String baseName = preset.isBlocks() ? "visualBlock_" : "visualParticle_";
            while (activeVisualizers.containsKey(baseName + count)) {
                count++;
            }
            finalName = baseName + count;
        }

        activeVisualizers.put(finalName, new VisualizerData(finalName, preset, pos, rot));
        return finalName;
    }

    public static void stop(String name) {
        activeVisualizers.remove(name);
    }

    public static void stopAllGlobally() {
        activeVisualizers.clear();
    }

    public static @NotNull Collection<VisualizerData> getAllGlobalData() {
        return activeVisualizers.values();
    }

    public static class VisualizerData {
        private final String name;
        private final VisualPreset preset;
        private final Vector3d pos;
        private final Vector3f rot;
        private final Set<Vector3i> activeBlocks = ConcurrentHashMap.newKeySet();

        public VisualizerData(String name, VisualPreset preset, Vector3d pos, Vector3f rot) {
            this.name = name;
            this.preset = preset;
            this.pos = pos;
            this.rot = rot;
        }

        public String getName() {
            return name;
        }

        public VisualPreset getPreset() {
            return preset;
        }

        public Vector3d getPos() {
            return pos;
        }

        public Vector3f getRot() {
            return rot;
        }

        public Set<Vector3i> getActiveBlocks() {
            return activeBlocks;
        }
    }
}