package org.astral.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.jtransforms.fft.FloatFFT_1D;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.concurrent.*;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public class OpenALAudioEngine {

    private static final int FFT_SIZE = 2048;
    public static final int NUM_BARS = 32;

    private static final FloatFFT_1D fft = new FloatFFT_1D(FFT_SIZE);

    private static ShortBuffer pcmData;
    private static int numChannels;

    private static final float[] smoothedBars = new float[NUM_BARS];
    private static final float[] bandMaxes = new float[NUM_BARS];
    private static final float[] previousFrame = new float[NUM_BARS];
    private static final float[] beatIntensity = new float[NUM_BARS];
    private static final long[] lastEventTime = new long[NUM_BARS];

    private static final float[] globalBandMax = new float[NUM_BARS];
    private static final float[] globalBandAvg = new float[NUM_BARS];

    private static long device, context;
    private static int bufferId = 0, sourceId = 0;
    private static int sampleRate;

    private static final float ATTACK = 0.75f;
    private static final float DECAY = 0.20f;
    private static long lastHitComboTimeMs = 0;

    private static boolean engineRunning = false;
    private static boolean isLooping = false;
    private static boolean shuttingDown = false;

    private static CountDownLatch exitLatch;
    private static ScheduledExecutorService executor;
    private static WebVisualizer webVisualizer;

    private static float totalDurationSeconds = 0f;

    private static final int[] BOUNDARIES = new int[NUM_BARS + 1];

    static {
        System.setProperty("org.lwjgl.util.Debug", "false");
        System.setProperty("org.lwjgl.util.NoChecks", "true");

        BOUNDARIES[0] = 1;
        double maxBin = FFT_SIZE / 2.0;
        double logMin = Math.log10(1);
        double logMax = Math.log10(maxBin);

        for (int i = 1; i <= NUM_BARS; i++) {
            double val = logMin + (logMax - logMin) * ((double) i / NUM_BARS);
            BOUNDARIES[i] = (int) Math.round(Math.pow(10, val));
            if (BOUNDARIES[i] <= BOUNDARIES[i - 1]) {
                BOUNDARIES[i] = BOUNDARIES[i - 1] + 1;
            }
        }
        if (BOUNDARIES[NUM_BARS] > FFT_SIZE / 2) {
            BOUNDARIES[NUM_BARS] = FFT_SIZE / 2;
        }
    }

    // --- NUEVO MÉTODO PARA ELIMINAR DUPLICACIÓN ---
    private static float[] extractWindowedSamples(int startIdx) {
        float[] samples = new float[FFT_SIZE];
        for (int j = 0; j < FFT_SIZE; j++) {
            int idx = startIdx + (j * numChannels);
            float s = (numChannels == 2)
                    ? (pcmData.get(idx) + pcmData.get(idx + 1)) / 65536f
                    : pcmData.get(idx) / 32768f;

            // Aplicar Ventana de Hann para evitar artefactos en el FFT
            samples[j] = s * (float) (0.5 * (1.0 - Math.cos(2 * Math.PI * j / (FFT_SIZE - 1))));
        }
        return samples;
    }

    public static void startEngine() {
        if (engineRunning) return;
        initOpenAL();
        engineRunning = true;
        exitLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(OpenALAudioEngine::shutdown));
        webVisualizer = new WebVisualizer("OpenAL (AudioSpectrum)", 8080, OpenALAudioEngine::setVolume);
        webVisualizer.start();
        runProcessingLoop();
    }

    public static void playNewSong(String path, Runnable onPlaybackStart) {
        if (!engineRunning) return;
        if (sourceId != 0) {
            alSourceStop(sourceId);
            alDeleteSources(sourceId);
            alDeleteBuffers(bufferId);
        }
        Arrays.fill(smoothedBars, 0);
        Arrays.fill(beatIntensity, 0);
        Arrays.fill(previousFrame, 0);
        Arrays.fill(bandMaxes, 1.0f);
        loadAndAnalyzeAudio(path);
        alSourcei(sourceId, AL_LOOPING, isLooping ? AL_TRUE : AL_FALSE);
        alSourcePlay(sourceId);
        if (onPlaybackStart != null) onPlaybackStart.run();
    }

    private static void loadAndAnalyzeAudio(String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            ByteBuffer pathBuffer = stack.UTF8(path);
            long decoder = STBVorbis.stb_vorbis_open_filename(pathBuffer, error, null);
            if (decoder == 0) throw new RuntimeException("Error: archivo no encontrado: " + path);

            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);
            numChannels = info.channels();
            sampleRate = info.sample_rate();

            int totalSamplesInStream = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
            totalDurationSeconds = (float) totalSamplesInStream / sampleRate;

            int totalSamples = totalSamplesInStream * numChannels;
            pcmData = BufferUtils.createShortBuffer(totalSamples);
            STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, numChannels, pcmData);
            STBVorbis.stb_vorbis_close(decoder);

            bufferId = alGenBuffers();
            alBufferData(bufferId, (numChannels == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcmData, sampleRate);
            sourceId = alGenSources();
            alSourcei(sourceId, AL_BUFFER, bufferId);

            preScanAudio();
        }
    }

    private static void preScanAudio() {
        System.out.println("\u001B[33m[BeatEngine] Pre-escaneando canción (Análisis Multi-Banda)...\u001B[0m");
        long start = System.currentTimeMillis();
        Arrays.fill(globalBandMax, 0f);
        float[] sumEnergy = new float[NUM_BARS];
        int frames = 0;

        for (int idx = 0; idx + (FFT_SIZE * numChannels) < pcmData.capacity(); idx += FFT_SIZE * numChannels) {
            float[] frame = computeFFT(extractWindowedSamples(idx)); // Usando método extraído
            for (int i = 0; i < NUM_BARS; i++) {
                if (frame[i] > globalBandMax[i]) globalBandMax[i] = frame[i];
                sumEnergy[i] += frame[i];
            }
            frames++;
        }

        for (int i = 0; i < NUM_BARS; i++) {
            globalBandAvg[i] = sumEnergy[i] / Math.max(1, frames);
        }
        System.out.println("\u001B[32m[BeatEngine] Escaneo completado en " + (System.currentTimeMillis() - start) + "ms.\u001B[0m");
    }

    private static float[] computeFFT(float[] samples) {
        fft.realForward(samples);
        float[] magnitudes = new float[FFT_SIZE / 2];
        magnitudes[0] = Math.abs(samples[0]);
        for (int k = 1; k < FFT_SIZE / 2; k++) {
            float re = samples[2 * k];
            float im = samples[2 * k + 1];
            magnitudes[k] = (float) Math.sqrt(re * re + im * im);
        }
        float[] bars = new float[NUM_BARS];
        for (int i = 0; i < NUM_BARS; i++) {
            int start = BOUNDARIES[i];
            int end = BOUNDARIES[i + 1];
            float sum = 0;
            for (int j = start; j < end; j++) sum += magnitudes[j];
            bars[i] = sum / (end - start);
        }
        return bars;
    }

    private static void runProcessingLoop() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if (!engineRunning || sourceId == 0 || pcmData == null) return;

            int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
            float offset = alGetSourcef(sourceId, AL11.AL_SEC_OFFSET);

            if (state == AL_PLAYING) {
                RhythmAPI.isPaused = false;
                long currentTime = System.currentTimeMillis();
                int currentSampleIdx = (int) (offset * sampleRate) * numChannels;

                if (currentSampleIdx >= 0 && currentSampleIdx + (FFT_SIZE * numChannels) < pcmData.capacity()) {
                    float[] currentFrame = computeFFT(extractWindowedSamples(currentSampleIdx)); // Usando método extraído
                    float totalFrameEnergy = 0.0f;

                    for (int i = 0; i < NUM_BARS; i++) {
                        if (currentFrame[i] > bandMaxes[i]) bandMaxes[i] = currentFrame[i];
                        else bandMaxes[i] *= 0.992f;

                        float rawValue = currentFrame[i] / Math.max(bandMaxes[i], 0.1f);
                        long currentCooldown = (i < 5) ? 250 : ((i < 17) ? 150 : 100);
                        boolean isHit = isIsHit(i, rawValue, currentFrame);

                        if (isHit && (currentTime - lastEventTime[i] >= currentCooldown)) {
                            beatIntensity[i] = 1.0f;
                            lastEventTime[i] = currentTime;
                            if (i < 4) RhythmAPI.triggerBass();
                            else if (i >= 8 && i <= 12) RhythmAPI.triggerSnare();
                        } else {
                            beatIntensity[i] = Math.max(0, beatIntensity[i] - 0.08f);
                        }

                        previousFrame[i] = rawValue;
                        float speed = (rawValue > smoothedBars[i]) ? ATTACK : DECAY;
                        smoothedBars[i] += (rawValue - smoothedBars[i]) * speed;
                        totalFrameEnergy += smoothedBars[i];
                    }

                    System.arraycopy(smoothedBars, 0, RhythmAPI.currentBars, 0, Math.min(NUM_BARS, RhythmAPI.currentBars.length));
                    RhythmAPI.globalEnergy = totalFrameEnergy / NUM_BARS;

                    float maxKickInt = 0, maxSnareInt = 0, maxHatInt = 0;
                    for (int k = 0; k < 5; k++) maxKickInt = Math.max(maxKickInt, beatIntensity[k]);
                    for (int s = 5; s < 17; s++) maxSnareInt = Math.max(maxSnareInt, beatIntensity[s]);
                    for (int h = 17; h < 32; h++) maxHatInt = Math.max(maxHatInt, beatIntensity[h]);

                    RhythmAPI.kickIntensity = maxKickInt;
                    RhythmAPI.snareIntensity = maxSnareInt;
                    RhythmAPI.hatIntensity = maxHatInt;


                    if (maxHatInt > 0.85f && (currentTime - lastEventTime[17] >= 150)) {
                        RhythmAPI.triggerHat();
                        lastEventTime[17] = currentTime;
                    }

                    float maxHit = Math.max(maxKickInt, Math.max(maxSnareInt, maxHatInt));
                    RhythmAPI.particleSpeedMultiplier = 1.0f + (RhythmAPI.globalEnergy * 0.5f) + (maxHit * 3.5f);

                    if (beatIntensity[1] == 1.0f || beatIntensity[10] == 1.0f) {
                        RhythmAPI.hitCombo++;
                        lastHitComboTimeMs = currentTime;
                    } else if (currentTime - lastHitComboTimeMs > 1500) {
                        RhythmAPI.hitCombo = 0;
                    }
                    RhythmAPI.gameTimeOfDay = (RhythmAPI.gameTimeOfDay + 0.0005f + (RhythmAPI.globalEnergy * 0.02f)) % 24.0f;
                }
                if (webVisualizer != null) webVisualizer.update(smoothedBars, beatIntensity, RhythmAPI.globalEnergy, RhythmAPI.hitCombo, RhythmAPI.particleSpeedMultiplier, false, offset, totalDurationSeconds);
            } else {
                RhythmAPI.isPaused = (state == AL_PAUSED);
                if (webVisualizer != null) webVisualizer.update(smoothedBars, beatIntensity, RhythmAPI.globalEnergy, RhythmAPI.hitCombo, RhythmAPI.particleSpeedMultiplier, true, offset, totalDurationSeconds);
            }
        }, 0, 16, TimeUnit.MILLISECONDS);
    }

    private static boolean isIsHit(int i, float rawValue, float[] currentFrame) {
        float jumpRequired = (i < 5) ? 1.50f : ((i < 17) ? 1.35f : 1.25f);
        float minThreshold = (i < 5) ? 0.50f : 0.25f;

        float absoluteFloor = globalBandMax[i] * ((i < 5) ? 0.30f : 0.20f);
        float avgFloor = globalBandAvg[i] * ((i < 5) ? 1.50f : 1.20f);

        boolean isHit;
        isHit = (rawValue > previousFrame[i] * jumpRequired)
                && (rawValue > minThreshold)
                && (currentFrame[i] > absoluteFloor)
                && (currentFrame[i] > avgFloor);
        return isHit;
    }

    // Métodos de control y limpieza (se mantienen igual)...
    public static void setLooping(boolean loop) { isLooping = loop; if (sourceId != 0) alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE); }
    public static void pauseSong() { if (sourceId != 0) alSourcePause(sourceId); }
    public static void resumeSong() { if (sourceId != 0) alSourcePlay(sourceId); }
    public static void stopSong() { if (sourceId != 0) alSourceStop(sourceId); }
    public static void setVolume(float volume) { if (sourceId != 0) alSourcef(sourceId, AL_GAIN, Math.max(0.0f, volume)); }
    public static void waitForExit() { try { if (exitLatch != null) exitLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    public static void shutdown() { if (shuttingDown) return; shuttingDown = true; engineRunning = false; if (executor != null) executor.shutdownNow(); cleanup(); if (exitLatch != null) exitLatch.countDown(); }

    private static void initOpenAL() {
        device = alcOpenDevice((ByteBuffer) null);
        ALCCapabilities caps = ALC.createCapabilities(device);
        try (MemoryStack stack = MemoryStack.stackPush()) { context = alcCreateContext(device, stack.ints(0)); }
        alcMakeContextCurrent(context);
        AL.createCapabilities(caps);
    }

    private static void cleanup() {
        if (sourceId != 0) { alSourceStop(sourceId); alDeleteSources(sourceId); alDeleteBuffers(bufferId); }
        if (webVisualizer != null) webVisualizer.stop();
        if (context != 0) alcDestroyContext(context);
        if (device != 0) alcCloseDevice(device);
    }
}