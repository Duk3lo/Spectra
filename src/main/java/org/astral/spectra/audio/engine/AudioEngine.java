package org.astral.spectra.audio.engine;

import org.astral.spectra.web.WebVisualizer;
import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.audio.analysis.SpectrumAnalyzer;
import org.astral.spectra.config.AudioConfig;
import org.astral.spectra.audio.decode.AudioDecoder;
import org.astral.spectra.audio.playback.OpenALContext;
import org.astral.spectra.audio.playback.OpenALPlayer;
import org.astral.spectra.audio.state.AudioBuffer;
import org.astral.spectra.audio.detection.BeatDetector;
import org.astral.spectra.logging.EngineLogger;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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

    public AudioEngine(AudioConfig config, EngineLogger logger) {
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.player = new OpenALPlayer();
        this.analyzer = new SpectrumAnalyzer(config);
    }

    public void setWebVisualizer(WebVisualizer webVisualizer) {
        this.webVisualizer = webVisualizer;
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

        executor.execute(() -> {
            synchronized (engineLock) {
                this.config = newConfig;

                setVolume(config.getGeneral().getCurrentVolume());

                this.analyzer = new SpectrumAnalyzer(config);
                this.beatDetector = new BeatDetector(config, logger);

                if (currentBuffer != null) {
                    try {
                        beatDetector.preScan(currentBuffer, analyzer);
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

    public void playSong(Path path) {
        playSong(path, 0, null);
    }

    public void playSong(Path path, Runnable onPlay) {
        playSong(path, 0, onPlay);
    }

    public void playSong(Path path, long startTimeMs, Runnable onPlay) {
        startPlaybackAsync(() -> AudioDecoder.loadAudio(path), startTimeMs, onPlay);
    }

    public void playResource(String resourcePath) {
        playResource(resourcePath, null);
    }

    public void playResource(String resourcePath, Runnable onPlay) {
        playResource(resourcePath, 0, onPlay);
    }

    public void playResource(String resourcePath, long startTimeMs, Runnable onPlay) {
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

                    player.cleanup();
                    currentBuffer = newBuffer;
                    beatDetector = newDetector;

                    player.load(currentBuffer, liveVolume);
                    if (startTimeMs > 0) {
                        player.setOffsetSeconds(startTimeMs / 1000.0f);
                    }

                    player.play(false);
                    AudioAPI.reset();
                }

                if (onPlay != null) {
                    waitForPlaybackAndRun(onPlay, 0);
                }

            } catch (Exception e) {
                logger.error("Error starting playback", e);
            }
        });
    }

    private void waitForPlaybackAndRun(Runnable onPlay, int attempts) {
        if (player.getState() == AL_PLAYING) {
            onPlay.run();
        }
        else if (attempts < 50) {
            executor.schedule(() -> waitForPlaybackAndRun(onPlay, attempts + 1), 1, TimeUnit.MILLISECONDS);
        }
        else {
            logger.warn("Playback started but AL_PLAYING state was not reached after 50ms.");
        }
    }

    public void seekTo(long timeMs) {
        synchronized (engineLock) {
            if (currentBuffer != null && engineRunning) {
                float targetSeconds = timeMs / 1000.0f;
                targetSeconds = Math.clamp(targetSeconds, 0.0f, currentBuffer.durationSeconds());
                player.setOffsetSeconds(targetSeconds);
                AudioAPI.setCurrentPositionSeconds(targetSeconds);
                logger.info("Seeked to: " + targetSeconds + "s");
            }
        }
    }

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

                int state = player.getState();
                offset = player.getOffsetSeconds();
                duration = currentBuffer.durationSeconds();

                if (state == AL_STOPPED && offset >= duration - 0.1f) {
                    logger.info("Song finished. Clearing memory...");
                    currentBuffer = null;
                    AudioAPI.reset();
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
                combo = AudioAPI.getHitCombo();
                speed = AudioAPI.getParticleSpeedMultiplier();
            }
            if (webVisualizer != null) {
                webVisualizer.update(
                        bars, intensities, features, energy,
                        combo, speed, paused, offset, duration
                );
            }

        } catch (Throwable t) {
            assert t instanceof Exception;
            logger.error("\u001B[31m[AudioEngine] Error processing frame: " + t.getMessage() + "\u001B[0m", t);
        }
    }

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
            player.pause();
            AudioAPI.setPaused(true);
            AudioAPI.setPlaying(false);
        }
    }

    public void resumeSong() {
        synchronized (engineLock) {
            player.play(false);
            AudioAPI.setPaused(false);
            AudioAPI.setPlaying(true);
        }
    }

    public void stopSong() {
        synchronized (engineLock) {
            player.stop();
            AudioAPI.setPlaying(false);
            AudioAPI.setPaused(true);
        }
    }

    public void waitForExit() {
        try {
            if (exitLatch != null) {
                exitLatch.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        synchronized (engineLock) {
            if (shuttingDown) return;
            shuttingDown = true;
            engineRunning = false;

            if (loopTask != null) {
                loopTask.cancel(false);
            }

            if (executor != null) {
                executor.shutdownNow();
            }

            player.cleanup();
            OpenALContext.destroy();

            if (webVisualizer != null) {
                webVisualizer.stop();
            }

            if (exitLatch != null) {
                exitLatch.countDown();
            }
        }
    }

    public AudioConfig getConfig() {
        return this.config;
    }

}