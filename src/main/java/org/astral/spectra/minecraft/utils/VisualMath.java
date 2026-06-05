package org.astral.spectra.minecraft.utils;

import org.astral.spectra.audio.api.AudioAPI;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;

public final class VisualMath {
    public static @NonNull Vector getOffset(@NonNull String shape, int i, int total, double radius, double intensity, double spacing) {
        double angle = (2 * Math.PI / total) * i;
        double speedFactor = 1.0 + (AudioAPI.getGlobalEnergy() * 2.0);
        double time = (System.currentTimeMillis() / 1000.0) * speedFactor;

        return switch (shape.toLowerCase()) {
            case "tornado" -> {
                double tornadoRadius = radius * ((double) i / total);
                yield new Vector(Math.cos(angle + time * 2) * tornadoRadius, 0, Math.sin(angle + time * 2) * tornadoRadius);
            }
            case "helix" -> new Vector(Math.cos(angle + time) * radius, ((double) i / total) * 6.0, Math.sin(angle + time) * radius);
            case "sphere" -> {
                double phi = Math.acos(1 - 2 * (double) i / total);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + time;
                double r = radius + (AudioAPI.getKickIntensity() * 3.0);
                yield new Vector(Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r, Math.sin(theta) * Math.sin(phi) * r);
            }
            case "pulse_ring" -> {
                double r = radius + (intensity * 5.0) + (Math.sin(time * 4) * 0.5);
                yield new Vector(Math.cos(angle) * r, 0, Math.sin(angle) * r);
            }
            case "heart" -> {
                double x = 16 * Math.pow(Math.sin(angle), 3);
                double z = 13 * Math.cos(angle) - 5 * Math.cos(2 * angle) - 2 * Math.cos(3 * angle) - Math.cos(4 * angle);
                yield new Vector(x * (radius / 10), Math.sin(time + i) * 0.5, -z * (radius / 10));
            }
            case "spiral" -> {
                double spiralR = (radius / total) * i;
                yield new Vector(Math.cos(angle + time) * spiralR, 0, Math.sin(angle + time) * spiralR);
            }
            case "wave" -> new Vector(i * spacing, Math.sin((i * 0.5) + time) * 3.0, 0);
            case "circle" -> new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            default -> new Vector(i * spacing, 0, 0);
        };
    }
}