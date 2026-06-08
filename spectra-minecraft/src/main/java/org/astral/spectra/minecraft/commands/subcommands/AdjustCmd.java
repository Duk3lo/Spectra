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
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§eUsage: /spectra adjust <milliseconds> (e.g. 500 or -500)");
            return;
        }

        if (!AudioAPI.isPlaying()) {
            sender.sendMessage("§cNo song is currently playing.");
            return;
        }

        try {
            int msOffset = Integer.parseInt(args[1]);
            long newTimeMs = (long) (AudioAPI.getCurrentTime() * 1000) + msOffset;
            if (newTimeMs < 0) newTimeMs = 0;

            plugin.getAudioEngine().seekTo(newTimeMs);

            sender.sendMessage("§a[Spectra] Visual sync adjusted by §e" + msOffset + "ms§a.");
            sender.sendMessage("§7Current engine time: " + AudioAPI.getCurrentTimeFormatted());

        } catch (NumberFormatException e) {
            sender.sendMessage("§cPlease provide a valid number in milliseconds.");
        } catch (Exception e) {
            sender.sendMessage("§cAn error occurred while adjusting the time.");
        }
    }

    @Override public List<String> tabComplete(CommandSender sender, String @NonNull [] args) {
        if (args.length == 2) {
            return List.of("500", "-500", "1000", "-1000");
        }
        return List.of();
    }
}