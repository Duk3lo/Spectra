package org.astral.spectyle.hytale.events.visuals;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VisualizerManager {

    private static final Map<String, VisualizerData> activeVisualizers = new ConcurrentHashMap<>();

    private VisualizerManager() {}

    public static @NotNull String start(String requestedName, String type, int maxHeight, double spacing, Vector3d pos, Vector3f rot) {
        String finalName = requestedName;

        if (finalName == null || finalName.trim().isEmpty()) {
            int count = 1;
            String baseName = type.equals("blocks") ? "visualBlock " : "visualParticle ";
            while (activeVisualizers.containsKey(baseName + count)) {
                count++;
            }
            finalName = baseName + count;
        }

        activeVisualizers.put(finalName, new VisualizerData(finalName, type, maxHeight, spacing, pos, rot));
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
        private final String type;
        private final int maxHeight;
        private final double spacing;
        private final Vector3d pos;
        private final Vector3f rot;
        private final Set<Vector3i> activeBlocks = ConcurrentHashMap.newKeySet();

        public VisualizerData(String name, String type, int maxHeight, double spacing, Vector3d pos, Vector3f rot) {
            this.name = name;
            this.type = type;
            this.maxHeight = maxHeight;
            this.spacing = spacing;
            this.pos = pos;
            this.rot = rot;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public int getMaxHeight() { return maxHeight; }
        public double getSpacing() { return spacing; }
        public Vector3d getPos() { return pos; }
        public Vector3f getRot() { return rot; }
        public Set<Vector3i> getActiveBlocks() { return activeBlocks; }
    }
}