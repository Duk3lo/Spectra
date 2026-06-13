package org.astral.spectra.audio.engine;

import org.astral.spectra.audio.analysis.SpectrumAnalyzer;
import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.audio.decode.AudioDecoder;
import org.astral.spectra.audio.detection.BeatDetector;
import org.astral.spectra.audio.playback.OpenALContext;
import org.astral.spectra.audio.playback.OpenALPlayer;
import org.astral.spectra.audio.state.AudioBuffer;
import org.astral.spectra.config.AudioConfig;
import org.astral.spectra.logging.EngineLogger;
import org.astral.spectra.web.WebVisualizer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.lwjgl.openal.AL10.*;

public class AudioEngine {

    private AudioConfig config;
    private final OpenALPlayer player;
    private final EngineLogger logger;

    private AudioBuffer currentBuffer;
    private SpectrumAnalyzer analyzer;
    private BeatDetector beatDetector;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> loopTask;

    private volatile boolean engineRunning = false;
    private volatile boolean shuttingDown = false;

    private CountDownLatch exitLatch;

    private final Object engineLock = new Object();

    private WebVisualizer webVisualizer;

    private float liveVolume = 0.1f;

    private volatile boolean virtualPlayback = false;
    private volatile long virtualPlaybackStartMs = 0L;
    private volatile long virtualPlaybackOffsetMs = 0L;

    private float lastKnownOffset = 0.0f;

    public AudioEngine(AudioConfig config, EngineLogger logger) {
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.player = new OpenALPlayer();
        this.analyzer = new SpectrumAnalyzer(config);
    }

    public void setWebVisualizer(WebVisualizer webVisualizer) {
        this.webVisualizer = webVisualizer;
    }

    /**
     * true  → usar OpenAL  (OpenAL inicializado + forceWebAudio=false)
     * false → usar Web audio (browser reproduce el audio)
     */
    private boolean isUsingOpenAL() {
        return OpenALContext.isReady() && !config.getGeneral().isForceWebAudio();
    }

    public void start() {
        synchronized (engineLock) {
            if (engineRunning) return;

            OpenALContext.init();
            exitLatch = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            executor = Executors.newScheduledThreadPool(2);
            engineRunning = true;

            setVolume(config.getGeneral().getCurrentVolume());
            startProcessingTask();
        }
    }

    private void startProcessingTask() {
        if (executor == null) return;

        if (loopTask != null && !loopTask.isCancelled()) {
            loopTask.cancel(false);
        }

        loopTask = executor.scheduleAtFixedRate(
                this::processAudioFrame,
                0,
                config.getGeneral().getUpdateRateMs(),
                TimeUnit.MILLISECONDS
        );
    }

    public void reloadConfiguration(AudioConfig newConfig) {
        Objects.requireNonNull(newConfig, "newConfig");
        logger.info("\u001B[36m[AudioEngine] Applying new configuration live...\u001B[0m");

        boolean wasUsingOpenAL = isUsingOpenAL(); // Guardar el estado ANTES de aplicar cambios

        executor.execute(() -> {
            synchronized (engineLock) {
                this.config = newConfig;
                boolean nowUsingOpenAL = isUsingOpenAL();

                setVolume(config.getGeneral().getCurrentVolume());

                this.analyzer = new SpectrumAnalyzer(config);
                this.beatDetector = new BeatDetector(config, logger);

                if (currentBuffer != null) {
                    try {
                        beatDetector.preScan(currentBuffer, analyzer);

                        if (wasUsingOpenAL != nowUsingOpenAL) {
                            transferPlaybackState(nowUsingOpenAL);
                        }
                    } catch (Exception e) {
                        logger.error("Error during pre Scan in reload Configuration", e);
                    }
                }

                if (engineRunning) {
                    startProcessingTask();
                }
            }
        });
    }

