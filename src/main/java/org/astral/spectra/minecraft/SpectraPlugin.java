package org.astral.spectra.minecraft;

import org.astral.spectra.minecraft.commands.RegisterCommands;
import org.astral.spectra.minecraft.events.RegisterEvents;
import org.astral.spectra.minecraft.pack.PackServer;
import org.astral.spectra.minecraft.pack.ResourcePackManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpectraPlugin extends JavaPlugin {

    private ResourcePackManager packManager;
    private PackServer packServer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String serverIp = getConfig().getString("web-server.ip", "127.0.0.1");
        int serverPort = getConfig().getInt("web-server.port", 8080);

        this.packManager = new ResourcePackManager(this);
        this.packManager.buildPack();

        this.packServer = new PackServer(this, packManager);
        this.packServer.start(serverPort);

        RegisterEvents.registerAll(this, serverIp);
        RegisterCommands.registerAll(this);

        getLogger().info("Spectra Plugin activado. ¡Sistema Dinámico Listo!");
        getLogger().info("Hosteando pack en: http://" + serverIp + ":" + serverPort);
    }

    @Override
    public void onDisable() {
        if (packServer != null) {
            packServer.stop();
        }
        getLogger().info("Spectra Plugin desactivado.");
    }

    public ResourcePackManager getPackManager(){
        return packManager;
    }
}