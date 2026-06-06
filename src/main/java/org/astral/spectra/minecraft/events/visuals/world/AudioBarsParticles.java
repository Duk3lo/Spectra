package org.astral.spectra.minecraft.events.visuals.world;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.astral.spectra.minecraft.events.visuals.VisualizerData;
import org.astral.spectra.minecraft.utils.VisualMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;

public final class AudioBarsParticles {

    public static void spawn(@NonNull VisualizerData data, float @NonNull [] bars) {
        VisualPreset preset = data.getPreset();
        if (bars.length == 0) return;

        double rotationAngle = Math.toRadians(-data.getYaw());

        for (int i = 0; i < bars.length; i++) {
            float val = Math.clamp(bars[i], 0, 1);
            if (val <= 0.05f) continue;

            Vector offset = VisualMath.getOffset(preset.getShape(), i, bars.length, preset.getRadius(), val, preset.getSpacing(), preset.getMaxHeight());

            offset.rotateAroundY(rotationAngle);

            Location base = data.getPos().clone().add(offset);
            double height = val * preset.getMaxHeight();
            Location top = base.clone().add(0, height, 0);

            Color dynColor = VisualMath.getDynamicColor(i, bars.length);

            double localSpiral = VisualMath.globalPhase * 5.0 + i;
            double spiralX = Math.cos(localSpiral) * 0.6;
            double spiralZ = Math.sin(localSpiral) * 0.6;

            double floatUp = (VisualMath.globalPhase * 3.0 + (i * 0.2)) % 3.0;

            Location spiralLoc = top.clone().add(spiralX, floatUp, spiralZ);
            spawnColored(spiralLoc, dynColor, 1.0f, 1);

            spawnSafe(preset.getLowParticle(), base);
            spawnSafe(preset.getHighParticle(), top);
        }
    }

    public static void spawnBeatEffect(@NonNull VisualizerData data, float @NonNull [] bars) {
        VisualPreset preset = data.getPreset();
        float intensity = AudioAPI.getKickIntensity();
        double rotationAngle = Math.toRadians(-data.getYaw());

        for (int i = 0; i < bars.length; i++) {
            float val = Math.clamp(bars[i], 0, 1);
            if (val <= 0.1f) continue;

            Vector offset = VisualMath.getOffset(preset.getShape(), i, bars.length, preset.getRadius(), val, preset.getSpacing(), preset.getMaxHeight());

            offset.rotateAroundY(rotationAngle);

            Location top = data.getPos().clone().add(offset).add(0, val * preset.getMaxHeight(), 0);

            if (intensity > 0.6f) {
                org.bukkit.Color beatColor = VisualMath.getDynamicColor(i, bars.length);
                spawnColored(top, beatColor, 1.5f + intensity, 3);
            }
        }
    }

    public static void spawnColored(Location loc, org.bukkit.Color color, float size, int count) {
        if (loc == null || loc.getWorld() == null || color == null) return;
        Particle.DustOptions dust = new Particle.DustOptions(color, size);
        loc.getWorld().spawnParticle(Particle.DUST, loc, count, 0.1, 0.1, 0.1, 0, dust);
    }

    public static void spawnSafe(Particle particle, Location loc) {
        if (particle == null || loc == null || loc.getWorld() == null) return;
        try {
            if (particle.getDataType() == Void.class) {
                loc.getWorld().spawnParticle(particle, loc, 1, 0.02, 0.05, 0.02, 0.01);
            }
        } catch (Exception ignored) {}
    }
}