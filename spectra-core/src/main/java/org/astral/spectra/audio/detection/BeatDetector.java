package org.astral.spectra.audio.detection;

import org.astral.spectra.audio.analysis.SpectrumAnalyzer;
import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.audio.api.ReactiveSnapshot;
import org.astral.spectra.audio.state.AudioBuffer;
import org.astral.spectra.config.AudioConfig;
import org.astral.spectra.logging.EngineLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class BeatDetector {
    private final AudioConfig config;
    private final EngineLogger logger;

    private final float[] smoothedBars;
    private final float[] bandMaxes;
    private final float[] previousFrame;
    private final float[] beatIntensity;
    private final long[] lastEventTime;
    private final float[] globalBandMax;
    private final float[] globalBandAvg;
    private final float[] sumEnergyBuffer;

    private final int bassEndIndex;
    private final int snareEndIndex;
    private long lastHitComboTimeMs = 0;

    private final Map<String, Float> reactiveSignals = new LinkedHashMap<>();

    public BeatDetector(@NotNull AudioConfig config, @NotNull EngineLogger logger) {
        this.config = config;
        this.logger = logger;

        int numBars = config.getVisualizer().getNumBars();

        smoothedBars = new float[numBars];
        bandMaxes = new float[numBars];
        previousFrame = new float[numBars];
        beatIntensity = new float[numBars];
        lastEventTime = new long[numBars];
        globalBandMax = new float[numBars];
        globalBandAvg = new float[numBars];
        sumEnergyBuffer = new float[numBars];

        Arrays.fill(bandMaxes, 1.0f);

        bassEndIndex = Math.max(1, (int) (numBars * 0.15f));
        snareEndIndex = Math.max(bassEndIndex + 1, (int) (numBars * 0.50f));
    }

    public void preScan(@NotNull AudioBuffer buffer, SpectrumAnalyzer analyzer) {
        logger.info("\u001B[33m[BeatEngine] Pre-scanning song (Multi-Band Analysis)...\u001B[0m");
        long start = System.currentTimeMillis();

        int numBars = config.getVisualizer().getNumBars();
        int fftSize = config.getVisualizer().getFftSize();
        int numChannels = buffer.channels();

        Arrays.fill(globalBandMax, 0f);
        Arrays.fill(globalBandAvg, 0f);
        Arrays.fill(sumEnergyBuffer, 0f);

        int frames = 0;

        for (int idx = 0; idx + (fftSize * numChannels) < buffer.pcmData().capacity(); idx += fftSize * numChannels) {
            float[] frame = analyzer.computeFFT(analyzer.extractSamples(buffer, idx));
            for (int i = 0; i < numBars; i++) {
                if (frame[i] > globalBandMax[i]) globalBandMax[i] = frame[i];
                sumEnergyBuffer[i] += frame[i];
            }
            frames++;
        }

        for (int i = 0; i < numBars; i++) {
            globalBandAvg[i] = sumEnergyBuffer[i] / Math.max(1, frames);
        }

        logger.info("\u001B[32m[BeatEngine] Scan completed in " + (System.currentTimeMillis() - start) + "ms.\u001B[0m");
    }

    public void processFrame(float[] currentFrame, long currentTime) {
        int numBars = config.getVisualizer().getNumBars();
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

            long currentCooldown = (i < bassEndIndex) ? config.getBeatDetection().getBassCooldownMs()
                    : (i < snareEndIndex) ? config.getBeatDetection().getSnareCooldownMs()
                      : config.getBeatDetection().getHatCooldownMs();

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

            float speed = (rawValue > smoothedBars[i]) ? config.getSmoothing().getAttack() : config.getSmoothing().getDecay();
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

        float kickCap = 0.85f;
        float kickNormalized = Math.min(maxKickInt / kickCap, 1.0f);
        AudioAPI.setParticleSpeedMultiplier(1.0f + (kickNormalized * kickNormalized * 2.5f));

        boolean comboHit = (beatIntensity.length > 1 && beatIntensity[1] == 1.0f)
                || (bassEndIndex + 1 < beatIntensity.length && beatIntensity[bassEndIndex + 1] == 1.0f);

        if (comboHit) {
            AudioAPI.incrementHitCombo();
            lastHitComboTimeMs = currentTime;
        } else if (currentTime - lastHitComboTimeMs > 1500) {
            AudioAPI.resetHitCombo();
        }

        buildReactiveSignals(
                targetEnergy, maxKickInt, maxSnareInt, maxHatInt,
                bassEnergy, midEnergy, highEnergy,
                bassCount, midCount, highCount
        );
    }

    private void buildReactiveSignals(float targetEnergy, float maxKickInt, float maxSnareInt, float maxHatInt,
                                      float bassEnergy, float midEnergy, float highEnergy,
                                      int bassCount, int midCount, int highCount) {
        reactiveSignals.clear();

        float bassAvg = bassCount == 0 ? 0f : bassEnergy / bassCount;
        float midAvg = midCount == 0 ? 0f : midEnergy / midCount;
        float highAvg = highCount == 0 ? 0f : highEnergy / highCount;

        float vocalPresence = (midAvg * 0.75f) + (highAvg * 0.45f);
        if (maxHatInt > 0.85f) vocalPresence *= 0.85f;

        float transientLevel = (maxKickInt + maxSnareInt + maxHatInt) / 3.0f;
        float brightness = (highAvg * 0.75f) + (maxHatInt * 0.25f);
        float subBass = bassAvg * 1.15f;
        float comboFactor = Math.min(AudioAPI.getHitCombo() / 20.0f, 1.0f);

        reactiveSignals.put("kick", clamp01(maxKickInt));
        reactiveSignals.put("snare", clamp01(maxSnareInt));
        reactiveSignals.put("hat", clamp01(maxHatInt));
        reactiveSignals.put("energy", clamp01(targetEnergy));
        reactiveSignals.put("bassEnergy", clamp01(bassAvg));
        reactiveSignals.put("midEnergy", clamp01(midAvg));
        reactiveSignals.put("highEnergy", clamp01(highAvg));
        reactiveSignals.put("vocalPresence", clamp01(vocalPresence));
        reactiveSignals.put("transientLevel", clamp01(transientLevel));
        reactiveSignals.put("brightness", clamp01(brightness));
        reactiveSignals.put("subBass", clamp01(subBass));
        reactiveSignals.put("combo", comboFactor);

        AudioAPI.setReactiveSnapshot(new ReactiveSnapshot(
                clamp01(maxKickInt), clamp01(maxSnareInt), clamp01(maxHatInt),
                clamp01(targetEnergy), clamp01(bassAvg), clamp01(midAvg),
                clamp01(highAvg), clamp01(vocalPresence),
                comboFactor, clamp01(transientLevel),
                clamp01(brightness), clamp01(subBass)
        ));
    }

    private boolean checkIsHit(int i, float rawValue, float[] currentFrame) {
        float jumpRequired = (i < bassEndIndex) ? config.getBeatDetection().getBassJumpThreshold()
                : ((i < snareEndIndex) ? config.getBeatDetection().getSnareJumpThreshold() : config.getBeatDetection().getHatJumpThreshold());

        float minThreshold = (i < bassEndIndex) ? 0.50f : 0.25f;
        float absoluteFloor = globalBandMax[i] * ((i < bassEndIndex) ? 0.30f : 0.20f);
        float avgFloor = globalBandAvg[i] * ((i < bassEndIndex) ? 1.50f : 1.20f);

        return (rawValue > previousFrame[i] * jumpRequired)
                && (rawValue > minThreshold)
                && (currentFrame[i] > absoluteFloor)
                && (currentFrame[i] > avgFloor);
    }

    private float clamp01(float v) {
        return Math.clamp(v, 0f, 1f);
    }

    public float[] getSmoothedBars() { return smoothedBars; }
    public float[] getBeatIntensity() { return beatIntensity; }
    public Map<String, Float> getReactiveSignals() { return reactiveSignals; }
}