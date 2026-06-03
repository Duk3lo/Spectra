package org.astral.spectra.hytale.configuration;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.astral.spectra.config.AudioConfig;
import org.jetbrains.annotations.NotNull;

public final class AudioConfigAdapter {

    public static final BuilderCodec<AudioConfig> CODEC = BuilderCodec.builder(AudioConfig.class, AudioConfig::new)
            .append(new KeyedCodec<>("General", generalCodec()), (m, v, ctx) -> m.setGeneral(v), (m, ctx) -> m.getGeneral())
            .add()
            .append(new KeyedCodec<>("Visualizer", visualizerCodec()), (m, v, ctx) -> m.setVisualizer(v), (m, ctx) -> m.getVisualizer())
            .add()
            .append(new KeyedCodec<>("Smoothing", smoothingCodec()), (m, v, ctx) -> m.setSmoothing(v), (m, ctx) -> m.getSmoothing())
            .add()
            .append(new KeyedCodec<>("BeatDetection", beatDetectionCodec()), (m, v, ctx) -> m.setBeatDetection(v), (m, ctx) -> m.getBeatDetection())
            .add()
            .build();

    private static @NotNull BuilderCodec<AudioConfig.General> generalCodec() {
        return BuilderCodec.builder(AudioConfig.General.class, AudioConfig.General::new)
                .append(new KeyedCodec<>("CurrentVolume", Codec.FLOAT), (m, v, ctx) -> m.setCurrentVolume(v), (m, ctx) -> m.getCurrentVolume()).add()
                .append(new KeyedCodec<>("UpdateRateMs", Codec.INTEGER), (m, v, ctx) -> m.setUpdateRateMs(v), (m, ctx) -> m.getUpdateRateMs()).add()
                .append(new KeyedCodec<>("DelayedTask", Codec.STRING), (m, v, ctx) -> m.setDelayedTask(v), (m, ctx) -> m.getDelayedTask()).add()
                .build();
    }

    private static @NotNull BuilderCodec<AudioConfig.Visualizer> visualizerCodec() {
        return BuilderCodec.builder(AudioConfig.Visualizer.class, AudioConfig.Visualizer::new)
                .append(new KeyedCodec<>("FftSize", Codec.INTEGER), (m, v, ctx) -> m.setFftSize(v), (m, ctx) -> m.getFftSize()).add()
                .append(new KeyedCodec<>("NumBars", Codec.INTEGER), (m, v, ctx) -> m.setNumBars(v), (m, ctx) -> m.getNumBars()).add()
                .build();
    }

    private static @NotNull BuilderCodec<AudioConfig.Smoothing> smoothingCodec() {
        return BuilderCodec.builder(AudioConfig.Smoothing.class, AudioConfig.Smoothing::new)
                .append(new KeyedCodec<>("Attack", Codec.FLOAT), (m, v, ctx) -> m.setAttack(v), (m, ctx) -> m.getAttack()).add()
                .append(new KeyedCodec<>("Decay", Codec.FLOAT), (m, v, ctx) -> m.setDecay(v), (m, ctx) -> m.getDecay()).add()
                .build();
    }

    private static @NotNull BuilderCodec<AudioConfig.BeatDetection> beatDetectionCodec() {
        return BuilderCodec.builder(AudioConfig.BeatDetection.class, AudioConfig.BeatDetection::new)
                .append(new KeyedCodec<>("BassJumpThreshold", Codec.FLOAT), (m, v, ctx) -> m.setBassJumpThreshold(v), (m, ctx) -> m.getBassJumpThreshold()).add()
                .append(new KeyedCodec<>("SnareJumpThreshold", Codec.FLOAT), (m, v, ctx) -> m.setSnareJumpThreshold(v), (m, ctx) -> m.getSnareJumpThreshold()).add()
                .append(new KeyedCodec<>("HatJumpThreshold", Codec.FLOAT), (m, v, ctx) -> m.setHatJumpThreshold(v), (m, ctx) -> m.getHatJumpThreshold()).add()
                .append(new KeyedCodec<>("BassCooldownMs", Codec.LONG), (m, v, ctx) -> m.setBassCooldownMs(v), (m, ctx) -> m.getBassCooldownMs()).add()
                .append(new KeyedCodec<>("SnareCooldownMs", Codec.LONG), (m, v, ctx) -> m.setSnareCooldownMs(v), (m, ctx) -> m.getSnareCooldownMs()).add()
                .append(new KeyedCodec<>("HatCooldownMs", Codec.LONG), (m, v, ctx) -> m.setHatCooldownMs(v), (m, ctx) -> m.getHatCooldownMs()).add()
                .build();
    }
}