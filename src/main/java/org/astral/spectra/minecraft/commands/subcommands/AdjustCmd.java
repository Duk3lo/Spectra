package org.astral.spectra.minecraft.commands.subcommands;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class AdjustCmd implements SubCommand {
    private final SpectraPlugin plugin;

    public AdjustCmd(SpectraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NonNull String getName() { return "adjust"; }

    @Override
    public void execute(@NonNull CommandSender sender, String @NonNull [] args) {
        if (!sender.hasPermission("spectra.admin")) {
            sender.sendMessage("§cNo tienes permisos.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§eUso: /spectra adjust <milisegundos> (Ej: 500 o -500)");
            return;
        }

        if (!AudioAPI.isPlaying()) {
            sender.sendMessage("§cNo hay ninguna canción reproduciéndose.");
            return;
        }

        try {
            int msOffset = Integer.parseInt(args[1]);
            long newTimeMs = (long) (AudioAPI.getCurrentTime() * 1000) + msOffset;
            if (newTimeMs < 0) newTimeMs = 0;

            plugin.getAudioEngine().seekTo(newTimeMs);

            sender.sendMessage("§a[Spectra] Sincronización visual ajustada en §e" + msOffset + "ms§a.");
            sender.sendMessage("§7Tiempo actual del motor: " + AudioAPI.getCurrentTimeFormatted());

        } catch (NumberFormatException e) {
            sender.sendMessage("§cPor favor, ingresa un número válido en milisegundos.");
        } catch (Exception e) {
            sender.sendMessage("§cOcurrió un error al ajustar el tiempo.");
        }
    }

    @Override public List<String> tabComplete(CommandSender sender, String @NonNull [] args) {
        if (args.length == 2) {
            return List.of("500", "-500", "1000", "-1000");
        }
        return List.of();
    }
}