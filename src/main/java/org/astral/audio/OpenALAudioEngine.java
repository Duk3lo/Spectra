package org.astral.audio;

import org.jetbrains.annotations.NotNull;
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

    // ==== Configuración Activa y Sincronización ====
    private static volatile AudioEngineConfig config = null;
    private static final Object engineLock = new Object(); // Lock para evitar crasheos al recargar en vivo

    // ==== Variables dependientes de la configuración ====
    private static FloatFFT_1D fft;
    private static float[] smoothedBars;
    private static float[] bandMaxes;
    private static float[] previousFrame;
    private static float[] beatIntensity;
    private static long[] lastEventTime;
    private static float[] globalBandMax;
    private static float[] globalBandAvg;
    private static int[] BOUNDARIES;

    // Rangos dinámicos calculados (Bajos, Cajas, Platillos)
    private static int bassEndIndex, snareEndIndex;

    // ==== Variables de OpenAL ====
    private static ShortBuffer pcmData;
    private static int numChannels, sampleRate;
    private static long device, context;
    private static int bufferId = 0, sourceId = 0;
    private static float totalDurationSeconds = 0f;

    // ==== Estado del Engine ====
    private static long lastHitComboTimeMs = 0;
    private static boolean engineRunning = false;
    private static boolean isLooping = false;
    private static boolean shuttingDown = false;

    private static CountDownLatch exitLatch;
    private static ScheduledExecutorService executor;
    private static ScheduledFuture<?> loopTask;

    private static final WebVisualizer webVisualizer = Main.getWebVisualizer();

    static {
        System.setProperty("org.lwjgl.util.Debug", "false");
        System.setProperty("org.lwjgl.util.NoChecks", "true");
        applyConfiguration(new AudioEngineConfig(), false);
    }

    private static void initOpenAL() {
        device = alcOpenDevice((ByteBuffer) null);
        ALCCapabilities caps = ALC.createCapabilities(device);
        try (MemoryStack stack = MemoryStack.stackPush()) { context = alcCreateContext(device, stack.ints(0)); }
        alcMakeContextCurrent(context);
        AL.createCapabilities(caps);
    }

    public static void reloadConfiguration(AudioEngineConfig newConfig) {
        System.out.println("\u001B[36m[BeatEngine] Aplicando nueva configuración en vivo...\u001B[0m");
        applyConfiguration(newConfig, true);
    }

    private static void applyConfiguration(AudioEngineConfig newConfig, boolean isReload) {
        synchronized (engineLock) {
            boolean rebuildArrays = (config == null) ||
                    (smoothedBars == null) ||
                    (config.getFftSize() != newConfig.getFftSize()) ||
                    (config.getNumBars() != newConfig.getNumBars());
            config = newConfig;

            if (rebuildArrays) {
                int fftSize = config.getFftSize();
                int numBars = config.getNumBars();

                fft = new FloatFFT_1D(fftSize);
                smoothedBars = new float[numBars];
                bandMaxes = new float[numBars];
                previousFrame = new float[numBars];
                beatIntensity = new float[numBars];
                lastEventTime = new long[numBars];
                globalBandMax = new float[numBars];
                globalBandAvg = new float[numBars];
                BOUNDARIES = new int[numBars + 1];

                Arrays.fill(bandMaxes, 1.0f);

                // Calcular límites logarítmicos
                BOUNDARIES[0] = 1;
                double maxBin = fftSize / 2.0;
                double logMin = Math.log10(1);
                double logMax = Math.log10(maxBin);

                for (int i = 1; i <= numBars; i++) {
                    double val = logMin + (logMax - logMin) * ((double) i / numBars);
                    BOUNDARIES[i] = (int) Math.round(Math.pow(10, val));
                    if (BOUNDARIES[i] <= BOUNDARIES[i - 1]) BOUNDARIES[i] = BOUNDARIES[i - 1] + 1;
                }
                if (BOUNDARIES[numBars] > fftSize / 2) BOUNDARIES[numBars] = fftSize / 2;

                // Calcular dinámicamente qué barra es Bajo, Caja o Platillo basado en %
                bassEndIndex = Math.max(1, (int)(numBars * 0.15f));  // 15% de barras
                snareEndIndex = Math.max(bassEndIndex + 1, (int)(numBars * 0.50f)); // Hasta el 50%

                // Si estamos recargando y hay una canción sonando, debemos re-escanear
                if (isReload && pcmData != null && engineRunning) {
                    preScanAudio();
                }
            }

            // Si la tasa de actualización cambió, reiniciamos el loop
            if (isReload && engineRunning) {
                if (loopTask != null) loopTask.cancel(false);
                startProcessingTask();
            }
        }
    }

    public static void startEngine() {
        if (engineRunning) return;
        initOpenAL();
        engineRunning = true;
        exitLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(OpenALAudioEngine::shutdown));

        executor = Executors.newSingleThreadScheduledExecutor();
        startProcessingTask();
    }

    private static void startProcessingTask() {
        loopTask = executor.scheduleAtFixedRate(() -> {
            if (!engineRunning || sourceId == 0 || pcmData == null) return;

            synchronized (engineLock) {
                processAudioFrame();
            }
        }, 0, config.getUpdateRateMs(), TimeUnit.MILLISECONDS);
    }

    private static void processAudioFrame() {
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        float offset = alGetSourcef(sourceId, AL11.AL_SEC_OFFSET);

        if (state == AL_PLAYING) {
            RhythmAPI.isPaused = false;
            long currentTime = System.currentTimeMillis();
            int currentSampleIdx = (int) (offset * sampleRate) * numChannels;

            int fftSize = config.getFftSize();
            int numBars = config.getNumBars();

            if (currentSampleIdx >= 0 && currentSampleIdx + (fftSize * numChannels) < pcmData.capacity()) {
                float[] currentFrame = computeFFT(extractWindowedSamples(currentSampleIdx, fftSize));
                float totalFrameEnergy = 0.0f;

                for (int i = 0; i < numBars; i++) {
                    if (currentFrame[i] > bandMaxes[i]) bandMaxes[i] = currentFrame[i];
                    else bandMaxes[i] *= 0.992f;

                    float rawValue = currentFrame[i] / Math.max(bandMaxes[i], 0.1f);

                    long currentCooldown = (i < bassEndIndex) ? config.getBassCooldownMs() :
                            ((i < snareEndIndex) ? config.getSnareCooldownMs() : config.getHatCooldownMs());

                    boolean isHit = checkIsHit(i, rawValue, currentFrame);

                    if (isHit && (currentTime - lastEventTime[i] >= currentCooldown)) {
                        beatIntensity[i] = 1.0f;
                        lastEventTime[i] = currentTime;
                        if (i < bassEndIndex) RhythmAPI.triggerBass();
                        else if (i < snareEndIndex) RhythmAPI.triggerSnare();
                    } else {
                        beatIntensity[i] = Math.max(0, beatIntensity[i] - 0.08f);
                    }

                    previousFrame[i] = rawValue;
                    float speed = (rawValue > smoothedBars[i]) ? config.getAttack() : config.getDecay();
                    smoothedBars[i] += (rawValue - smoothedBars[i]) * speed;
                    totalFrameEnergy += smoothedBars[i];
                }

                // Asegurar que RhythmAPI.currentBars tenga el tamaño correcto si es que lo usas
                if(RhythmAPI.currentBars == null || RhythmAPI.currentBars.length != numBars) {
                    RhythmAPI.currentBars = new float[numBars];
                }
                System.arraycopy(smoothedBars, 0, RhythmAPI.currentBars, 0, numBars);
                RhythmAPI.globalEnergy = totalFrameEnergy / numBars;

                // Intensidades dinámicas
                float maxKickInt = 0, maxSnareInt = 0, maxHatInt = 0;
                for (int k = 0; k < bassEndIndex; k++) maxKickInt = Math.max(maxKickInt, beatIntensity[k]);
                for (int s = bassEndIndex; s < snareEndIndex; s++) maxSnareInt = Math.max(maxSnareInt, beatIntensity[s]);
                for (int h = snareEndIndex; h < numBars; h++) maxHatInt = Math.max(maxHatInt, beatIntensity[h]);

                RhythmAPI.kickIntensity = maxKickInt;
                RhythmAPI.snareIntensity = maxSnareInt;
                RhythmAPI.hatIntensity = maxHatInt;

                if (maxHatInt > 0.85f && (currentTime - lastEventTime[snareEndIndex] >= config.getHatCooldownMs())) {
                    RhythmAPI.triggerHat();
                    lastEventTime[snareEndIndex] = currentTime;
                }

                float maxHit = Math.max(maxKickInt, Math.max(maxSnareInt, maxHatInt));
                RhythmAPI.particleSpeedMultiplier = 1.0f + (RhythmAPI.globalEnergy * 0.5f) + (maxHit * 3.5f);

                if (beatIntensity[1] == 1.0f || beatIntensity[bassEndIndex + 1] == 1.0f) {
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
    }

    private static boolean checkIsHit(int i, float rawValue, float[] currentFrame) {
        float jumpRequired = (i < bassEndIndex) ? config.getBassJumpThreshold() :
                ((i < snareEndIndex) ? config.getSnareJumpThreshold() : config.getHatJumpThreshold());

        float minThreshold = (i < bassEndIndex) ? 0.50f : 0.25f;
        float absoluteFloor = globalBandMax[i] * ((i < bassEndIndex) ? 0.30f : 0.20f);
        float avgFloor = globalBandAvg[i] * ((i < bassEndIndex) ? 1.50f : 1.20f);

        return (rawValue > previousFrame[i] * jumpRequired)
                && (rawValue > minThreshold)
                && (currentFrame[i] > absoluteFloor)
                && (currentFrame[i] > avgFloor);
    }

    private static float @NotNull [] extractWindowedSamples(int startIdx, int fftSize) {
        float[] samples = new float[fftSize];
        for (int j = 0; j < fftSize; j++) {
            int idx = startIdx + (j * numChannels);
            float s = (numChannels == 2)
                    ? (pcmData.get(idx) + pcmData.get(idx + 1)) / 65536f
                    : pcmData.get(idx) / 32768f;

            samples[j] = s * (float) (0.5 * (1.0 - Math.cos(2 * Math.PI * j / (fftSize - 1))));
        }
        return samples;
    }

    private static float @NotNull [] computeFFT(float[] samples) {
        int fftSize = config.getFftSize();
        int numBars = config.getNumBars();

        fft.realForward(samples);
        float[] magnitudes = new float[fftSize / 2];
        magnitudes[0] = Math.abs(samples[0]);

        for (int k = 1; k < fftSize / 2; k++) {
            float re = samples[2 * k];
            float im = samples[2 * k + 1];
            magnitudes[k] = (float) Math.sqrt(re * re + im * im);
        }

        float[] bars = new float[numBars];
        for (int i = 0; i < numBars; i++) {
            int start = BOUNDARIES[i];
            int end = BOUNDARIES[i + 1];
            float sum = 0;
            for (int j = start; j < end; j++) sum += magnitudes[j];
            bars[i] = sum / Math.max(1, (end - start));
        }
        return bars;
    }

    private static void preScanAudio() {
        System.out.println("\u001B[33m[BeatEngine] Pre-escaneando canción (Análisis Multi-Banda)...\u001B[0m");
        long start = System.currentTimeMillis();

        int numBars = config.getNumBars();
        int fftSize = config.getFftSize();

        Arrays.fill(globalBandMax, 0f);
        float[] sumEnergy = new float[numBars];
        int frames = 0;

        for (int idx = 0; idx + (fftSize * numChannels) < pcmData.capacity(); idx += fftSize * numChannels) {
            float[] frame = computeFFT(extractWindowedSamples(idx, fftSize));
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

    public static void playNewSong(String path, Runnable onPlaybackStart) {
        synchronized (engineLock) {
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

    // ========== METODOS DE CONTROL (PAUSE, STOP, ETC) MANTENIDOS IGUAL ==========
    public static void setLooping(boolean loop) {
        isLooping = loop;
        if (sourceId != 0) alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : AL_FALSE);
    }
    public static void pauseSong() { if (sourceId != 0) alSourcePause(sourceId); }
    public static void resumeSong() { if (sourceId != 0) alSourcePlay(sourceId); }
    public static void stopSong() { if (sourceId != 0) alSourceStop(sourceId); }
    public static void setVolume(float volume) { if (sourceId != 0) alSourcef(sourceId, AL_GAIN, Math.max(0.0f, volume)); }

    public static void waitForExit() {
        try { if (exitLatch != null) exitLatch.await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void shutdown() {
        if (shuttingDown) return;
        shuttingDown = true;
        engineRunning = false;
        if (executor != null) executor.shutdownNow();
        cleanup();
        if (exitLatch != null) exitLatch.countDown();
    }

    private static void cleanup() {
        if (sourceId != 0) {
            alSourceStop(sourceId);
            alDeleteSources(sourceId);
            alDeleteBuffers(bufferId);
        }
        if (webVisualizer != null) webVisualizer.stop();
        if (context != 0) alcDestroyContext(context);
        if (device != 0) alcCloseDevice(device);
    }
}