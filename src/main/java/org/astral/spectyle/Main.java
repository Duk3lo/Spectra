package org.astral.spectyle;

import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.logging.ConsoleLogger;
import org.astral.spectyle.logging.EngineLogger;
import org.astral.spectyle.web.WebVisualizer;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String finalAudio = "musics/mind.ogg";

    private static final EngineLogger logger = new ConsoleLogger();

    private static final WebVisualizer webVisualizer = new WebVisualizer("OpenAL (AudioSpectrum)", 8080, logger);

    public static void main(String[] args) {
        AudioConfig config = new AudioConfig();
        AudioEngine engine = new AudioEngine(config, logger);

        logger.info("\u001B[35m[Main] Iniciando Motor de Audio: Nuevo sistema modular\u001B[0m");

        engine.setWebVisualizer(webVisualizer);
        webVisualizer.setVolumeCallback(engine::setVolume);
        webVisualizer.start();
        webVisualizer.waitForConnection();
        engine.start();

        engine.playResource(finalAudio, () -> {
            logger.info("\u001B[32m[Main] OpenAL ha comenzado la reproducción nativa con éxito.\u001B[0m");
            logger.info("Accion para otra cosa");
        }, 227, TimeUnit.MILLISECONDS);

        engine.waitForExit();
    }
}