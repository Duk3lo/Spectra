package org.astral.spectra.hytale.loggin;

import org.astral.spectra.logging.EngineLogger;
import org.astral.spectra.hytale.SpectraPlugin;

public final class PluginLogger implements EngineLogger {

    private final SpectraPlugin plugin;

    public PluginLogger(SpectraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void info(String msg) {
        plugin.getLogger().atInfo().log(msg);
    }

    @Override
    public void warn(String msg) {
        plugin.getLogger().atWarning().log(msg);
    }

    @Override
    public void error(String msg) {
        plugin.getLogger().atSevere().log(msg);
    }

    @Override
    public void error(String msg, Throwable throwable) {
        plugin.getLogger().atSevere().withCause(throwable).log(msg);
    }
}