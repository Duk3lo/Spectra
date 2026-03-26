package org.astral.audio;

import java.util.concurrent.atomic.AtomicBoolean;

public class RhythmAPI {

    private static final AtomicBoolean bassHit = new AtomicBoolean(false);
    private static final AtomicBoolean snareHit = new AtomicBoolean(false);
    private static final AtomicBoolean hatHit = new AtomicBoolean(false); // NUEVO

    public static float globalEnergy = 0.0f;
    public static int hitCombo = 0;
    public static float gameTimeOfDay = 6.0f;
    public static float particleSpeedMultiplier = 1.0f;
    public static boolean isPaused = false;

    // Intensidades en tiempo real (0.0 a 1.0) para usar en animaciones
    public static float kickIntensity = 0.0f;  // NUEVO
    public static float snareIntensity = 0.0f; // NUEVO
    public static float hatIntensity = 0.0f;   // NUEVO

    public static float[] currentBars = new float[32];

    protected static void triggerBass() { bassHit.set(true); }
    protected static void triggerSnare() { snareHit.set(true); }
    protected static void triggerHat() { hatHit.set(true); } // NUEVO

    public static boolean popBassEvent() { return bassHit.getAndSet(false); }
    public static boolean popSnareEvent() { return snareHit.getAndSet(false); }
    public static boolean popHatEvent() { return hatHit.getAndSet(false); } // NUEVO

    public static float getEnergy() { return globalEnergy; }

    public static float getBarValue(int index) {
        if (index < 0 || index >= currentBars.length) return 0.0f;
        return currentBars[index];
    }

    public static float[] getAllBars() { return currentBars; }
}