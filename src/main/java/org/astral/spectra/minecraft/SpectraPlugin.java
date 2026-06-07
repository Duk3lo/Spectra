package org.astral.spectra.minecraft;

import org.astral.spectra.audio.engine.AudioEngine;
import org.astral.spectra.minecraft.commands.RegisterCommands;
import org.astral.spectra.minecraft.config.SpectraConfigManager;
import org.astral.spectra.minecraft.events.RegisterEvents;
import org.astral.spectra.minecraft.events.visuals.VisualizerManager;
import org.astral.spectra.minecraft.events.visuals.RhythmTaskSystem;
import org.astral.spectra.minecraft.loggin.PluginLogger;
import org.astral.spectra.minecraft.pack.PackServer;
import org.astral.spectra.minecraft.pack.ResourcePackManager;
import org.astral.spectra.web.WebVisualizer;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpectraPlugin extends JavaPlugin {

    private static SpectraPlugin INSTANCE;
    private SpectraConfigManager configManager;
    private ResourcePackManager packManager;
    private PackServer packServer;
    private AudioEngine engine;

    @Override
    public void onEnable() {
        INSTANCE = this;
        PluginLogger pluginLogger = new PluginLogger(this);

        this.configManager = new SpectraConfigManager(this);
        this.configManager.loadAllConfigs();

        this.packManager = new ResourcePackManager(this);
        this.packManager.buildPack();
        this.packServer = new PackServer(this, packManager);
        this.packServer.start(configManager.getServerPort());

        WebVisualizer webVisualizer = new WebVisualizer("OpenAL - Spectra (Minecraft)", configManager.getWebVisualizerPort(), pluginLogger);
        this.engine = new AudioEngine(configManager.getAudioConfig(), pluginLogger);
        this.engine.setWebVisualizer(webVisualizer);
        webVisualizer.setVolumeCallback(engine::setVolume);

        webVisualizer.start();
        this.engine.start();

        VisualizerManager.init(this);

        RegisterEvents.registerAll(this);
        RegisterCommands.registerAll(this);

        new RhythmTaskSystem(this).start();

        getLogger().info("Spectra loaded successfully.");
    }

    @Override
    public void onDisable() {
        if (packServer != null) packServer.stop();
        if (engine != null) engine.shutdown();
        VisualizerManager.stopAll();
    }

    public static SpectraPlugin getInstance() { return INSTANCE; }
    public SpectraConfigManager getConfigManager() { return configManager; }
    public ResourcePackManager getPackManager() { return packManager; }
    public AudioEngine getAudioEngine() { return engine; }
}