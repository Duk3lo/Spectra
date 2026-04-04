package org.astral.spectyle.hytale.events.event.world;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public final class AudioBarsParticles {

    private static final String BAR_HIGH = "BeamEmiter_Heal_Green";
    private static final String BAR_LOW = "BeamEmiter_Heal_Red";

    private static final double BACK_DISTANCE = 1.6;
    private static final double BASE_Y_OFFSET = 0.35;

    // 🔹 Nueva constante: Distancia entre cada barrita
    private static final double SPACING = 0.3;

    private AudioBarsParticles() {
    }

    public static void spawnBarsBehind(@NotNull Vector3d playerPos,
                                       @NotNull Vector3f headRotation,
                                       float[] bars,
                                       ComponentAccessor<EntityStore> accessor) {

        if (bars == null || bars.length == 0) return;

        double yawDegrees = -headRotation.getY();
        double yaw = Math.toRadians(yawDegrees);

        // 🧭 Vector hacia adelante (Forward)
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);

        // 🧭 Vector hacia la derecha (Right) para colocar las barras lado a lado
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);

        // 📍 Punto central detrás del jugador
        double centerX = playerPos.getX() - (forwardX * BACK_DISTANCE);
        double centerZ = playerPos.getZ() - (forwardZ * BACK_DISTANCE);
        double baseY = playerPos.getY() + BASE_Y_OFFSET;

        // 📏 Calcular el ancho total para que queden centradas respecto a la espalda
        double totalWidth = (bars.length - 1) * SPACING;
        double startOffset = -totalWidth / 2.0;

        // 🔄 Bucle para dibujar CADA barrita por separado
        for (int i = 0; i < bars.length; i++) {
            float value = clamp01(bars[i]);

            // 🛑 Si el valor es muy bajo (silencio), NO generamos partículas para esta barrita
            // Esto ayuda a "limpiar" la pantalla automáticamente cuando la música hace pausas
            if (value <= 0.02f) {
                continue;
            }

            // Calculamos cuánto nos movemos hacia un lado para esta barrita
            double currentOffset = startOffset + (i * SPACING);

            // Posición X y Z finales para esta barrita específica
            double barX = centerX + (rightX * currentOffset);
            double barZ = centerZ + (rightZ * currentOffset);

            // 🎨 Color según intensidad de esta frecuencia específica
            String particle = value > 0.45f ? BAR_HIGH : BAR_LOW;

            // 🔴 Bolita base (fija)
            ParticleUtil.spawnParticleEffect(
                    BAR_LOW,
                    new Vector3d(barX, baseY, barZ),
                    accessor
            );

            // 🟢 Bolita superior (barra que sube y baja)
            ParticleUtil.spawnParticleEffect(
                    particle,
                    new Vector3d(barX, baseY + (value * 1.5), barZ),
                    accessor
            );
        }
    }

    private static float clamp01(float v) {
        return Math.clamp(v, 0f, 1f);
    }
}