    private void transferPlaybackState(boolean toOpenAL) {
        float currentOffset = lastKnownOffset;
        boolean isCurrentlyPaused = toOpenAL ? !virtualPlayback : (player.getState() != AL_PLAYING);

        if (webVisualizer != null) {
            webVisualizer.setAudioEnabled(!toOpenAL);
        }

        if (toOpenAL) {
            // Cambio: de Web Audio (Virtual) a OpenAL
            virtualPlayback = false;
            player.load(currentBuffer, liveVolume);
            player.setOffsetSeconds(currentOffset);
            if (!isCurrentlyPaused) {
                player.play(false);
            }
        } else {
            // Cambio: de OpenAL a Web Audio (Virtual)
            player.stop();
            virtualPlayback = !isCurrentlyPaused;
            virtualPlaybackStartMs = System.currentTimeMillis();
            virtualPlaybackOffsetMs = (long) (currentOffset * 1000L);
        }
    }

    // -------------------------------------------------------------------------
    // PLAYBACK PÚBLICO
    // -------------------------------------------------------------------------

    public float getVolume() {
        return this.liveVolume;
    }

    public void playSong(Path path) {
        playSong(path, 0, null);
    }

    public void playSong(Path path, Runnable onPlay) {
        playSong(path, 0, onPlay);
    }

    public void playSong(Path path, long startTimeMs, Runnable onPlay) {
        if (webVisualizer != null) {
            try {
                webVisualizer.setAudioTrack(Files.readAllBytes(path));
            } catch (Exception e) {
                logger.error("Failed to read raw file for web visualizer", e);
            }
        }
        startPlaybackAsync(() -> AudioDecoder.loadAudio(path), startTimeMs, onPlay);
    }

    public void playResource(String resourcePath) {
        playResource(resourcePath, null);
    }

    public void playResource(String resourcePath, Runnable onPlay) {
        playResource(resourcePath, 0, onPlay);
    }

