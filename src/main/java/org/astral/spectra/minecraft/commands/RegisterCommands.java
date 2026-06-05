package org.astral.spectra.minecraft.commands;

import org.astral.spectra.minecraft.SpectraPlugin;

import java.util.Objects;

public final class RegisterCommands {

    public static void registerAll(SpectraPlugin plugin) {
        SpectraAdminCommand adminCmd = new SpectraAdminCommand(plugin);

        if (plugin.getCommand("spectra") != null) {
            Objects.requireNonNull(plugin.getCommand("spectra")).setExecutor(adminCmd);
            Objects.requireNonNull(plugin.getCommand("spectra")).setTabCompleter(adminCmd);
        } else {
            plugin.getLogger().severe("¡Error! El comando 'spectra' no está en el plugin.yml");
        }
    }
}