package org.astral.spectra.hytale.events.visuals.world;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.astral.spectra.hytale.configuration.VisualsConfig.VisualPreset;
import org.astral.spectra.hytale.events.visuals.VisualizerManager.VisualizerData;
import org.jetbrains.annotations.NotNull;

public final class AudioBarsParticles {

    private static final double BASE_Y_OFFSET = 0.35;

    private AudioBarsParticles() {}

    public static void spawnBarsBehind(@NotNull VisualizerData data,
                                       float @NotNull [] bars,
                                       @NotNull ComponentAccessor<EntityStore> accessor) {

        if (bars.length == 0) return;

        VisualPreset preset = data.getPreset();
        if (preset == null) return;

        if (preset.isPulseRing()) {
            spawnPulseRing(data, bars, accessor);
        } else if (preset.isCircle() || preset.isRing()) {
            spawnCircle(data, bars, accessor);
        } else if (preset.isWave()) {
            spawnWave(data, bars, accessor);
        } else {
            spawnLine(data, bars, accessor);
        }
    }

    private static void spawnLine(@NotNull VisualizerData data,
                                  float @NotNull [] bars,
                                  @NotNull ComponentAccessor<EntityStore> accessor) {

        VisualPreset preset = data.getPreset();
        VisualizerUtil.LinearSetup setup = VisualizerUtil.calculateLinearSetup(data, bars.length, 0.0);
        double baseY = data.getPos().getY() + BASE_Y_OFFSET;

        for (int i = 0; i < bars.length; i++) {
            float value = VisualizerUtil.clamp01(bars[i]);
            if (value <= 0.02f) continue;

            VisualizerUtil.Position2D pos2d = VisualizerUtil.getLinearBarPosition(
                    setup.centerX(), setup.centerZ(), setup.dir(), setup.startOffset(), i, preset.getSpacing()
            );

            spawnBarParticles(preset, accessor, pos2d.x(), baseY, pos2d.z(), value);
        }
    }

    private static void spawnCircle(@NotNull VisualizerData data,
                                    float @NotNull [] bars,
                                    @NotNull ComponentAccessor<EntityStore> accessor) {

        VisualPreset preset = data.getPreset();
        double radius = Math.max(1.0, preset.getRadius());
        double baseY = data.getPos().getY() + BASE_Y_OFFSET;

        for (int i = 0; i < bars.length; i++) {
            float value = VisualizerUtil.clamp01(bars[i]);
            if (value <= 0.02f) continue;

            VisualizerUtil.Position2D pos2d = VisualizerUtil.getCircleBarPosition(data, i, bars.length, radius);
            spawnBarParticles(preset, accessor, pos2d.x(), baseY, pos2d.z(), value);
        }
    }

    private static void spawnPulseRing(@NotNull VisualizerData data,
                                       float @NotNull [] bars,
                                       @NotNull ComponentAccessor<EntityStore> accessor) {

        VisualPreset preset = data.getPreset();
        double baseRadius = Math.max(1.0, preset.getRadius());
        double spread = Math.max(1.5, preset.getRadius() * 0.35);
        double baseY = data.getPos().getY() + BASE_Y_OFFSET;
        long time = System.currentTimeMillis();

        for (int i = 0; i < bars.length; i++) {
            float value = VisualizerUtil.clamp01(bars[i]);
            if (value <= 0.02f) continue;

            VisualizerUtil.PulseRingPoint point = VisualizerUtil.getPulseRingPoint(
                    data, value, i, bars.length, baseRadius, spread, time
            );

            spawnBarParticles(preset, accessor, point.x(), baseY, point.z(), point.value());
        }
    }

    private static void spawnWave(@NotNull VisualizerData data,
                                  float @NotNull [] bars,
                                  @NotNull ComponentAccessor<EntityStore> accessor) {

        VisualPreset preset = data.getPreset();
        VisualizerUtil.LinearSetup setup = VisualizerUtil.calculateLinearSetup(data, bars.length, 0.0);
        double baseY = data.getPos().getY() + BASE_Y_OFFSET;

        for (int i = 0; i < bars.length; i++) {
            float value = VisualizerUtil.clamp01(bars[i]);
            if (value <= 0.02f) continue;

            VisualizerUtil.Position2D pos2d = VisualizerUtil.getLinearBarPosition(
                    setup.centerX(), setup.centerZ(), setup.dir(), setup.startOffset(), i, preset.getSpacing()
            );

            double waveYOffset = Math.sin((i * preset.getWaveFrequency()) + Math.toRadians(data.getRot().getY())) * preset.getWaveAmplitude();
            spawnBarParticles(preset, accessor, pos2d.x(), baseY + waveYOffset, pos2d.z(), value);
        }
    }

    private static void spawnBarParticles(@NotNull VisualPreset preset,
                                          @NotNull ComponentAccessor<EntityStore> accessor,
                                          double x, double y, double z,
                                          float value) {

        String particle = value > 0.45f ? preset.getHighParticle() : preset.getLowParticle();

        ParticleUtil.spawnParticleEffect(
                preset.getLowParticle(),
                new Vector3d(x, y, z),
                accessor
        );

        ParticleUtil.spawnParticleEffect(
                particle,
                new Vector3d(x, y + (value * (preset.getMaxHeight() * 0.15)), z),
                accessor
        );

        if (value > 0.80f && preset.getHitParticle() != null && !preset.getHitParticle().isBlank()) {
            ParticleUtil.spawnParticleEffect(
                    preset.getHitParticle(),
                    new Vector3d(x, y + 0.7, z),
                    accessor
            );
        }
    }
}