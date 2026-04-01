package org.astral.spectyle.audio.engine;

import org.astral.spectyle.web.WebVisualizer;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.audio.analysis.SpectrumAnalyzer;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.audio.decode.AudioDecoder;
import org.astral.spectyle.audio.playback.OpenALContext;
import org.astral.spectyle.audio.playback.OpenALPlayer;
import org.astral.spectyle.audio.state.AudioBuffer;
import org.astral.spectyle.audio.detection.BeatDetector;
import org.astral.spectyle.logging.EngineLogger;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.lwjgl.openal.AL10.AL_PAUSED;
import static org.lwjgl.openal.AL10.AL_PLAYING;

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

    public void playSong(Path path, Runnable onPlaybackStart, long delayTime, TimeUnit timeUnit) {
        playSong(path, 0, onPlaybackStart, delayTime, timeUnit);
    }

    public void playSong(Path path, long startTimeMs, Runnable onPlaybackStart, long delayTime, TimeUnit timeUnit) {
        schedulePlayback(() -> AudioDecoder.loadAudio(path), startTimeMs, onPlaybackStart, delayTime, timeUnit);
    }

    public void playResource(String resourcePath, Runnable onPlaybackStart, long delayTime, TimeUnit timeUnit) {
        playResource(resourcePath, 0, onPlaybackStart, delayTime, timeUnit);
    }

    public void playResource(String resourcePath, long startTimeMs, Runnable onPlaybackStart, long delayTime, TimeUnit timeUnit) {
        schedulePlayback(() -> AudioDecoder.loadResource(resourcePath), startTimeMs, onPlaybackStart, delayTime, timeUnit);
    }

    private void schedulePlayback(Supplier<AudioBuffer> loader, long startTimeMs, Runnable onPlaybackStart, long delayTime, TimeUnit timeUnit) {
        if (executor == null) {
            throw new IllegalStateException("Engine not started. Call start() first.");
        }

        executor.schedule(() -> {
            synchronized (engineLock) {
                if (!engineRunning) return;

                player.cleanup();

                currentBuffer = loader.get();
                player.load(currentBuffer, config.getGeneral().getCurrentVolume());

                if (startTimeMs > 0) {
                    float seconds = startTimeMs / 1000.0f;
                    player.setOffsetSeconds(seconds);
                }

                beatDetector = new BeatDetector(config, logger);
                beatDetector.preScan(currentBuffer, analyzer);

                player.play(false);
                AudioAPI.reset();

                if (onPlaybackStart != null) {
                    onPlaybackStart.run();
                }
            }
        }, delayTime, timeUnit);
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
        float v = Math.clamp(volume, 0.0f, 1.0f);

        synchronized (engineLock) {
            player.setVolume(v);
            AudioAPI.setVolume(v);
        }

        if (webVisualizer != null) {
            webVisualizer.sendVolumeUpdate(v);
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

    public WebVisualizer getWebVisualizer(){
        return webVisualizer;
    }
}