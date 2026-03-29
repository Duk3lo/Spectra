package org.astral.spectyle;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.hytale.AudioConfigAdapter;
import org.astral.spectyle.web.WebVisualizer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class Main extends JavaPlugin {

    private static Main instance;
    private static final String FINAL_AUDIO = "src/main/resources/musics/miss.ogg";
    private static final WebVisualizer webVisualizer = new WebVisualizer("OpenAL (AudioSpectrum)", 8080);

    private final Config<AudioConfig> audioConfigFile;
    private AudioConfig config;
    private final AudioEngine engine;

    public Main(@NotNull JavaPluginInit init) {
        super(init);
        this.audioConfigFile = this.withConfig("AudioConfig", AudioConfigAdapter.CODEC);
        this.config = this.audioConfigFile.load().join();
        this.engine = new AudioEngine(this.config);
    }

    @Override
    protected void setup() {
        instance = this;
        engine.setWebVisualizer(webVisualizer);
        webVisualizer.setVolumeCallback(engine::setVolume);
        webVisualizer.start();
        webVisualizer.waitForConnection();

        engine.start();

        engine.playSong(
                FINAL_AUDIO,
                () -> {
                    getLogger().atInfo().log("OpenAL ha comenzado la reproducción nativa con éxito.");
                },
                config.getGeneral().getDelayedTaskInMs(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    protected void shutdown() {
        engine.shutdown();
    }

    public void reloadConfig() {
        AudioConfig newConfig = audioConfigFile.load().join();
        this.config = newConfig;
        engine.reloadConfiguration(newConfig);
        getLogger().atInfo().log("Configuración recargada");
    }

    public static Main getInstance(){
        return instance;
    }
}