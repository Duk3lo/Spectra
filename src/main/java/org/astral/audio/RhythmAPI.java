package org.astral.audio;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RhythmAPI {

    private static final AtomicBoolean bassHit = new AtomicBoolean(false);
    private static final AtomicBoolean snareHit = new AtomicBoolean(false);
    private static final AtomicBoolean hatHit = new AtomicBoolean(false);

    private static volatile float globalEnergy = 0.0f;
    private static final AtomicInteger hitCombo = new AtomicInteger(0);
    private static volatile float particleSpeedMultiplier = 1.0f;

    private static volatile boolean isPaused = false;
    private static volatile boolean isPlaying = false;

    private static volatile float kickIntensity = 0.0f;
    private static volatile float snareIntensity = 0.0f;
    private static volatile float hatIntensity = 0.0f;

    private static volatile float currentPositionSeconds = 0.0f;
    private static volatile float totalDurationSeconds = 0.0f;
    private static volatile float volume = 0.0f;

    private static volatile float[] currentBars = new float[0];

    public static @NotNull String getCurrentTimeFormatted() {
        return formatTime(currentPositionSeconds);
    }

    public static @NotNull String getTotalTimeFormatted() {
        return formatTime(totalDurationSeconds);
    }

    private static @NotNull String formatTime(float seconds) {
        if (seconds < 0 || Float.isNaN(seconds)) seconds = 0;
        int minutes = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%02d:%02d", minutes, secs);
    }

    protected static void triggerBass() { bassHit.set(true); }
    protected static void triggerSnare() { snareHit.set(true); }
    protected static void triggerHat() { hatHit.set(true); }

    public static boolean popBassEvent() { return bassHit.getAndSet(false); }
    public static boolean popSnareEvent() { return snareHit.getAndSet(false); }
    public static boolean popHatEvent() { return hatHit.getAndSet(false); }

    public static float getBarValue(int index) {
        float[] bars = currentBars;
        if (index < 0 || index >= bars.length) return 0.0f;
        return bars[index];
    }

    public static float[] getAllBars() {
        return currentBars;
    }

    public static void setBars(float[] newBars) {
        if (newBars != null) currentBars = newBars;
    }

    public static float getCurrentTime() { return currentPositionSeconds; }
    public static float getTotalTime() { return totalDurationSeconds; }

    public static float getProgress() {
        float total = totalDurationSeconds;
        if (total <= 0) return 0.0f;
        return currentPositionSeconds / total;
    }

    public static int getHitCombo() {
        return hitCombo.get();
    }

    public static void incrementHitCombo() {
        hitCombo.incrementAndGet();
    }

    public static void resetHitCombo() {
        hitCombo.set(0);
    }

    public static float getGlobalEnergy() {
        return globalEnergy;
    }

    public static synchronized void updateGlobalEnergy(float targetEnergy, float attack, float decay) {
        float current = globalEnergy;
        float speed = (targetEnergy > current) ? attack : decay;
        globalEnergy = current + ((targetEnergy - current) * speed);
    }

    public static float getParticleSpeedMultiplier() {
        return particleSpeedMultiplier;
    }

    public static void setParticleSpeedMultiplier(float value) {
        particleSpeedMultiplier = value;
    }

    public static void setCurrentPositionSeconds(float value) {
        currentPositionSeconds = value;
    }

    public static void setTotalDurationSeconds(float value) {
        totalDurationSeconds = value;
    }

    public static void setPaused(boolean paused) {
        isPaused = paused;
    }

    public static boolean isPaused() {
        return isPaused;
    }

    public static void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public static boolean isPlaying() {
        return isPlaying;
    }

    public static void setKickIntensity(float value) { kickIntensity = value; }
    public static void setSnareIntensity(float value) { snareIntensity = value; }
    public static void setHatIntensity(float value) { hatIntensity = value; }

    public static float getKickIntensity() { return kickIntensity; }
    public static float getSnareIntensity() { return snareIntensity; }
    public static float getHatIntensity() { return hatIntensity; }

    public static void reset() {
        globalEnergy = 0.0f;
        hitCombo.set(0);

        kickIntensity = 0.0f;
        snareIntensity = 0.0f;
        hatIntensity = 0.0f;

        currentPositionSeconds = 0.0f;
        totalDurationSeconds = 0.0f;

        isPlaying = false;
        isPaused = false;

        particleSpeedMultiplier = 1.0f;

        Arrays.fill(currentBars, 0.0f);
    }

    public static float getVolume() {
        return volume;
    }

    public static void setVolume(float volume) {
        RhythmAPI.volume = volume;
    }
}