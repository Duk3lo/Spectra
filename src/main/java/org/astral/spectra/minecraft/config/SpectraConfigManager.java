package org.astral.spectra.minecraft.config;

import org.astral.spectra.config.AudioConfig;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class SpectraConfigManager {
    private final SpectraPlugin plugin;
    private AudioConfig audioConfig;
    private VisualsConfig visualsConfig;

    private String serverIp;
    private int serverPort;
    private int webVisualizerPort;

    public SpectraConfigManager(SpectraPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAllConfigs() {
        createDirectories();

        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.serverIp = config.getString("web-server.ip", "127.0.0.1");
        this.serverPort = config.getInt("web-server.port", 8080);
        this.webVisualizerPort = config.getInt("web-visualizer.port", 8081);

        this.audioConfig = new AudioConfig();
        AudioConfig.General gen = new AudioConfig.General();
        gen.setCurrentVolume((float) config.getDouble("audio.general.current-volume", 1.0));
        gen.setUpdateRateMs(config.getInt("audio.general.update-rate-ms", 16));
        audioConfig.setGeneral(gen);

        AudioConfig.Visualizer vis = new AudioConfig.Visualizer();
        vis.setFftSize(config.getInt("audio.visualizer.fft-size", 2048));
        vis.setNumBars(config.getInt("audio.visualizer.num-bars", 64));
        audioConfig.setVisualizer(vis);

        File presetsFile = new File(plugin.getDataFolder(), "presets.yml");
        if (!presetsFile.exists()) plugin.saveResource("presets.yml", false);
        FileConfiguration presetsConfig = YamlConfiguration.loadConfiguration(presetsFile);

        this.visualsConfig = new VisualsConfig();
        ConfigurationSection section = presetsConfig.getConfigurationSection("presets");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection presetSec = section.getConfigurationSection(key);
                if (presetSec != null) {
                    this.visualsConfig.loadPresetFromYaml(key, presetSec);
                }
            }
        }
    }

    private void createDirectories() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Failed to create the main plugin folder.");
        }

        File importFolder = new File(plugin.getDataFolder(), "import");
        if (!importFolder.exists() && !importFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create the import folder.");
        }

        File soundsFolder = new File(plugin.getDataFolder(), "sounds");
        if (!soundsFolder.exists() && !soundsFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create the sounds folder.");
        }
    }

    public String getServerIp() {
        return serverIp;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getWebVisualizerPort() {
        return webVisualizerPort;
    }

    public AudioConfig getAudioConfig() {
        return audioConfig;
    }

    public VisualsConfig getVisualsConfig() {
        return visualsConfig;
    }
}