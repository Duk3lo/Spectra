package org.astral.spectra.minecraft.events.visuals;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.astral.spectra.minecraft.events.visuals.world.AudioBarsBlocks;
import org.astral.spectra.minecraft.utils.SchedulerUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class VisualizerManager {
    private static final Map<String, VisualizerData> activeVisualizers = new ConcurrentHashMap<>();
    private static File dataFile;
    private static YamlConfiguration dataConfig;

    public static void init(@NonNull SpectraPlugin plugin) {
        activeVisualizers.entrySet().removeIf(entry -> entry.getValue().isPersistent());
        dataFile = new File(plugin.getDataFolder(), "visuals_data.yml");
        loadPersistentVisuals();
    }

    public static @NonNull String start(String reqName, String presetName, VisualPreset preset, Location pos, boolean save, Set<UUID> targets) {
        String name = (reqName == null || reqName.isEmpty()) ? "vis_" + System.currentTimeMillis() : reqName;
        VisualizerData data = new VisualizerData(name, presetName, preset, pos, pos.getYaw());
        data.setPersistent(save);
        data.getTargets().addAll(targets);

        activeVisualizers.put(name, data);
        if (save) saveToDisk(data);
        return name;
    }

    public static void stop(String name) {
        VisualizerData data = activeVisualizers.remove(name);
        if (data != null) {
            SchedulerUtil.runOnRegion(SpectraPlugin.getInstance(), data.getPos(), () -> AudioBarsBlocks.clearJustBlocks(data));
            if (data.isPersistent()) removeFromDisk(name);
        }
    }

    public static void stopAll() {
        for (String name : new HashSet<>(activeVisualizers.keySet())) {
            VisualizerData data = activeVisualizers.get(name);
            if (data != null) {
                SchedulerUtil.runOnRegion(SpectraPlugin.getInstance(), data.getPos(), () -> AudioBarsBlocks.clearJustBlocks(data));
                if (!data.isPersistent()) activeVisualizers.remove(name);
            }
        }
    }

    private static void saveToDisk(@NonNull VisualizerData data) {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        String path = "saved." + data.getName();
        dataConfig.set(path + ".preset", data.getPresetName());
        dataConfig.set(path + ".shape", data.getPreset().getShape());
        dataConfig.set(path + ".height", data.getPreset().getMaxHeight());
        dataConfig.set(path + ".radius", data.getPreset().getRadius());
        dataConfig.set(path + ".spacing", data.getPreset().getSpacing());
        dataConfig.set(path + ".debris", data.getPreset().isDebris());
        dataConfig.set(path + ".glow", data.getPreset().isGlow());
        dataConfig.set(path + ".walk-bars", data.getPreset().isWalkBars());
        dataConfig.set(path + ".bars", data.getPreset().getBars());
        dataConfig.set(path + ".location", data.getPos());
        dataConfig.set(path + ".yaw", data.getYaw());

        List<String> uuidStrings = data.getTargets().stream().map(UUID::toString).collect(Collectors.toList());
        dataConfig.set(path + ".targets", uuidStrings);

        try { dataConfig.save(dataFile); } catch (Exception ignored) {}
    }

    private static void removeFromDisk(String name) {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        dataConfig.set("saved." + name, null);
        try { dataConfig.save(dataFile); } catch (Exception ignored) {}
    }

    private static void loadPersistentVisuals() {
        if (!dataFile.exists()) return;
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = dataConfig.getConfigurationSection("saved");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String pName = section.getString(key + ".preset");
            Location loc = section.getLocation(key + ".location");
            float yaw = (float) section.getDouble(key + ".yaw", 0.0);

            VisualPreset base = SpectraPlugin.getInstance().getConfigManager().getVisualsConfig().getPresetsMap().get(pName);
            if (base != null && loc != null) {
                VisualPreset custom = new VisualPreset(base);
                custom.setShape(section.getString(key + ".shape", base.getShape()));
                custom.setMaxHeight(section.getInt(key + ".height", base.getMaxHeight()));
                custom.setRadius(section.getDouble(key + ".radius", base.getRadius()));
                custom.setSpacing(section.getDouble(key + ".spacing", base.getSpacing()));
                custom.setDebris(section.getBoolean(key + ".debris", base.isDebris()));
                custom.setGlow(section.getBoolean(key + ".glow", base.isGlow()));
                custom.setWalkBars(section.getBoolean(key + ".walk-bars", base.isWalkBars()));
                custom.setBars(section.getInt(key + ".bars", base.getBars()));

                VisualizerData data = new VisualizerData(key, pName, custom, loc, yaw);
                data.setPersistent(true);

                List<String> uuidStrings = section.getStringList(key + ".targets");
                for (String s : uuidStrings) {
                    try { data.getTargets().add(UUID.fromString(s)); } catch (Exception ignored) {}
                }

                activeVisualizers.put(key, data);
            }
        }
    }

    public static Collection<VisualizerData> getAllVisualizers() { return activeVisualizers.values(); }
}