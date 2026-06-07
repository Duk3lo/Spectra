package org.astral.spectra.minecraft.commands;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.bukkit.command.PluginCommand;

public final class RegisterCommands {

    public static void registerAll(SpectraPlugin plugin) {
        SpectraCommand adminCmd = new SpectraCommand(plugin);
        PluginCommand spectraCommand = plugin.getCommand("spectra");

        if (spectraCommand != null) {
            spectraCommand.setExecutor(adminCmd);
            spectraCommand.setTabCompleter(adminCmd);
        } else {
            plugin.getLogger().severe("Error! The 'spectra' command is not defined in plugin.yml");
        }
    }
}