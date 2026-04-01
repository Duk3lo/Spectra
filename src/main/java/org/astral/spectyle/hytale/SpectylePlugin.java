package org.astral.spectyle.hytale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.hytale.commands.SpectyleCommands;
import org.astral.spectyle.hytale.configuration.AudioConfigAdapter;
import org.astral.spectyle.hytale.configuration.ConfigLoader;
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
    private final PluginLogger pluginLogger = new PluginLogger(this);

    public SpectylePlugin(@NotNull JavaPluginInit init) {
        super(init);
        audioConfigFile = withConfig("AudioConfig", AudioConfigAdapter.CODEC);
        webVisualizer = new WebVisualizer("OpenAL (AudioSpectrum) - Hytale", 8080, pluginLogger);
    }

    @Override
    protected void setup() {
        instance = this;
        ConfigLoader.init(this);
        ConfigLoader.add("AudioConfig", audioConfigFile);
        ConfigLoader.loadAll();
        config = audioConfigFile.load().join();
        engine = new AudioEngine(config, pluginLogger);

        engine.setWebVisualizer(webVisualizer);
        webVisualizer.setVolumeCallback(engine::setVolume);
        webVisualizer.start();
        webVisualizer.waitForConnection();
        engine.start();
        getLogger().atInfo().log("Engine Started");
        getCommandRegistry().registerCommand(new SpectyleCommands(command, "Using for Audios Analyzer"));
    }

    @Override
    protected void shutdown() {
        engine.shutdown();
        getLogger().atInfo().log("Close Engine");
    }

    public void reloadConfig() {
        AudioConfig newConfig = audioConfigFile.load().join();
        this.config = newConfig;
        engine.reloadConfiguration(newConfig);
        getLogger().atInfo().log("Reloading Success!");
    }

    public static SpectylePlugin getInstance(){
        return instance;
    }

    public AudioEngine getAudioEngine() {
        return engine;
    }

    public AudioConfig getAudioConfig(){
        return config;
    }
}