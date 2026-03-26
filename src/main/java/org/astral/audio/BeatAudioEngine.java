package org.astral.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public class BeatAudioEngine {

    private static final int FFT_SIZE = 1024;
    private static final int NUM_BARS = 16;
    private static final List<float[]> fftFrames = new ArrayList<>();

    private static final float[] smoothedBars = new float[NUM_BARS];
    private static final float[] bandMaxes = new float[NUM_BARS];
    private static final float[] previousFrame = new float[NUM_BARS];
    private static final float[] beatIntensity = new float[NUM_BARS];
    private static final long[] lastEventTime = new long[NUM_BARS];

    private static final long COOLDOWN_BASS = 300;
    private static final long COOLDOWN_SNARE = 200;

    private static long device, context;
    private static int bufferId = 0, sourceId = 0;
    private static int sampleRate;

    private static final float ATTACK = 0.70f;
    private static final float DECAY = 0.15f;
    private static long lastHitComboTimeMs = 0;

    private static boolean engineRunning = false;
    private static boolean isLooping = false;
    private static boolean shuttingDown = false; // Bandera para evitar doble apagado

    private static CountDownLatch exitLatch;
    private static ScheduledExecutorService executor;

    static {
        System.setProperty("org.lwjgl.util.Debug", "false");
        System.setProperty("org.lwjgl.util.NoChecks", "true");
    }

    public static void startEngine() {
        if (engineRunning) return;

        initOpenAL();
        engineRunning = true;
        exitLatch = new CountDownLatch(1);

        // NÚCLEO MÁGICO: Hook de apagado forzado. Si el juego crashea o detienes el IDE, esto se ejecuta SI o SI.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\u001B[31m[BeatEngine] Parada de emergencia detectada. Limpiando...\u001B[0m");
            shutdown();
        }));

        WebVisualizer.start();

        runProcessingLoop();
    }

    public static void playNewSong(String path) {
        if (!engineRunning) return;

        if (sourceId != 0) {
            alSourceStop(sourceId);
            alDeleteSources(sourceId);
            alDeleteBuffers(bufferId);
        }

        fftFrames.clear();
        Arrays.fill(smoothedBars, 0);
        Arrays.fill(beatIntensity, 0);
        Arrays.fill(previousFrame, 0);
        Arrays.fill(bandMaxes, 1.5f);

        loadAndAnalyzeAudio(path);

        alSourcei(sourceId, AL_LOOPING, isLooping ? AL_TRUE : AL_FALSE);
        alSourcePlay(sourceId);
    }

    public static void pauseSong() {
        if (sourceId != 0 && alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING) {
            alSourcePause(sourceId);
        }
    }

    public static void resumeSong() {
        if (sourceId != 0 && alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PAUSED) {
            alSourcePlay(sourceId);
        }
    }

    public static void stopSong() {
        if (sourceId != 0) {
            alSourceStop(sourceId);
            Arrays.fill(smoothedBars, 0);
            System.arraycopy(smoothedBars, 0, RhythmAPI.currentBars, 0, NUM_BARS);
        }
    }

    public static void setLooping(boolean loop) {
        isLooping = loop;
        if (sourceId != 0) {
            alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
        }
    }

    public static void setVolume(float volume) {
        if (sourceId != 0) {
            alSourcef(sourceId, AL_GAIN, Math.max(0.0f, volume));
        }
    }

    public static void shutdown() {
        if (shuttingDown) return; // Si ya se está apagando, salimos
        shuttingDown = true;
        engineRunning = false;

        System.out.println("[BeatEngine] Cerrando Motor...");

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow(); // Frena el bucle en seco
        }
        cleanup();
        if (exitLatch != null) exitLatch.countDown();
    }

    public static void waitForExit() {
        try {
            if (exitLatch != null) exitLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void initOpenAL() {
        device = alcOpenDevice((ByteBuffer) null);
        ALCCapabilities caps = ALC.createCapabilities(device);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            context = alcCreateContext(device, stack.ints(0));
        }
        alcMakeContextCurrent(context);
        AL.createCapabilities(caps);
    }

    private static void loadAndAnalyzeAudio(String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            long decoder = STBVorbis.stb_vorbis_open_filename(path, error, null);
            if (decoder == 0) throw new RuntimeException("Error: archivo no encontrado: " + path);

            STBVorbisInfo info = STBVorbisInfo.malloc(stack);
            STBVorbis.stb_vorbis_get_info(decoder, info);
            int channels = info.channels();
            sampleRate = info.sample_rate();

            int totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder) * channels;
            ShortBuffer pcm = BufferUtils.createShortBuffer(totalSamples);
            STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            STBVorbis.stb_vorbis_close(decoder);

            bufferId = alGenBuffers();
            alBufferData(bufferId, (channels == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm, sampleRate);
            sourceId = alGenSources();
            alSourcei(sourceId, AL_BUFFER, bufferId);

            int step = FFT_SIZE / 2;
            for (int i = 0; i < pcm.capacity() - (FFT_SIZE * channels); i += (step * channels)) {
                float[] samples = new float[FFT_SIZE];
                for (int j = 0; j < FFT_SIZE; j++) {
                    int idx = i + (j * channels);
                    float s = (channels == 2) ? (pcm.get(idx) + pcm.get(idx + 1)) / 65536f : pcm.get(idx) / 32768f;
                    samples[j] = s * (float) (0.5 * (1.0 - Math.cos(2 * Math.PI * j / (FFT_SIZE - 1))));
                }
                float[] frame = computeFFT(samples);
                fftFrames.add(frame);
                for (int b = 0; b < NUM_BARS; b++) {
                    if (frame[b] > bandMaxes[b]) bandMaxes[b] = frame[b];
                }
            }
        }
    }

    private static float[] computeFFT(float[] input) {
        int n = input.length;
        float[] real = new float[n], imag = new float[n];
        System.arraycopy(input, 0, real, 0, n);

        for (int i = 0, j = 0; i < n; i++) {
            if (i < j) { float t = real[i]; real[i] = real[j]; real[j] = t; }
            int m = n >> 1;
            while (m >= 1 && j >= m) { j -= m; m >>= 1; }
            j += m;
        }
        for (int k = 1; k < n; k <<= 1) {
            float angle = (float) (-Math.PI / k);
            for (int i = 0; i < n; i += (k << 1)) {
                for (int j = 0; j < k; j++) {
                    float c = (float) Math.cos(angle * j), s = (float) Math.sin(angle * j);
                    float tr = c * real[i + j + k] - s * imag[i + j + k];
                    float ti = s * real[i + j + k] + c * imag[i + j + k];
                    real[i + j + k] = real[i + j] - tr; imag[i + j + k] = imag[i + j] - ti;
                    real[i + j] += tr; imag[i + j] += ti;
                }
            }
        }

        float[] bars = new float[NUM_BARS];
        int[] boundaries = {1, 2, 4, 6, 9, 14, 20, 28, 40, 56, 80, 112, 160, 224, 320, 448, 511};

        for (int i = 0; i < NUM_BARS; i++) {
            int start = boundaries[i];
            int end = boundaries[i+1];
            float sum = 0;
            for (int j = start; j < end; j++) {
                sum += (float) Math.sqrt(real[j] * real[j] + imag[j] * imag[j]);
            }
            bars[i] = sum / (end - start);
        }
        return bars;
    }

    private static void runProcessingLoop() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {

            if (!engineRunning) return;
            if (sourceId == 0) return;

            int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
            float offset = alGetSourcef(sourceId, AL11.AL_SEC_OFFSET);

            if (state == AL_PLAYING) {
                RhythmAPI.isPaused = false;
                int frameIdx = (int) (offset * sampleRate / ((float) FFT_SIZE / 2));
                long currentTime = System.currentTimeMillis();

                if (frameIdx < fftFrames.size()) {
                    float[] currentFrame = fftFrames.get(frameIdx);
                    float totalFrameEnergy = 0.0f;

                    for (int i = 0; i < NUM_BARS; i++) {
                        float rawValue = currentFrame[i] / (bandMaxes[i] * 0.95f);
                        long currentCooldown = (i < 3) ? COOLDOWN_BASS : ((i < 8) ? COOLDOWN_SNARE : 0);
                        boolean isHit = rawValue > previousFrame[i] * 1.4f && rawValue > 0.25f;

                        if (isHit && (currentTime - lastEventTime[i] >= currentCooldown)) {
                            beatIntensity[i] = 1.0f;
                            lastEventTime[i] = currentTime;
                            if (i == 0 || i == 1) RhythmAPI.triggerBass();
                            else if (i == 5) RhythmAPI.triggerSnare();
                        } else {
                            beatIntensity[i] -= 0.08f;
                            if (beatIntensity[i] < 0) beatIntensity[i] = 0;
                        }

                        previousFrame[i] = rawValue;
                        float speed = (rawValue > smoothedBars[i]) ? ATTACK : DECAY;
                        smoothedBars[i] += (rawValue - smoothedBars[i]) * speed;
                        totalFrameEnergy += smoothedBars[i];
                    }

                    System.arraycopy(smoothedBars, 0, RhythmAPI.currentBars, 0, NUM_BARS);

                    RhythmAPI.globalEnergy = totalFrameEnergy / NUM_BARS;
                    float currentHatIntensity = Math.max(smoothedBars[10], smoothedBars[11]);
                    RhythmAPI.particleSpeedMultiplier = 1.0f + (currentHatIntensity * 2.5f);

                    if (beatIntensity[1] == 1.0f || beatIntensity[5] == 1.0f) {
                        RhythmAPI.hitCombo++;
                        lastHitComboTimeMs = currentTime;
                    } else if (currentTime - lastHitComboTimeMs > 1500) {
                        RhythmAPI.hitCombo = 0;
                    }

                    float baseTimeSpeed = 0.0005f;
                    float dynamicSpeed = (RhythmAPI.globalEnergy > 0.4f) ? (RhythmAPI.globalEnergy - 0.4f) * 0.04f : 0.0f;
                    float timeJump = ((beatIntensity[0] > 0.9f || beatIntensity[1] > 0.9f) && RhythmAPI.globalEnergy > 0.5f) ? 0.02f : 0.0f;

                    RhythmAPI.gameTimeOfDay += baseTimeSpeed + dynamicSpeed + timeJump;
                    if (RhythmAPI.gameTimeOfDay >= 24.0f) RhythmAPI.gameTimeOfDay -= 24.0f;
                }

                WebVisualizer.update(smoothedBars, beatIntensity, RhythmAPI.globalEnergy, RhythmAPI.hitCombo, RhythmAPI.particleSpeedMultiplier, false);

            } else if (state == AL_PAUSED) {
                RhythmAPI.isPaused = true;
                WebVisualizer.update(smoothedBars, beatIntensity, RhythmAPI.globalEnergy, RhythmAPI.hitCombo, RhythmAPI.particleSpeedMultiplier, true);

            } else if (state == AL_STOPPED) {
                RhythmAPI.globalEnergy = 0;
                WebVisualizer.update(smoothedBars, beatIntensity, RhythmAPI.globalEnergy, RhythmAPI.hitCombo, RhythmAPI.particleSpeedMultiplier, true);
            }

        }, 0, 16, TimeUnit.MILLISECONDS);
    }

    private static void cleanup() {
        System.out.println("[BeatEngine] Destruyendo sesión de Audio y Web...");
        if (sourceId != 0) {
            alSourceStop(sourceId);
            alDeleteSources(sourceId);
            alDeleteBuffers(bufferId);
        }
        WebVisualizer.stop();
        if (context != 0) alcDestroyContext(context);
        if (device != 0) alcCloseDevice(device);
        System.out.println("\u001B[32m[BeatEngine] Apagado limpio y completado.\u001B[0m");
    }
}