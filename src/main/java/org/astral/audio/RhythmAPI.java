package org.astral.audio;

import java.util.concurrent.atomic.AtomicBoolean;

public class RhythmAPI {

    private static final AtomicBoolean bassHit = new AtomicBoolean(false);
    private static final AtomicBoolean snareHit = new AtomicBoolean(false);

    public static float globalEnergy = 0.0f;
    public static int hitCombo = 0;
    public static float gameTimeOfDay = 6.0f;
    public static float particleSpeedMultiplier = 1.0f;
    public static boolean isPaused = false;

    // NUEVO: Aquí guardaremos el valor visual de las 16 barras (de 0.0f a 1.0f)
    public static float[] currentBars = new float[16];

    protected static void triggerBass() { bassHit.set(true); }
    protected static void triggerSnare() { snareHit.set(true); }

    public static boolean popBassEvent() {
        return bassHit.getAndSet(false);
    }

    public static boolean popSnareEvent() {
        return snareHit.getAndSet(false);
    }

    public static float getEnergy() {
        return globalEnergy;
    }

    // =========================================================
    //        NUEVOS GETTERS PARA LAS BARRAS
    // =========================================================

    /**
     * Obtiene el valor exacto de una barra específica (0 a 15).
     * Devuelve un valor entre 0.0f y 1.0f.
     */
    public static float getBarValue(int index) {
        if (index < 0 || index >= 16) return 0.0f;
        return currentBars[index];
    }

    /**
     * Obtiene todas las barras de golpe (útil si haces un bucle 'for' en tu mod).
     */
    public static float[] getAllBars() {
        return currentBars;
    }
}