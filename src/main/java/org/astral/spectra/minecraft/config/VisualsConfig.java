package org.astral.spectra.minecraft.config;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;
import java.util.*;

public final class VisualsConfig {
    private final Map<String, VisualPreset> presetsMap = new LinkedHashMap<>();

    public void loadPresetFromYaml(String name, @NonNull ConfigurationSection sec) {
        VisualPreset p = new VisualPreset();
        p.renderMode = sec.getString("render-mode", "mixed");
        p.shape = sec.getString("shape", "line");
        p.mainBlock = Material.matchMaterial(sec.getString("main-block", "CYAN_CONCRETE"));
        p.accentBlock = Material.matchMaterial(sec.getString("accent-block", "BLUE_CONCRETE"));
        p.hitBlock = Material.matchMaterial(sec.getString("hit-block", "WHITE_CONCRETE"));

        try {
            p.lowParticle = Particle.valueOf(sec.getString("low-particle", "FIREWORKS_SPARK"));
            p.highParticle = Particle.valueOf(sec.getString("high-particle", "END_ROD"));
        } catch (Exception ignored) {}

        p.maxHeight = sec.getInt("max-height", 10);
        p.spacing = sec.getDouble("spacing", 1.0);
        p.radius = sec.getDouble("radius", 6.0);
        presetsMap.put(name.toLowerCase(), p);
    }

    public Map<String, VisualPreset> getPresetsMap() { return presetsMap; }

    public static class VisualPreset {
        public String renderMode = "mixed", shape = "line";
        public Material mainBlock = Material.CYAN_CONCRETE, accentBlock = Material.BLUE_CONCRETE, hitBlock = Material.WHITE_CONCRETE;
        public Particle lowParticle = Particle.FIREWORK, highParticle = Particle.END_ROD;
        public double spacing = 1.0, radius = 6.0;
        public int maxHeight = 10;

        public VisualPreset() {}
        public VisualPreset(@NonNull VisualPreset other) {
            this.renderMode = other.renderMode; this.shape = other.shape;
            this.mainBlock = other.mainBlock; this.accentBlock = other.accentBlock; this.hitBlock = other.hitBlock;
            this.lowParticle = other.lowParticle; this.highParticle = other.highParticle;
            this.spacing = other.spacing; this.radius = other.radius; this.maxHeight = other.maxHeight;
        }

        public String getRenderMode() { return renderMode; }
        public String getShape() { return shape; }
        public void setShape(String s) { this.shape = s; }
        public void setMaxHeight(int h) { this.maxHeight = h; }
        public void setSpacing(double s) { this.spacing = s; }
        public void setRadius(double r) { this.radius = r; }
        public int getMaxHeight() { return maxHeight; }
        public double getSpacing() { return spacing; }
        public double getRadius() { return radius; }
        public Material getMainBlock() { return mainBlock; }
        public Material getAccentBlock() { return accentBlock; }
        public Material getHitBlock() { return hitBlock; }
        public Particle getLowParticle() { return lowParticle; }
        public Particle getHighParticle() { return highParticle; }
    }
}