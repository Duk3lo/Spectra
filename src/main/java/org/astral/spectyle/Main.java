package org.astral.spectyle;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.web.WebVisualizer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class Main{

    private static final String finalAudio = "src/main/resources/musics/miss.ogg";
    private static final WebVisualizer webVisualizer = new WebVisualizer("OpenAL (AudioSpectrum)", 8080);




    public static void main(String[] args) {
        AudioConfig config = new AudioConfig();
        AudioEngine engine = new AudioEngine(config);

        System.out.println("\u001B[35m[Main] Iniciando Motor de Audio: Nuevo sistema modular\u001B[0m");

        engine.setWebVisualizer(webVisualizer);

        webVisualizer.setVolumeCallback(engine::setVolume);
        webVisualizer.start();
        webVisualizer.waitForConnection();

        engine.start();

        engine.playSong(finalAudio, () -> {
            System.out.println("\u001B[32m[Main] OpenAL ha comenzado la reproducción nativa con éxito.\u001B[0m");
            System.out.println("Accion para otra cosa");
        }, config.getGeneral().getDelayedTaskInMs(), TimeUnit.MILLISECONDS);

        engine.waitForExit();
    }


}