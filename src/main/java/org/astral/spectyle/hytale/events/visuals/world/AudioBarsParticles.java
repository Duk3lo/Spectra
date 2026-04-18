package org.astral.spectyle.hytale.events.visuals.world;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.astral.spectyle.hytale.events.visuals.VisualizerManager.VisualizerData;
import org.jetbrains.annotations.NotNull;

public final class AudioBarsParticles {

    private static final String BAR_HIGH = "BeamEmiter_Heal_Green";
    private static final String BAR_LOW = "BeamEmiter_Heal_Red";

    private static final double BACK_DISTANCE = 0.0;
    private static final double BASE_Y_OFFSET = 0.35;

    private AudioBarsParticles() {}

    public static void spawnBarsBehind(@NotNull VisualizerData data,
                                       float[] bars,
                                       ComponentAccessor<EntityStore> accessor) {

        if (bars == null || bars.length == 0) return;

        Adjust.Direction dir = Adjust.calculateDirections(data.getRot().getY());

        double centerX = data.getPos().getX() - (dir.forwardX() * BACK_DISTANCE);
        double centerZ = data.getPos().getZ() - (dir.forwardZ() * BACK_DISTANCE);
        double baseY = data.getPos().getY() + BASE_Y_OFFSET;

        double totalWidth = (bars.length - 1) * data.getSpacing();
        double startOffset = -totalWidth / 2.0;

        for (int i = 0; i < bars.length; i++) {
            float value = clamp01(bars[i]);

            if (value <= 0.02f) continue;

            double currentOffset = startOffset + (i * data.getSpacing());

            double barX = centerX + (dir.leftX() * currentOffset);
            double barZ = centerZ + (dir.leftZ() * currentOffset);

            String particle = value > 0.45f ? BAR_HIGH : BAR_LOW;

            ParticleUtil.spawnParticleEffect(
                    BAR_LOW,
                    new Vector3d(barX, baseY, barZ),
                    accessor
            );

            ParticleUtil.spawnParticleEffect(
                    particle,
                    new Vector3d(barX, baseY + (value * (data.getMaxHeight() * 0.15)), barZ),
                    accessor
            );
        }
    }

    private static float clamp01(float v) {
        return Math.clamp(v, 0f, 1f);
    }
}