    public void playResource(String resourcePath, long startTimeMs, Runnable onPlay) {
        if (webVisualizer != null) { // Quitamos la condición de !isUsingOpenAL()
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    webVisualizer.setAudioTrack(is.readAllBytes());
                }
            } catch (Exception e) {
                logger.error("Failed to read raw resource for web visualizer", e);
            }
        }
        startPlaybackAsync(() -> AudioDecoder.loadResource(resourcePath), startTimeMs, onPlay);
    }

    private void startPlaybackAsync(Supplier<AudioBuffer> loader, long startTimeMs, Runnable onPlay) {
        if (executor == null) {
            throw new IllegalStateException("Engine not started. Call start() first.");
        }

        executor.execute(() -> {
            try {
                AudioBuffer newBuffer = loader.get();
                BeatDetector newDetector = new BeatDetector(config, logger);
                newDetector.preScan(newBuffer, analyzer);

                synchronized (engineLock) {
                    if (!engineRunning) return;

                    // FIX: notificar al browser si debe reproducir audio o solo visualizar.
                    // Esto previene el doble audio (OpenAL + WebAudio al mismo tiempo).
                    if (webVisualizer != null) {
                        webVisualizer.setAudioEnabled(!isUsingOpenAL());
                    }

                    player.cleanup();
                    currentBuffer = newBuffer;
                    beatDetector = newDetector;

                    if (isUsingOpenAL()) {
                        virtualPlayback = false;

                        player.load(currentBuffer, liveVolume);
                        if (startTimeMs > 0) {
                            player.setOffsetSeconds(startTimeMs / 1000.0f);
                        }
                        player.play(false);

                    } else {
                        virtualPlayback = true;
                        virtualPlaybackOffsetMs = startTimeMs;
                        virtualPlaybackStartMs = System.currentTimeMillis();
                    }

                    AudioAPI.reset();
                    AudioAPI.setPlaying(true);
                    AudioAPI.setPaused(false);
                }

                if (onPlay != null) {
                    if (isUsingOpenAL()) {
                        waitForPlaybackAndRun(onPlay, 0);
                    } else {
                        onPlay.run();
                    }
                }

            } catch (Exception e) {
                logger.error("Error starting playback", e);
            }
        });
    }

    private void waitForPlaybackAndRun(Runnable onPlay, int attempts) {
        if (player.getState() == AL_PLAYING) {
            onPlay.run();
        } else if (attempts < 50) {
            executor.schedule(() -> waitForPlaybackAndRun(onPlay, attempts + 1), 1, TimeUnit.MILLISECONDS);
        } else {
            logger.warn("Playback started but AL_PLAYING state was not reached after 50ms.");
        }
    }

    public void seekTo(long timeMs) {
        synchronized (engineLock) {
            if (currentBuffer != null && engineRunning) {
                float targetSeconds = timeMs / 1000.0f;
                targetSeconds = Math.clamp(targetSeconds, 0.0f, currentBuffer.durationSeconds());

                if (isUsingOpenAL()) {
                    player.setOffsetSeconds(targetSeconds);
                } else if (virtualPlayback) {
                    virtualPlaybackOffsetMs = (long) (targetSeconds * 1000L);
                    virtualPlaybackStartMs = System.currentTimeMillis();
                }

                AudioAPI.setCurrentPositionSeconds(targetSeconds);
                logger.info("Seeked to: " + targetSeconds + "s");
            }
        }
    }

    // -------------------------------------------------------------------------
    // LOOP PRINCIPAL
    // -------------------------------------------------------------------------

    private void processAudioFrame() {
        if (!engineRunning) return;

        try {
            float[] bars;
            float[] intensities;
            java.util.Map<String, Float> features;
            float energy;
            int combo;
            float speed;
            boolean paused;
            float offset;
            float duration;

            synchronized (engineLock) {
                if (currentBuffer == null || beatDetector == null || analyzer == null) return;

                boolean useOpenAL = isUsingOpenAL();
                int state;

                duration = currentBuffer.durationSeconds();

                if (useOpenAL) {
                    state = player.getState();
                    offset = player.getOffsetSeconds();

                    if (state == AL_STOPPED && offset < 0.1f && lastKnownOffset >= duration - 0.5f) {
                        offset = duration;
                    }
                    lastKnownOffset = offset;

                } else if (virtualPlayback) {
                    offset = ((System.currentTimeMillis() - virtualPlaybackStartMs) / 1000.0f)
                            + (virtualPlaybackOffsetMs / 1000.0f);

                    if (offset >= duration) {
                        state = AL_STOPPED;
                        offset = duration;
                    } else {
                        state = AL_PLAYING;
                    }
                    lastKnownOffset = offset;

                } else {
                    state = AL_STOPPED;
                    offset = 0f;
                }

                if (state == AL_STOPPED && offset >= duration - 0.5f) {
                    logger.info("\u001B[32m[AudioEngine] Song finished. Clearing memory...\u001B[0m");
                    currentBuffer = null;
                    virtualPlayback = false;
                    virtualPlaybackStartMs = 0L;
                    virtualPlaybackOffsetMs = 0L;
                    lastKnownOffset = 0.0f;
                    AudioAPI.reset();

                    if (webVisualizer != null) {
                        int numBars = config.getVisualizer().getNumBars();
                        webVisualizer.update(new float[numBars], new float[numBars],
                                new java.util.HashMap<>(), 0f, 0, 1f, true, duration, duration, liveVolume);
                    }
                    return;
                }

                AudioAPI.setCurrentPositionSeconds(offset);
                AudioAPI.setTotalDurationSeconds(duration);

                if (state == AL_PLAYING) {
                    AudioAPI.setPaused(false);
                    AudioAPI.setPlaying(true);

                    int currentSampleIdx = (int) (offset * currentBuffer.sampleRate()) * currentBuffer.channels();
                    int fftSize = config.getVisualizer().getFftSize();

                    if (currentSampleIdx >= 0 &&
                            currentSampleIdx + (fftSize * currentBuffer.channels()) < currentBuffer.pcmData().capacity()) {

                        float[] samples = analyzer.extractSamples(currentBuffer, currentSampleIdx);
                        float[] currentFrame = analyzer.computeFFT(samples);
                        beatDetector.processFrame(currentFrame, System.currentTimeMillis());
                    }
                    paused = false;

                } else {
                    AudioAPI.setPaused(state == AL_PAUSED);
                    AudioAPI.setPlaying(false);
                    paused = true;
                    int numBars = config.getVisualizer().getNumBars();
                    float[] emptyFrame = new float[numBars];
                    beatDetector.processFrame(emptyFrame, System.currentTimeMillis());
                }

                bars = beatDetector.getSmoothedBars();
                intensities = beatDetector.getBeatIntensity();

                java.util.Map<String, Float> rawFeatures = beatDetector.getReactiveSignals();
                features = (rawFeatures != null)
                        ? new java.util.LinkedHashMap<>(rawFeatures)
                        : new java.util.LinkedHashMap<>();

                energy = AudioAPI.getGlobalEnergy();
                combo  = AudioAPI.getHitCombo();
                speed  = AudioAPI.getParticleSpeedMultiplier();
            }

            if (webVisualizer != null) {
                webVisualizer.update(bars, intensities, features, energy,
                        combo, speed, paused, offset, duration, liveVolume);
            }

        } catch (Throwable t) {
            assert t instanceof Exception;
            logger.error("\u001B[31m[AudioEngine] Error processing frame: " + t.getMessage() + "\u001B[0m", t);
        }
    }

    // -------------------------------------------------------------------------
    // CONTROLES
    // -------------------------------------------------------------------------

    public void setVolume(float volume) {
        this.liveVolume = Math.clamp(volume, 0.0f, 1.0f);

        synchronized (engineLock) {
            player.setVolume(liveVolume);
            AudioAPI.setVolume(liveVolume);
        }

        if (webVisualizer != null) {
            webVisualizer.sendVolumeUpdate(liveVolume);
        }
    }

    public void pauseSong() {
        synchronized (engineLock) {
            if (isUsingOpenAL()) {
                player.pause();
            } else if (virtualPlayback) {
                virtualPlaybackOffsetMs += (System.currentTimeMillis() - virtualPlaybackStartMs);
                virtualPlayback = false;
            }

            AudioAPI.setPaused(true);
            AudioAPI.setPlaying(false);
        }
    }

    public void resumeSong() {
        synchronized (engineLock) {
            if (isUsingOpenAL()) {
                player.play(false);
            } else {
                virtualPlayback = true;
                virtualPlaybackStartMs = System.currentTimeMillis();
            }

            AudioAPI.setPaused(false);
            AudioAPI.setPlaying(true);
        }
    }

    public void stopSong() {
        synchronized (engineLock) {
            if (isUsingOpenAL()) {
                player.stop();
            }

            virtualPlayback = false;
            virtualPlaybackStartMs = 0L;
            virtualPlaybackOffsetMs = 0L;
            lastKnownOffset = 0.0f;

            AudioAPI.setPlaying(false);
            AudioAPI.setPaused(true);
        }
    }

    public void waitForExit() {
        try {
            if (exitLatch != null) exitLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        synchronized (engineLock) {
            if (shuttingDown) return;
            shuttingDown = true;
            engineRunning = false;

            if (loopTask != null) loopTask.cancel(false);
            if (executor != null) executor.shutdownNow();

            player.cleanup();
            OpenALContext.destroy();

            if (webVisualizer != null) webVisualizer.stop();
            if (exitLatch != null) exitLatch.countDown();
        }
    }

    public AudioConfig getConfig() {
        return this.config;
    }
}