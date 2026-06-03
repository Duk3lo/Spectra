package org.astral.spectra.hytale.events.visuals.world;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import org.astral.spectra.hytale.configuration.VisualsConfig.VisualPreset;
import org.astral.spectra.hytale.events.visuals.VisualizerManager.VisualizerData;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class AudioBarsBlocks {

    private static final double FRONT_DISTANCE = 0.0;

    private AudioBarsBlocks() {}

    public static void draw(@NotNull VisualizerData data, float @NotNull [] bars, @NotNull World world) {
        VisualPreset preset = data.getPreset();
        if (preset == null) return;

        clearJustBlocks(world, data.getActiveBlocks());

        if (bars.length == 0) return;

        if (preset.isPulseRing()) {
            drawPulseRing(data, bars, world);
        } else if (preset.isCircle() || preset.isRing()) {
            drawCircle(data, bars, world);
        } else if (preset.isWave()) {
            drawWave(data, bars, world);
        } else {
            drawLine(data, bars, world);
        }
    }

    private static void drawLine(@NotNull VisualizerData data, float @NotNull [] bars, @NotNull World world) {
        VisualPreset preset = data.getPreset();
        Set<Vector3i> activeBlocks = data.getActiveBlocks();
        VisualizerUtil.LinearSetup setup = VisualizerUtil.calculateLinearSetup(data, bars.length, FRONT_DISTANCE);

        for (int i = 0; i < bars.length; i++) {
            float value = VisualizerUtil.clamp01(bars[i]);
            if (value <= 0.05f) continue;

            VisualizerUtil.Position2D pos2d = VisualizerUtil.getLinearBarPosition(
                    setup.centerX(), setup.centerZ(), setup.dir(), setup.startOffset(), i, preset.getSpacing()
            );

            int blocksHigh = (int) Math.ceil(value * preset.getMaxHeight());
            VisualizerUtil.drawVerticalBar(world, activeBlocks, preset, pos2d.x(), setup.baseY(), pos2d.z(), blocksHigh);
        }
    }

    private static void drawCircle(@NotNull VisualizerData data, float @NotNull [] bars, @NotNull World world) {
        VisualPreset preset = data.getPreset();
        Set<Vector3i> activeBlocks = data.getActiveBlocks();

        int baseY = (int) Math.floor(data.getPos().getY());
        double radius = Math.max(1.0, preset.getRadius());

        for (int i = 0; i < bars.length; i++) {
            float value = VisualizerUtil.clamp01(bars[i]);
            if (value <= 0.02f) continue;

            VisualizerUtil.Position2D pos2d = VisualizerUtil.getCircleBarPosition(data, i, bars.length, radius);
            int blocksHigh = (int) Math.ceil(value * preset.getMaxHeight());

            VisualizerUtil.drawVerticalBar(world, activeBlocks, preset, pos2d.x(), baseY, pos2d.z(), blocksHigh);
        }
    }

    private static void drawPulseRing(@NotNull VisualizerData data, float @NotNull [] bars, @NotNull World world) {
        VisualPreset preset = data.getPreset();
        Set<Vector3i> activeBlocks = data.getActiveBlocks();

        int baseY = (int) Math.floor(data.getPos().getY());
        double baseRadius = Math.max(1.0, preset.getRadius());
        double spread = Math.max(1.5, preset.getRadius() * 0.35);
        long time = System.currentTimeMillis();

        for (int i = 0; i < bars.length; i++) {
            float value = VisualizerUtil.clamp01(bars[i]);
            if (value <= 0.02f) continue;

            VisualizerUtil.PulseRingPoint point = VisualizerUtil.getPulseRingPoint(
                    data, value, i, bars.length, baseRadius, spread, time
            );

            int blocksHigh = 1 + (int) Math.round(point.value() * 2.0);
            VisualizerUtil.drawVerticalBar(world, activeBlocks, preset, point.x(), baseY, point.z(), blocksHigh);
        }
    }

    private static void drawWave(@NotNull VisualizerData data, float @NotNull [] bars, @NotNull World world) {
        VisualPreset preset = data.getPreset();
        Set<Vector3i> activeBlocks = data.getActiveBlocks();
        VisualizerUtil.LinearSetup setup = VisualizerUtil.calculateLinearSetup(data, bars.length, FRONT_DISTANCE);

        for (int i = 0; i < bars.length; i++) {
            float value = VisualizerUtil.clamp01(bars[i]);
            if (value <= 0.05f) continue;

            VisualizerUtil.Position2D pos2d = VisualizerUtil.getLinearBarPosition(
                    setup.centerX(), setup.centerZ(), setup.dir(), setup.startOffset(), i, preset.getSpacing()
            );

            double waveYOffset = Math.sin((i * preset.getWaveFrequency()) + Math.toRadians(data.getRot().getY())) * preset.getWaveAmplitude();
            int waveBaseY = setup.baseY() + (int) Math.round(waveYOffset);
            int blocksHigh = (int) Math.ceil(value * preset.getMaxHeight());

            VisualizerUtil.drawVerticalBar(world, activeBlocks, preset, pos2d.x(), waveBaseY, pos2d.z(), blocksHigh);
        }
    }

    private static void clearJustBlocks(@NotNull World world, @NotNull Set<Vector3i> activeBlocks) {
        if (!activeBlocks.isEmpty()) {
            for (Vector3i pos : activeBlocks) {
                VisualizerUtil.removeBlock(world, pos);
            }
            activeBlocks.clear();
        }
    }

    public static void stopAndReset(@NotNull World world, @NotNull VisualizerData data) {
        clearJustBlocks(world, data.getActiveBlocks());
    }
}