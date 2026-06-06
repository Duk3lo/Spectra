package org.astral.spectra.minecraft.events.visuals;

import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.bukkit.Location;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VisualizerData {
    private final String name;
    private final VisualPreset preset;
    private final String presetName;
    private final Location pos;
    private final float yaw;
    private final Set<Location> activeBlocks = ConcurrentHashMap.newKeySet();
    private final Set<UUID> targets = ConcurrentHashMap.newKeySet();
    private boolean persistent = false;

    public VisualizerData(String name, String presetName, VisualPreset preset, Location pos, float yaw) {
        this.name = name;
        this.presetName = presetName;
        this.preset = preset;
        this.pos = pos;
        this.yaw = yaw;
    }

    public String getName() { return name; }
    public String getPresetName() { return presetName; }
    public VisualPreset getPreset() { return preset; }
    public Location getPos() { return pos; }
    public float getYaw() { return yaw; }
    public Set<Location> getActiveBlocks() { return activeBlocks; }
    public Set<UUID> getTargets() { return targets; }
    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
}