package org.astral.spectyle.hytale.configuration;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VisualsConfig {

    public static final BuilderCodec<VisualsConfig> CODEC =
            BuilderCodec.builder(VisualsConfig.class, VisualsConfig::new)
                    .append(new KeyedCodec<>("Presets", new MapCodec<>(VisualPreset.CODEC, LinkedHashMap::new)),
                            (c, v, ctx) -> c.presetsMap = v,
                            (c, ctx) -> c.presetsMap)
                    .add()
                    .build();

    private Map<String, VisualPreset> presetsMap = new LinkedHashMap<>();

    public VisualsConfig() {
        presetsMap.put("neon_line", VisualPreset.neonLine());
        presetsMap.put("fire_circle", VisualPreset.fireCircle());
        presetsMap.put("aurora_wave", VisualPreset.auroraWave());
        presetsMap.put("crystal_ring", VisualPreset.crystalRing());
        presetsMap.put("pulse_ring", VisualPreset.pulseRing());
    }

    public Map<String, VisualPreset> getPresetsMap() {
        return presetsMap;
    }

    public static class VisualPreset {
        public static final BuilderCodec<VisualPreset> CODEC =
                BuilderCodec.builder(VisualPreset.class, VisualPreset::new)
                        .append(new KeyedCodec<>("RenderMode", Codec.STRING),
                                (r, v, ctx) -> r.renderMode = v,
                                (r, ctx) -> r.renderMode)
                        .add()
                        .append(new KeyedCodec<>("Shape", Codec.STRING),
                                (r, v, ctx) -> r.shape = v,
                                (r, ctx) -> r.shape)
                        .add()
                        .append(new KeyedCodec<>("MainBlock", Codec.STRING),
                                (r, v, ctx) -> r.mainBlock = v,
                                (r, ctx) -> r.mainBlock)
                        .add()
                        .append(new KeyedCodec<>("AccentBlock", Codec.STRING),
                                (r, v, ctx) -> r.accentBlock = v,
                                (r, ctx) -> r.accentBlock)
                        .add()
                        .append(new KeyedCodec<>("HitBlock", Codec.STRING),
                                (r, v, ctx) -> r.hitBlock = v,
                                (r, ctx) -> r.hitBlock)
                        .add()
                        .append(new KeyedCodec<>("LowParticle", Codec.STRING),
                                (r, v, ctx) -> r.lowParticle = v,
                                (r, ctx) -> r.lowParticle)
                        .add()
                        .append(new KeyedCodec<>("HighParticle", Codec.STRING),
                                (r, v, ctx) -> r.highParticle = v,
                                (r, ctx) -> r.highParticle)
                        .add()
                        .append(new KeyedCodec<>("HitParticle", Codec.STRING),
                                (r, v, ctx) -> r.hitParticle = v,
                                (r, ctx) -> r.hitParticle)
                        .add()
                        .append(new KeyedCodec<>("Spacing", Codec.DOUBLE),
                                (r, v, ctx) -> r.spacing = v,
                                (r, ctx) -> r.spacing)
                        .add()
                        .append(new KeyedCodec<>("Radius", Codec.DOUBLE),
                                (r, v, ctx) -> r.radius = v,
                                (r, ctx) -> r.radius)
                        .add()
                        .append(new KeyedCodec<>("MaxHeight", Codec.INTEGER),
                                (r, v, ctx) -> r.maxHeight = v,
                                (r, ctx) -> r.maxHeight)
                        .add()
                        .append(new KeyedCodec<>("WaveAmplitude", Codec.DOUBLE),
                                (r, v, ctx) -> r.waveAmplitude = v,
                                (r, ctx) -> r.waveAmplitude)
                        .add()
                        .append(new KeyedCodec<>("WaveFrequency", Codec.DOUBLE),
                                (r, v, ctx) -> r.waveFrequency = v,
                                (r, ctx) -> r.waveFrequency)
                        .add()
                        .build();

        private String renderMode = "blocks";
        private String shape = "line";
        private String mainBlock = "Rock_Crystal_Blue_Block";
        private String accentBlock = "Rock_Crystal_Cyan_Block";
        private String hitBlock = "Rock_Crystal_Blue_Block";
        private String lowParticle = "Magic_Blue";
        private String highParticle = "Magic_Cyan";
        private String hitParticle = "Explosion_Small";
        private double spacing = 1.0;
        private double radius = 6.0;
        private int maxHeight = 10;
        private double waveAmplitude = 2.0;
        private double waveFrequency = 0.45;

        public VisualPreset() {}

        public VisualPreset(String renderMode, String shape, String mainBlock, String accentBlock,
                            String hitBlock, String lowParticle, String highParticle, String hitParticle,
                            double spacing, double radius, int maxHeight) {
            this.renderMode = renderMode;
            this.shape = shape;
            this.mainBlock = mainBlock;
            this.accentBlock = accentBlock;
            this.hitBlock = hitBlock;
            this.lowParticle = lowParticle;
            this.highParticle = highParticle;
            this.hitParticle = hitParticle;
            this.spacing = spacing;
            this.radius = radius;
            this.maxHeight = maxHeight;
        }

        public VisualPreset(@NotNull VisualPreset other) {
            this.renderMode = other.renderMode;
            this.shape = other.shape;
            this.mainBlock = other.mainBlock;
            this.accentBlock = other.accentBlock;
            this.hitBlock = other.hitBlock;
            this.lowParticle = other.lowParticle;
            this.highParticle = other.highParticle;
            this.hitParticle = other.hitParticle;
            this.spacing = other.spacing;
            this.radius = other.radius;
            this.maxHeight = other.maxHeight;
            this.waveAmplitude = other.waveAmplitude;
            this.waveFrequency = other.waveFrequency;
        }

        public static @NotNull VisualPreset neonLine() {
            return new VisualPreset(
                    "blocks",
                    "line",
                    "Rock_Crystal_Blue_Block",
                    "Rock_Crystal_Cyan_Block",
                    "Rock_Crystal_Blue_Block",
                    "Magic_Blue",
                    "Magic_Cyan",
                    "Explosion_Small",
                    1.5,
                    6.0,
                    20
            );
        }

        public static @NotNull VisualPreset fireCircle() {
            return new VisualPreset(
                    "particles",
                    "circle",
                    "Magma_Block",
                    "Lava_Block",
                    "Magma_Block",
                    "Flame_Small",
                    "Flame_Large",
                    "Explosion_Huge",
                    1.0,
                    8.0,
                    15
            );
        }

        public static @NotNull VisualPreset auroraWave() {
            VisualPreset p = new VisualPreset(
                    "mixed",
                    "wave",
                    "Rock_Crystal_Cyan_Block",
                    "Rock_Crystal_Blue_Block",
                    "Rock_Crystal_Cyan_Block",
                    "Magic_Blue",
                    "Magic_Cyan",
                    "Explosion_Small",
                    0.9,
                    7.0,
                    18
            );
            p.waveAmplitude = 2.5;
            p.waveFrequency = 0.55;
            return p;
        }

        public static @NotNull VisualPreset crystalRing() {
            return new VisualPreset(
                    "blocks",
                    "ring",
                    "Rock_Crystal_Blue_Block",
                    "Rock_Crystal_Cyan_Block",
                    "Rock_Crystal_Cyan_Block",
                    "Magic_Blue",
                    "Magic_Cyan",
                    "Explosion_Small",
                    1.0,
                    10.0,
                    12
            );
        }

        public static @NotNull VisualPreset pulseRing() {
            VisualPreset p = new VisualPreset(
                    "mixed",
                    "pulse_ring",
                    "Rock_Crystal_Blue_Block",
                    "Rock_Crystal_Cyan_Block",
                    "Rock_Crystal_Cyan_Block",
                    "Magic_Blue",
                    "Magic_Cyan",
                    "Explosion_Small",
                    1.0,
                    8.0,
                    16
            );
            p.waveAmplitude = 1.5;
            p.waveFrequency = 0.40;
            return p;
        }

        public boolean isBlocks() {
            return "blocks".equalsIgnoreCase(renderMode);
        }

        public boolean isParticles() {
            return "particles".equalsIgnoreCase(renderMode);
        }

        public boolean isMixed() {
            return "mixed".equalsIgnoreCase(renderMode);
        }

        public boolean isCircle() {
            return "circle".equalsIgnoreCase(shape);
        }

        public boolean isRing() {
            return "ring".equalsIgnoreCase(shape);
        }

        public boolean isWave() {
            return "wave".equalsIgnoreCase(shape);
        }

        public boolean isPulseRing() {
            return "pulse_ring".equalsIgnoreCase(shape);
        }

        public String getRenderMode() {
            return renderMode;
        }

        public String getShape() {
            return shape;
        }

        public void setShape(String shape) {
            this.shape = shape;
        }

        public String getMainBlock() {
            return mainBlock;
        }

        public String getAccentBlock() {
            return accentBlock;
        }

        public String getHitBlock() {
            return hitBlock;
        }

        public String getLowParticle() {
            return lowParticle;
        }

        public String getHighParticle() {
            return highParticle;
        }

        public String getHitParticle() {
            return hitParticle;
        }

        public double getSpacing() {
            return spacing;
        }

        public void setSpacing(double spacing) {
            this.spacing = spacing;
        }

        public double getRadius() {
            return radius;
        }

        public void setRadius(double radius) {
            this.radius = radius;
        }

        public int getMaxHeight() {
            return maxHeight;
        }

        public void setMaxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
        }

        public double getWaveAmplitude() {
            return waveAmplitude;
        }

        public double getWaveFrequency() {
            return waveFrequency;
        }

        public void setWaveAmplitude(double waveAmplitude) {
            this.waveAmplitude = waveAmplitude;
        }

        public void setWaveFrequency(double waveFrequency) {
            this.waveFrequency = waveFrequency;
        }
    }
}