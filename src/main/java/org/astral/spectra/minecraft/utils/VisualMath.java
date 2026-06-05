package org.astral.spectra.minecraft.utils;

import org.astral.spectra.audio.api.AudioAPI;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;

import java.awt.*;

public final class VisualMath {

    public static double globalPhase = 0;

    public static void updatePhase() {
        double speed = 0.01 + (AudioAPI.getGlobalEnergy() * 0.05);
        globalPhase += speed;
    }

    public static org.bukkit.@NonNull Color getDynamicColor(int index, int total) {
        float hue = (float) (((globalPhase * 0.2) + ((double) index / total)) % 1.0);
        float saturation = Math.clamp(0.9f - (AudioAPI.getKickIntensity() * 0.3f), 0.5f, 1.0f);
        float brightness = Math.clamp(0.8f + (AudioAPI.getGlobalEnergy() * 0.2f), 0.6f, 1.0f);

        Color awtColor = java.awt.Color.getHSBColor(hue, saturation, brightness);
        return org.bukkit.Color.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
    }

    public static @NonNull Vector getRightVector(@NonNull Vector direction) {
        Vector up = new Vector(0, 1, 0);
        if (Math.abs(direction.getY()) > 0.99) {
            up = new Vector(1, 0, 0);
        }
        return direction.clone().crossProduct(up).normalize();
    }

    public static @NonNull Vector getUpVector(Vector direction, @NonNull Vector right) {
        return right.clone().crossProduct(direction).normalize();
    }

    public static @NonNull Vector getOffset(@NonNull String shape, int i, int total, double radius, double intensity, double spacing, double maxHeight) {
        double angle = (2 * Math.PI / total) * i;

        return switch (shape.toLowerCase()) {
            case "tornado" -> {
                double yFactor = (double) i / total;
                double tRadius = (radius * yFactor) + 1.0;
                double yPos = yFactor * maxHeight;
                yield new Vector(Math.cos(angle * 3 + globalPhase) * tRadius, yPos, Math.sin(angle * 3 + globalPhase) * tRadius);
            }
            case "helix" -> new Vector(Math.cos(angle + globalPhase) * radius, ((double) i / total) * maxHeight, Math.sin(angle + globalPhase) * radius);
            case "sphere" -> {
                double phi = Math.acos(1 - 2 * (double) i / total);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + globalPhase;
                double r = radius + (AudioAPI.getKickIntensity() * 2.0);
                yield new Vector(Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r, Math.sin(theta) * Math.sin(phi) * r);
            }
            case "pulse_ring" -> {
                double r = radius + (intensity * 4.0) + (Math.sin(globalPhase * 2) * 0.5);
                yield new Vector(Math.cos(angle) * r, 0, Math.sin(angle) * r);
            }
            case "heart" -> {
                double x = 16 * Math.pow(Math.sin(angle), 3);
                double z = 13 * Math.cos(angle) - 5 * Math.cos(2 * angle) - 2 * Math.cos(3 * angle) - Math.cos(4 * angle);
                yield new Vector(x * (radius / 10), Math.sin(globalPhase + i) * 0.5, -z * (radius / 10));
            }
            case "spiral" -> {
                double angleMulti = (4 * Math.PI / total) * i;
                double sRadius = (radius / total) * i + (intensity * 2);
                yield new Vector(Math.cos(angleMulti + globalPhase) * sRadius, 0, Math.sin(angleMulti + globalPhase) * sRadius);
            }
            case "wave" -> new Vector(i * spacing, Math.sin((i * 0.5) + globalPhase) * 3.0, 0);
            case "circle" -> new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            default -> new Vector(i * spacing, 0, 0);
        };
    }
}