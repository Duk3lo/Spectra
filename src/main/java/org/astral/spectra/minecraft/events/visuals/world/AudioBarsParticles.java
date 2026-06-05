package org.astral.spectra.minecraft.events.visuals.world;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.astral.spectra.minecraft.events.visuals.VisualizerData;
import org.astral.spectra.minecraft.utils.VisualMath;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;

public final class AudioBarsParticles {

    public static void spawn(@NonNull VisualizerData data, float @NonNull [] bars) {
        VisualPreset preset = data.getPreset();
        if (bars.length == 0) return;

        for (int i = 0; i < bars.length; i++) {
            float val = Math.clamp(bars[i], 0, 1);
            if (val <= 0.05f) continue;

            Vector offset = VisualMath.getOffset(preset.getShape(), i, bars.length, preset.getRadius(), val, preset.getSpacing());
            Location base = data.getPos().clone().add(offset);
            double height = val * preset.getMaxHeight();

            spawnSafe(preset.getLowParticle(), base, 1, 0, 0, 0, 0.01);

            Location top = base.clone().add(0, height, 0);
            spawnSafe(preset.getHighParticle(), top, 1, 0.05, 0.05, 0.05, 0.02);

            if (preset.getShape().equalsIgnoreCase("tornado")) {
                spawnSafe(Particle.CLOUD, top, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    public static void spawnBeatEffect(@NonNull VisualizerData data, float @NonNull [] bars) {
        VisualPreset preset = data.getPreset();
        float intensity = AudioAPI.getKickIntensity();

        for (int i = 0; i < bars.length; i++) {
            float val = Math.clamp(bars[i], 0, 1);
            if (val <= 0.1f) continue;

            Vector offset = VisualMath.getOffset(preset.getShape(), i, bars.length, preset.getRadius(), val, preset.getSpacing());
            Location top = data.getPos().clone().add(offset).add(0, val * preset.getMaxHeight(), 0);

            // Usamos partículas que NO requieren datos (Color) para evitar el error
            Particle beatPart = (intensity > 0.8f) ? Particle.SOUL_FIRE_FLAME : Particle.FIREWORK;
            spawnSafe(beatPart, top, 2, 0.2, 0.2, 0.2, 0.1);
        }
    }

    // MÉTODO SEGURO: Evita que el servidor crashee si la partícula pide Color o DustOptions
    private static void spawnSafe(Particle particle, Location loc, int count, double offsetX, double offsetY, double offsetZ, double speed) {
        try {
            // Verificamos si la partícula requiere datos adicionales
            if (particle.getDataType() == Void.class) {
                loc.getWorld().spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, speed);
            } else {
                // Si requiere datos (como REDSTONE), usamos una alternativa simple para no crashear
                loc.getWorld().spawnParticle(Particle.CRIT, loc, count, offsetX, offsetY, offsetZ, speed);
            }
        } catch (Exception ignored) {}
    }
}