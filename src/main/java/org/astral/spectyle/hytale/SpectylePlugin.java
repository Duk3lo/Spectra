package org.astral.spectyle.hytale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.hytale.commands.SpectyleCommands;
import org.astral.spectyle.hytale.configuration.AudioConfigAdapter;
import org.astral.spectyle.hytale.configuration.ConfigLoader;
import org.astral.spectyle.hytale.configuration.VisualsConfig;
import org.astral.spectyle.hytale.events.SpectyleEvents;
import org.astral.spectyle.hytale.loggin.PluginLogger;
import org.astral.spectyle.web.WebVisualizer;
import org.jetbrains.annotations.NotNull;

public final class SpectylePlugin extends JavaPlugin {
    private static SpectylePlugin instance;

    private final Config<AudioConfig> audioConfigFile;
    private AudioConfig config;
    private AudioEngine engine;
    private final WebVisualizer webVisualizer;
    private static final String command = "spec";
    private final PluginLogger pluginLogger;
    private final Config<VisualsConfig> visualsConfigFile;
    private VisualsConfig visualsConfig;
    private static final String audio_n = "AudioConfig";
    private static final String visuals_n = "VisualsConfig";

    public SpectylePlugin(@NotNull JavaPluginInit init) {
        super(init);
        this.pluginLogger = new PluginLogger(this);

        this.audioConfigFile = withConfig(audio_n, AudioConfigAdapter.CODEC);
        this.visualsConfigFile = withConfig(visuals_n, VisualsConfig.CODEC);

        this.webVisualizer = new WebVisualizer("OpenAL (AudioSpectrum) - Hytale", 8080, pluginLogger);
        bootstrapAssetPack();
    }

    private void bootstrapAssetPack() {
        ConfigLoader.init(this);
    }

    @Override
    protected void setup() {
        instance = this;

        ConfigLoader.add(audio_n, audioConfigFile);
        ConfigLoader.add(visuals_n, visualsConfigFile);
        ConfigLoader.loadAll();

        config = audioConfigFile.load().join();
        visualsConfig = visualsConfigFile.load().join();

        engine = new AudioEngine(config, pluginLogger);
        engine.setWebVisualizer(webVisualizer);
        webVisualizer.setVolumeCallback(engine::setVolume);

        webVisualizer.start();
        webVisualizer.waitForConnection();

        engine.start();

        getCommandRegistry().registerCommand(new SpectyleCommands(command, "Using for Audios Analyzer"));
        SpectyleEvents.RegisterAll();
        getLogger().atInfo().log("Engine Started");
    }

    @Override
    protected void shutdown() {
        if (engine != null) {
            engine.shutdown();
        }
        getLogger().atInfo().log("Close Engine");
    }

    public void reloadConfig() {
        AudioConfig newConfig = audioConfigFile.load().join();
        this.config = newConfig;
        this.visualsConfig = visualsConfigFile.load().join();
        engine.reloadConfiguration(newConfig);
        getLogger().atInfo().log("Reloading Success!");
    }

    public static SpectylePlugin getInstance() {
        return instance;
    }

    public AudioEngine getAudioEngine() {
        return engine;
    }

    public VisualsConfig getVisualsConfig() {
        return visualsConfig;
    }

}