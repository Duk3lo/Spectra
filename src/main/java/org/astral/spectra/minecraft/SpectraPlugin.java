package org.astral.spectra.minecraft;

import org.astral.spectra.minecraft.commands.Command;
import org.astral.spectra.minecraft.events.JoinListener;
import org.astral.spectra.minecraft.pack.PackServer;
import org.astral.spectra.minecraft.pack.ResourcePackManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SpectraPlugin extends JavaPlugin {

    private ResourcePackManager packManager;
    private PackServer packServer;

    @Override
    public void onEnable() {
        super.onEnable();

        packManager = new ResourcePackManager(this);
        packManager.buildPack();

        packServer = new PackServer(this, packManager);
        packServer.start(8080);

        // AQUÍ ESTÁ EL CAMBIO: Pasamos 'this' al comando
        Command myCommand = new Command(this);
        if (getCommand("playmusic") != null) {
            Objects.requireNonNull(getCommand("playmusic")).setExecutor(myCommand);
            Objects.requireNonNull(getCommand("playmusic")).setTabCompleter(myCommand);
        }

        // Mantén tu IP como la tienes, ya que veo que sí está descargando el pack
        getServer().getPluginManager().registerEvents(new JoinListener(packManager, "127.0.0.1"), this);

        getLogger().info("Spectra Plugin activado. ¡Sistema Dinámico Listo!");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (packServer != null) {
            packServer.stop();
        }
    }
}