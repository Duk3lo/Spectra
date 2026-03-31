package org.astral.spectyle.hytale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.hytale.commands.CommandAudio;
import org.astral.spectyle.hytale.configuration.AudioConfigAdapter;
import org.astral.spectyle.hytale.configuration.ConfigLoader;
import org.astral.spectyle.web.WebVisualizer;
import org.jetbrains.annotations.NotNull;

public final class SpectylePlugin extends JavaPlugin {
    private static SpectylePlugin instance;

    private final Config<AudioConfig> audioConfigFile;
    private AudioConfig config;
    private final AudioEngine engine;
    private final WebVisualizer webVisualizer;
    private static final String command = "spec";

    public SpectylePlugin(@NotNull JavaPluginInit init) {
        super(init);
        this.audioConfigFile = this.withConfig("AudioConfig", AudioConfigAdapter.CODEC);
        this.config = this.audioConfigFile.load().join();
        this.engine = new AudioEngine(this.config);
        this.webVisualizer = new WebVisualizer("OpenAL (AudioSpectrum)", 8080);

    }

    @Override
    protected void setup() {
        instance = this;
        ConfigLoader loader = new ConfigLoader(this).add("AudioConfig", audioConfigFile);

        loader.loadAll();
        config = audioConfigFile.load().join();

        System.out.println(getDataDirectory());

        engine.setWebVisualizer(webVisualizer);
        webVisualizer.setVolumeCallback(engine::setVolume);
        webVisualizer.start();
        webVisualizer.waitForConnection();
        engine.start();
        getLogger().atInfo().log("Engine Started");
        getCommandRegistry().registerCommand(new CommandAudio(command, "Using for Audios Analyzer"));
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
        getLogger().atInfo().log("Configuración recargada");
    }

    public static SpectylePlugin getInstance(){
        return instance;
    }

    public AudioEngine getAudioEngine() {
        return engine;
    }
}