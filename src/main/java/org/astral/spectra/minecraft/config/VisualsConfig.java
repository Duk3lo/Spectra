package org.astral.spectra.minecraft.config;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;
import java.util.*;

public final class VisualsConfig {
    private final Map<String, VisualPreset> presetsMap = new LinkedHashMap<>();

    public void loadPresetFromYaml(@NonNull String name, @NonNull ConfigurationSection sec) {
        VisualPreset p = new VisualPreset();
        p.renderMode = sec.getString("render-mode", "mixed");
        p.shape = sec.getString("shape", "line");

        p.mainBlock = parseMaterial(sec.getString("main-block"), Material.CYAN_CONCRETE);
        p.accentBlock = parseMaterial(sec.getString("accent-block"), Material.BLUE_CONCRETE);
        p.hitBlock = parseMaterial(sec.getString("hit-block"), Material.WHITE_CONCRETE);

        p.lowParticle = parseParticle(sec.getString("low-particle"), Particle.FIREWORK);
        p.highParticle = parseParticle(sec.getString("high-particle"), Particle.END_ROD);

        p.maxHeight = sec.getInt("max-height", 10);
        p.spacing = sec.getDouble("spacing", 1.0);
        p.radius = sec.getDouble("radius", 6.0);
        p.platforms = sec.getBoolean("platforms", false);
        p.debris = sec.getBoolean("debris", true);
        p.glow = sec.getBoolean("glow", true);
        p.walkBars = sec.getBoolean("walk-bars", false);
        p.bars = sec.getInt("bar-count", 0);

        presetsMap.put(name.toLowerCase(), p);
    }

    private Material parseMaterial(String name, Material def) {
        if (name == null) return def;
        Material m = Material.matchMaterial(name);
        return m != null ? m : def;
    }

    private Particle parseParticle(String name, Particle def) {
        if (name == null) return def;
        try { return Particle.valueOf(name.toUpperCase()); }
        catch (Exception e) { return def; }
    }

    public Map<String, VisualPreset> getPresetsMap() { return presetsMap; }

    public static class VisualPreset {
        public String renderMode = "mixed", shape = "line";
        public Material mainBlock = Material.CYAN_CONCRETE, accentBlock = Material.BLUE_CONCRETE, hitBlock = Material.WHITE_CONCRETE;
        public Particle lowParticle = Particle.FIREWORK, highParticle = Particle.END_ROD;
        public double spacing = 1.0, radius = 6.0;
        public int maxHeight = 10;
        public int bars = 0;
        public boolean platforms = false;
        public boolean debris = true;
        public boolean glow = true;
        public boolean walkBars = false;

        public VisualPreset() {}
        public VisualPreset(@NonNull VisualPreset other) {
            this.renderMode = other.renderMode; this.shape = other.shape;
            this.mainBlock = other.mainBlock; this.accentBlock = other.accentBlock; this.hitBlock = other.hitBlock;
            this.lowParticle = other.lowParticle; this.highParticle = other.highParticle;
            this.spacing = other.spacing; this.radius = other.radius; this.maxHeight = other.maxHeight;
            this.platforms = other.platforms;
            this.debris = other.debris;
            this.glow = other.glow;
            this.walkBars = other.walkBars;
            this.bars = other.bars;
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
        public boolean isPlatforms() { return platforms; }
        public boolean isDebris() { return debris; }
        public void setDebris(boolean d) { this.debris = d; }
        public boolean isGlow() { return glow; }
        public void setGlow(boolean g) { this.glow = g; }
        public boolean isWalkBars() { return walkBars; }
        public void setWalkBars(boolean w) { this.walkBars = w; }
        public int getBars() { return bars; }
        public void setBars(int b) { this.bars = b; }
    }
}