package org.astral.spectyle.audio.engine;

import org.astral.spectyle.web.WebVisualizer;
import org.astral.spectyle.audio.api.AudioAPI;
import org.astral.spectyle.audio.analysis.SpectrumAnalyzer;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.audio.decode.OggDecoder;
import org.astral.spectyle.audio.playback.OpenALContext;
import org.astral.spectyle.audio.playback.OpenALPlayer;
import org.astral.spectyle.audio.state.AudioBuffer;
import org.astral.spectyle.audio.detection.BeatDetector;
import org.astral.spectyle.logging.EngineLogger;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

            if (webVisualizer != null) {
                webVisualizer.sendVolumeUpdate(config.getGeneral().getCurrentVolume());
            }

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

        synchronized (engineLock) {
            boolean rebuildAnalysis =
                    (config.getVisualizer().getFftSize() != newConfig.getVisualizer().getFftSize()) ||
                            (config.getVisualizer().getNumBars() != newConfig.getVisualizer().getNumBars());

            boolean restartLoop = (config.getGeneral().getUpdateRateMs() != newConfig.getGeneral().getUpdateRateMs());

            this.config = newConfig;

            setVolume(config.getGeneral().getCurrentVolume());

            if (webVisualizer != null) {
                webVisualizer.sendVolumeUpdate(config.getGeneral().getCurrentVolume());
            }

            if (rebuildAnalysis) {
                this.analyzer = new SpectrumAnalyzer(config);
                if (currentBuffer != null) {
                    this.beatDetector = new BeatDetector(config, logger);
                    this.beatDetector.preScan(currentBuffer, analyzer);
                }
            }

            if (!rebuildAnalysis && beatDetector != null) {
                this.beatDetector = new BeatDetector(config, logger);
                if (currentBuffer != null) {
                    this.beatDetector.preScan(currentBuffer, analyzer);
                }
            }

            if (restartLoop && engineRunning) {
                startProcessingTask();
            }
        }
    }

    public void playSong(String path, Runnable onPlaybackStart, long delayTime, TimeUnit timeUnit) {
        if (executor == null) {
            throw new IllegalStateException("Engine not started. Call start() first.");
        }

        executor.schedule(() -> {
            synchronized (engineLock) {
                if (!engineRunning) return;

                player.cleanup();

                currentBuffer = OggDecoder.loadAudio(path);
                player.load(currentBuffer, config.getGeneral().getCurrentVolume());

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

    private void processAudioFrame() {
        if (!engineRunning) return;

        synchronized (engineLock) {
            if (currentBuffer == null || beatDetector == null || analyzer == null) return;

            int state = player.getState();
            float offset = player.getOffsetSeconds();
            float duration = currentBuffer.durationSeconds();

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

                if (webVisualizer != null) {
                    webVisualizer.update(
                            beatDetector.getSmoothedBars(),
                            beatDetector.getBeatIntensity(),
                            beatDetector.getReactiveSignals(),
                            AudioAPI.getGlobalEnergy(),
                            AudioAPI.getHitCombo(),
                            AudioAPI.getParticleSpeedMultiplier(),
                            false,
                            offset,
                            duration
                    );
                }

            } else {
                AudioAPI.setPaused(state == AL_PAUSED);
                AudioAPI.setPlaying(false);

                if (webVisualizer != null) {
                    webVisualizer.update(
                            beatDetector.getSmoothedBars(),
                            beatDetector.getBeatIntensity(),
                            beatDetector.getReactiveSignals(),
                            AudioAPI.getGlobalEnergy(),
                            AudioAPI.getHitCombo(),
                            AudioAPI.getParticleSpeedMultiplier(),
                            true,
                            offset,
                            duration
                    );
                }
            }
        }
    }

    public void setVolume(float volume) {
        synchronized (engineLock) {
            player.setVolume(volume);
            AudioAPI.setVolume(volume);
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
}