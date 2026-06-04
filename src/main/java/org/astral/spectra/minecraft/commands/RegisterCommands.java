package org.astral.spectra.minecraft.commands;

import org.astral.spectra.minecraft.commands.command.PlayMusicCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public final class RegisterCommands {

    public static void registerAll(JavaPlugin plugin) {
        // Instancia del comando de música
        PlayMusicCommand playMusicCmd = new PlayMusicCommand(plugin);

        // Registro seguro en el sistema de comandos de Minecraft
        if (plugin.getCommand("playmusic") != null) {
            Objects.requireNonNull(plugin.getCommand("playmusic")).setExecutor(playMusicCmd);
            Objects.requireNonNull(plugin.getCommand("playmusic")).setTabCompleter(playMusicCmd);
        } else {
            plugin.getLogger().severe("¡Error Crítico! El comando 'playmusic' no está en el plugin.yml");
        }

        // Aquí podrías registrar otros comandos en el futuro
        // plugin.getCommand("spectra").setExecutor(new AdminCommand(plugin));
    }
}