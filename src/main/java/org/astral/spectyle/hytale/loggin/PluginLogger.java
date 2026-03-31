package org.astral.spectyle.hytale.loggin;

import org.astral.spectyle.logging.EngineLogger;
import org.astral.spectyle.hytale.SpectylePlugin;

public final class PluginLogger implements EngineLogger {

    private final SpectylePlugin plugin;

    public PluginLogger(SpectylePlugin plugin) {
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