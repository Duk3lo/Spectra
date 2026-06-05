package org.astral.spectra.minecraft.loggin;

import org.astral.spectra.logging.EngineLogger;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class PluginLogger implements EngineLogger {

    private final Logger logger;

    public PluginLogger(@NonNull Plugin plugin) {
        this.logger = plugin.getLogger();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void warn(String msg) {
        logger.warning(msg);
    }

    @Override
    public void error(String msg) {
        logger.severe(msg);
    }

    @Override
    public void error(String msg, Throwable throwable) {
        logger.log(Level.SEVERE, msg, throwable);
    }
}