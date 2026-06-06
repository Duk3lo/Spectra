package org.astral.spectra.minecraft.commands.subcommands;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class AdjustCmd implements SubCommand {
    private final SpectraPlugin plugin;
    public AdjustCmd(SpectraPlugin plugin) { this.plugin = plugin; }


    @Override public @NonNull String getName() { return "adjust"; }

    @Override
    public void execute(CommandSender sender, String @NonNull [] args) {
        if (args.length < 2 || !AudioAPI.isPlaying()) return;
        try {
            int ms = Integer.parseInt(args[1]);
            plugin.getAudioEngine().seekTo((long) (AudioAPI.getCurrentTime() * 1000) + ms);
            sender.sendMessage("§eTiempo ajustado: " + AudioAPI.getCurrentTimeFormatted());
        } catch (Exception e) { sender.sendMessage("§cError."); }
    }

    @Override public List<String> tabComplete(CommandSender sender, String @NonNull [] args) {
        return args.length == 2 ? List.of("5000", "-5000", "10000") : List.of();
    }
}