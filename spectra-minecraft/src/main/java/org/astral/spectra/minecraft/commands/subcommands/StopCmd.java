package org.astral.spectra.minecraft.commands.subcommands;

import net.kyori.adventure.sound.SoundStop;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.commands.SpectraCommand;
import org.astral.spectra.minecraft.events.visuals.VisualizerManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

public final class StopCmd implements SubCommand {
    private final SpectraPlugin plugin;
    public StopCmd(SpectraPlugin plugin) { this.plugin = plugin; }

    @Override public @NonNull String getName() { return "stop"; }

    @Override
    public void execute(@NonNull CommandSender sender, String[] args) {
        if (!sender.hasPermission("spectra.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        plugin.getAudioEngine().stopSong();
        VisualizerManager.stopAll();

        for (UUID uuid : SpectraCommand.playersListen) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.stopSound(SoundStop.all());
            }
        }

        SpectraCommand.playersListen.clear();
        sender.sendMessage("§c⏹ Engine and music stopped.");
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}