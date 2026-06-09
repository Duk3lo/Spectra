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

    public SpectraConfigManager(SpectraPlugin plugin) { this.plugin = plugin; }

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
        gen.setDelayedTask(config.getString("audio.general.delayed-task", "0s"));
        gen.setAutoOpenBrowser(config.getBoolean("web-visualizer.auto-open-browser", true));

        audioConfig.setGeneral(gen);

        AudioConfig.Visualizer vis = new AudioConfig.Visualizer();
        vis.setFftSize(config.getInt("audio.visualizer.fft-size", 2048));
        vis.setNumBars(config.getInt("audio.visualizer.num-bars", 64));
        audioConfig.setVisualizer(vis);

        AudioConfig.Smoothing smoothing = new AudioConfig.Smoothing();
        smoothing.setAttack((float) config.getDouble("audio.smoothing.attack", 0.85));
        smoothing.setDecay((float) config.getDouble("audio.smoothing.decay", 0.18));
        audioConfig.setSmoothing(smoothing);

        AudioConfig.BeatDetection beat = new AudioConfig.BeatDetection();
        beat.setBassJumpThreshold((float) config.getDouble("audio.beat-detection.bass-jump-threshold", 1.35));
        beat.setSnareJumpThreshold((float) config.getDouble("audio.beat-detection.snare-jump-threshold", 1.25));
        beat.setHatJumpThreshold((float) config.getDouble("audio.beat-detection.hat-jump-threshold", 1.15));
        beat.setBassCooldownMs(config.getLong("audio.beat-detection.bass-cooldown-ms", 110));
        beat.setSnareCooldownMs(config.getLong("audio.beat-detection.snare-cooldown-ms", 80));
        beat.setHatCooldownMs(config.getLong("audio.beat-detection.hat-cooldown-ms", 50));
        audioConfig.setBeatDetection(beat);

        File presetsFile = new File(plugin.getDataFolder(), "presets.yml");
        if (!presetsFile.exists()) plugin.saveResource("presets.yml", false);
        FileConfiguration presetsConfig = YamlConfiguration.loadConfiguration(presetsFile);
        this.visualsConfig = new VisualsConfig();
        ConfigurationSection section = presetsConfig.getConfigurationSection("presets");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection presetSec = section.getConfigurationSection(key);
                if (presetSec != null) this.visualsConfig.loadPresetFromYaml(key, presetSec);
            }
        }
    }

    private void createDirectories() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) plugin.getLogger().warning("Failed to create the main plugin folder.");
        }

        File importFolder = new File(dataFolder, "import");
        if (!importFolder.exists()) {
            if (!importFolder.mkdirs()) plugin.getLogger().warning("Failed to create the import folder.");
        }

        File soundsFolder = new File(dataFolder, "sounds");
        if (!soundsFolder.exists()) {
            if (!soundsFolder.mkdirs()) plugin.getLogger().warning("Failed to create the sounds folder.");
        }
    }

    public String getServerIp() { return serverIp; }
    public int getServerPort() { return serverPort; }
    public int getWebVisualizerPort() { return webVisualizerPort; }
    public AudioConfig getAudioConfig() { return audioConfig; }
    public VisualsConfig getVisualsConfig() { return visualsConfig; }
}