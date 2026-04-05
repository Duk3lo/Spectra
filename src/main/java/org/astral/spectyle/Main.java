package org.astral.spectyle;

import org.astral.spectyle.audio.engine.AudioEngine;
import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.logging.ConsoleLogger;
import org.astral.spectyle.logging.EngineLogger;
import org.astral.spectyle.ui.AudioControlFrame;
import org.astral.spectyle.web.WebVisualizer;

import javax.swing.*;

public class Main {

    private static final String finalAudio = "musics/mind.ogg";
    private static final EngineLogger logger = new ConsoleLogger();
    private static final WebVisualizer webVisualizer =
            new WebVisualizer("OpenAL (AudioSpectrum)", 8080, logger);

    public static void main(String[] args) {
        AudioConfig config = new AudioConfig();
        AudioEngine engine = new AudioEngine(config, logger);

        logger.info("\u001B[35m[Main] Starting Audio Engine\u001B[0m");

        engine.setWebVisualizer(webVisualizer);
        webVisualizer.setVolumeCallback(engine::setVolume);

        webVisualizer.start();
        webVisualizer.waitForConnection();
        AudioControlFrame.setDefaultWebUrl(webVisualizer.getUrl());

        engine.start();

        SwingUtilities.invokeLater(() -> {
            AudioControlFrame frame = new AudioControlFrame(engine, logger);
            frame.setVisible(true);
        });

        engine.playResource(finalAudio);

        engine.waitForExit();
    }
}