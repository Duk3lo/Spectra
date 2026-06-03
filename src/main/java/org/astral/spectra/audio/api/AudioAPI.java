package org.astral.spectra.audio.api;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class AudioAPI {

    private static final AtomicBoolean bassHit = new AtomicBoolean(false);
    private static final AtomicBoolean snareHit = new AtomicBoolean(false);
    private static final AtomicBoolean hatHit = new AtomicBoolean(false);

    private static volatile float globalEnergy = 0.0f;
    private static final AtomicInteger hitCombo = new AtomicInteger(0);
    private static volatile float particleSpeedMultiplier = 1.0f;

    private static volatile boolean paused = false;
    private static volatile boolean playing = false;

    private static volatile float kickIntensity = 0.0f;
    private static volatile float snareIntensity = 0.0f;
    private static volatile float hatIntensity = 0.0f;

    private static volatile float currentPositionSeconds = 0.0f;
    private static volatile float totalDurationSeconds = 0.0f;
    private static volatile float volume = 0.0f;

    private static volatile float[] currentBars = new float[0];

    private static volatile ReactiveSnapshot reactiveSnapshot = new ReactiveSnapshot(
            0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f
    );

    private AudioAPI() {
    }

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

    public static void triggerBass() {
        setBassHit(true);
    }

    public static void triggerSnare() {
        setSnareHit(true);
    }

    public static void triggerHat() {
        setHatHit(true);
    }

    public static boolean isBassHit() {
        return bassHit.get();
    }

    public static boolean isSnareHit() {
        return snareHit.get();
    }

    public static boolean isHatHit() {
        return hatHit.get();
    }

    public static void setBassHit(boolean value) {
        bassHit.set(value);
    }

    public static void setSnareHit(boolean value) {
        snareHit.set(value);
    }

    public static void setHatHit(boolean value) {
        hatHit.set(value);
    }

    public static boolean consumeBassHit() {
        return bassHit.getAndSet(false);
    }

    public static boolean consumeSnareHit() {
        return snareHit.getAndSet(false);
    }

    public static boolean consumeHatHit() {
        return hatHit.getAndSet(false);
    }

    public static float getBarValue(int index) {
        float[] bars = currentBars;
        if (index < 0 || index >= bars.length) return 0.0f;
        return bars[index];
    }

    public static float @NotNull [] getAllBars() {
        return Arrays.copyOf(currentBars, currentBars.length);
    }

    public static void setBars(float[] newBars) {
        currentBars = (newBars == null) ? new float[0] : Arrays.copyOf(newBars, newBars.length);
    }

    public static float getCurrentTime() {
        return currentPositionSeconds;
    }

    public static void setCurrentPositionSeconds(float value) {
        currentPositionSeconds = value;
    }

    public static float getTotalTime() {
        return totalDurationSeconds;
    }

    public static void setTotalDurationSeconds(float value) {
        totalDurationSeconds = value;
    }

    public static float getProgress() {
        float total = totalDurationSeconds;
        if (total <= 0) return 0.0f;
        return currentPositionSeconds / total;
    }

    public static int getHitCombo() {
        return hitCombo.get();
    }

    public static void setHitCombo(int value) {
        hitCombo.set(Math.max(0, value));
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

    public static void setGlobalEnergy(float value) {
        globalEnergy = value;
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

    public static boolean isPaused() {
        return paused;
    }

    public static void setPaused(boolean value) {
        paused = value;
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static void setPlaying(boolean value) {
        playing = value;
    }

    public static float getKickIntensity() {
        return kickIntensity;
    }

    public static void setKickIntensity(float value) {
        kickIntensity = value;
    }

    public static float getSnareIntensity() {
        return snareIntensity;
    }

    public static void setSnareIntensity(float value) {
        snareIntensity = value;
    }

    public static float getHatIntensity() {
        return hatIntensity;
    }

    public static void setHatIntensity(float value) {
        hatIntensity = value;
    }

    public static ReactiveSnapshot getReactiveSnapshot() {
        return reactiveSnapshot;
    }

    public static void setReactiveSnapshot(ReactiveSnapshot snapshot) {
        if (snapshot != null) {
            reactiveSnapshot = snapshot;
        }
    }

    public static float getVolume() {
        return volume;
    }

    public static void setVolume(float value) {
        volume = value;
    }

    public static void reset() {
        globalEnergy = 0.0f;
        hitCombo.set(0);

        kickIntensity = 0.0f;
        snareIntensity = 0.0f;
        hatIntensity = 0.0f;

        currentPositionSeconds = 0.0f;
        totalDurationSeconds = 0.0f;

        playing = false;
        paused = false;

        particleSpeedMultiplier = 1.0f;
        currentBars = new float[0];

        reactiveSnapshot = new ReactiveSnapshot(
                0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f
        );

        bassHit.set(false);
        snareHit.set(false);
        hatHit.set(false);
    }
}