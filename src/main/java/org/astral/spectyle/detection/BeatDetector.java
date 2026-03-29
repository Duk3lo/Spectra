package org.astral.spectyle.detection;

import org.astral.spectyle.analysis.SpectrumAnalyzer;
import org.astral.spectyle.api.AudioAPI;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.state.AudioBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class BeatDetector {
    private final AudioConfig config;

    private final float[] smoothedBars;
    private final float[] bandMaxes;
    private final float[] previousFrame;
    private final float[] beatIntensity;
    private final long[] lastEventTime;
    private final float[] globalBandMax;
    private final float[] globalBandAvg;

    private final int bassEndIndex;
    private final int snareEndIndex;
    private long lastHitComboTimeMs = 0;

    private final Map<String, Float> reactiveSignals = new LinkedHashMap<>();

    public BeatDetector(@NotNull AudioConfig config) {
        this.config = config;

        int numBars = config.getNumBars();

        smoothedBars = new float[numBars];
        bandMaxes = new float[numBars];
        previousFrame = new float[numBars];
        beatIntensity = new float[numBars];
        lastEventTime = new long[numBars];
        globalBandMax = new float[numBars];
        globalBandAvg = new float[numBars];

        Arrays.fill(bandMaxes, 1.0f);

        bassEndIndex = Math.max(1, (int) (numBars * 0.15f));
        snareEndIndex = Math.max(bassEndIndex + 1, (int) (numBars * 0.50f));
    }

    public void preScan(@NotNull AudioBuffer buffer, SpectrumAnalyzer analyzer) {
        System.out.println("\u001B[33m[BeatEngine] Pre-escaneando canción (Análisis Multi-Banda)...\u001B[0m");
        long start = System.currentTimeMillis();

        int numBars = config.getNumBars();
        int fftSize = config.getFftSize();
        int numChannels = buffer.channels();

        Arrays.fill(globalBandMax, 0f);
        Arrays.fill(globalBandAvg, 0f);

        float[] sumEnergy = new float[numBars];
        int frames = 0;

        for (int idx = 0; idx + (fftSize * numChannels) < buffer.pcmData().capacity(); idx += fftSize * numChannels) {
            float[] frame = analyzer.computeFFT(analyzer.extractSamples(buffer, idx));
            for (int i = 0; i < numBars; i++) {
                if (frame[i] > globalBandMax[i]) globalBandMax[i] = frame[i];
                sumEnergy[i] += frame[i];
            }
            frames++;
        }

        for (int i = 0; i < numBars; i++) {
            globalBandAvg[i] = sumEnergy[i] / Math.max(1, frames);
        }

        System.out.println("\u001B[32m[BeatEngine] Escaneo completado en " + (System.currentTimeMillis() - start) + "ms.\u001B[0m");
    }

    public void processFrame(float[] currentFrame, long currentTime) {
        int numBars = config.getNumBars();
        float totalFrameEnergy = 0.0f;

        float maxKickInt = 0f;
        float maxSnareInt = 0f;
        float maxHatInt = 0f;

        float bassEnergy = 0f;
        float midEnergy = 0f;
        float highEnergy = 0f;

        int bassCount = 0;
        int midCount = 0;
        int highCount = 0;

        for (int i = 0; i < numBars; i++) {
            if (currentFrame[i] > bandMaxes[i]) {
                bandMaxes[i] = currentFrame[i];
            } else {
                bandMaxes[i] *= 0.992f;
            }

            float rawValue = currentFrame[i] / Math.max(bandMaxes[i], 0.1f);
            long currentCooldown = (i < bassEndIndex) ? config.getBassCooldownMs()
                    : (i < snareEndIndex) ? config.getSnareCooldownMs()
                      : config.getHatCooldownMs();

            boolean isHit = checkIsHit(i, rawValue, currentFrame);

            if (isHit && (currentTime - lastEventTime[i] >= currentCooldown)) {
                beatIntensity[i] = 1.0f;
                lastEventTime[i] = currentTime;

                if (i < bassEndIndex) AudioAPI.triggerBass();
                else if (i < snareEndIndex) AudioAPI.triggerSnare();
                else AudioAPI.triggerHat();
            } else {
                beatIntensity[i] = Math.max(0.0f, beatIntensity[i] - 0.08f);
            }

            previousFrame[i] = rawValue;

            float speed = (rawValue > smoothedBars[i]) ? config.getAttack() : config.getDecay();
            smoothedBars[i] += (rawValue - smoothedBars[i]) * speed;
            totalFrameEnergy += smoothedBars[i];

            if (i < bassEndIndex) {
                maxKickInt = Math.max(maxKickInt, beatIntensity[i]);
                bassEnergy += smoothedBars[i];
                bassCount++;
            } else if (i < snareEndIndex) {
                maxSnareInt = Math.max(maxSnareInt, beatIntensity[i]);
                midEnergy += smoothedBars[i];
                midCount++;
            } else {
                maxHatInt = Math.max(maxHatInt, beatIntensity[i]);
                highEnergy += smoothedBars[i];
                highCount++;
            }
        }

        float targetEnergy = totalFrameEnergy / Math.max(1, numBars);

        AudioAPI.setBars(smoothedBars);
        AudioAPI.updateGlobalEnergy(targetEnergy, 0.4f, 0.08f);
        AudioAPI.setKickIntensity(maxKickInt);
        AudioAPI.setSnareIntensity(maxSnareInt);
        AudioAPI.setHatIntensity(maxHatInt);

        if (maxHatInt > 0.85f && (currentTime - lastEventTime[Math.max(0, snareEndIndex - 1)] >= config.getHatCooldownMs())) {
            AudioAPI.triggerHat();
            lastEventTime[Math.max(0, snareEndIndex - 1)] = currentTime;
        }

        float kickCap = 0.85f;
        float kickNormalized = Math.min(maxKickInt / kickCap, 1.0f);
        float kickResponse = kickNormalized * kickNormalized;
        AudioAPI.setParticleSpeedMultiplier(1.0f + (kickResponse * 2.5f));

        boolean comboHit = (beatIntensity.length > 1 && beatIntensity[1] == 1.0f)
                || (bassEndIndex + 1 < beatIntensity.length && beatIntensity[bassEndIndex + 1] == 1.0f);

        if (comboHit) {
            AudioAPI.incrementHitCombo();
            lastHitComboTimeMs = currentTime;
        } else if (currentTime - lastHitComboTimeMs > 1500) {
            AudioAPI.resetHitCombo();
        }

        buildReactiveSignals(
                targetEnergy,
                maxKickInt,
                maxSnareInt,
                maxHatInt,
                bassEnergy,
                midEnergy,
                highEnergy,
                bassCount,
                midCount,
                highCount
        );
    }

    private void buildReactiveSignals(float targetEnergy,
                                      float maxKickInt,
                                      float maxSnareInt,
                                      float maxHatInt,
                                      float bassEnergy,
                                      float midEnergy,
                                      float highEnergy,
                                      int bassCount,
                                      int midCount,
                                      int highCount) {
        reactiveSignals.clear();

        reactiveSignals.put("kick", maxKickInt);
        reactiveSignals.put("snare", maxSnareInt);
        reactiveSignals.put("hat", maxHatInt);
        reactiveSignals.put("energy", clamp01(targetEnergy));
        reactiveSignals.put("bassEnergy", clamp01(bassCount == 0 ? 0f : bassEnergy / bassCount));
        reactiveSignals.put("midEnergy", clamp01(midCount == 0 ? 0f : midEnergy / midCount));
        reactiveSignals.put("highEnergy", clamp01(highCount == 0 ? 0f : highEnergy / highCount));
        reactiveSignals.put("combo", Math.min(AudioAPI.getHitCombo() / 20.0f, 1.0f));

        addDynamicBands();
        addTopPeaks();
    }

    private void addDynamicBands() {
        int numBars = smoothedBars.length;
        int bands = Math.clamp(8, 4, numBars);

        for (int b = 0; b < bands; b++) {
            int start = (b * numBars) / bands;
            int end = ((b + 1) * numBars) / bands;

            float sum = 0f;
            for (int i = start; i < end; i++) {
                sum += smoothedBars[i];
            }

            float avg = sum / Math.max(1, end - start);
            reactiveSignals.put(String.format(Locale.US, "band_%02d", b + 1), clamp01(avg));
        }
    }

    private void addTopPeaks() {
        int n = smoothedBars.length;
        boolean[] blocked = new boolean[n];

        for (int peakIndex = 0; peakIndex < 6; peakIndex++) {
            int bestIdx = -1;
            float bestValue = 0f;

            for (int i = 0; i < n; i++) {
                if (blocked[i]) continue;

                float value = smoothedBars[i];
                if (value > bestValue) {
                    bestValue = value;
                    bestIdx = i;
                }
            }

            if (bestIdx < 0 || bestValue < 0.05f) {
                break;
            }

            reactiveSignals.put(String.format(Locale.US, "peak_%02d", peakIndex + 1), clamp01(bestValue));

            int from = Math.max(0, bestIdx - 1);
            int to = Math.min(n - 1, bestIdx + 1);
            for (int i = from; i <= to; i++) {
                blocked[i] = true;
            }
        }
    }

    private boolean checkIsHit(int i, float rawValue, float[] currentFrame) {
        float jumpRequired = (i < bassEndIndex) ? config.getBassJumpThreshold()
                : ((i < snareEndIndex) ? config.getSnareJumpThreshold() : config.getHatJumpThreshold());

        float minThreshold = (i < bassEndIndex) ? 0.50f : 0.25f;
        float absoluteFloor = globalBandMax[i] * ((i < bassEndIndex) ? 0.30f : 0.20f);
        float avgFloor = globalBandAvg[i] * ((i < bassEndIndex) ? 1.50f : 1.20f);

        return (rawValue > previousFrame[i] * jumpRequired)
                && (rawValue > minThreshold)
                && (currentFrame[i] > absoluteFloor)
                && (currentFrame[i] > avgFloor);
    }

    private float clamp01(float v) {
        if (v < 0f) return 0f;
        return Math.min(v, 1f);
    }

    public float[] getSmoothedBars() {
        return smoothedBars;
    }

    public float[] getBeatIntensity() {
        return beatIntensity;
    }

    public Map<String, Float> getReactiveSignals() {
        return reactiveSignals;
    }
}