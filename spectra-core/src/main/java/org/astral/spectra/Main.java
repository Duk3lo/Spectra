package org.astral.spectra;

import org.astral.spectra.audio.engine.AudioEngine;
import org.astral.spectra.config.AudioConfig;
import org.astral.spectra.logging.ConsoleLogger;
import org.astral.spectra.logging.EngineLogger;
import org.astral.spectra.ui.AudioControlFrame;
import org.astral.spectra.web.WebVisualizer;

import javax.swing.*;

public class Main {

    private static final String finalAudio = "musics/mind.ogg";
    private static final EngineLogger logger = new ConsoleLogger();
    private static final WebVisualizer webVisualizer =
            new WebVisualizer("OpenAL (AudioSpectrum)", 8080, logger);

    static void main() {
        AudioConfig config = new AudioConfig();

        AudioEngine engine = new AudioEngine(config, logger);

        logger.info("\u001B[35m[Main] Starting Audio Engine\u001B[0m");
        webVisualizer.setAutoOpen(config.getGeneral().isAutoOpenBrowser());

        engine.setWebVisualizer(webVisualizer);
        webVisualizer.setVolumeCallback(engine::setVolume);

        webVisualizer.start();

        if (config.getGeneral().isAutoOpenBrowser()) {
            webVisualizer.waitForConnection();
        }

        AudioControlFrame.setDefaultWebUrl(webVisualizer.getUrl());

        engine.start();

        SwingUtilities.invokeLater(() -> {
            AudioControlFrame frame = new AudioControlFrame(engine, logger);
            frame.setVisible(true);
        });
        System.out.println();
        engine.playResource(finalAudio);

        engine.waitForExit();
    }
}