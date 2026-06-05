package org.astral.spectra.minecraft.commands.subcommands;

import net.kyori.adventure.sound.SoundStop;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.commands.SpectraAdminCommand;
import org.astral.spectra.minecraft.events.visuals.VisualizerManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class StopCmd implements SubCommand {
    private final SpectraPlugin plugin;
    public StopCmd(SpectraPlugin plugin) { this.plugin = plugin; }

    @Override public String getName() { return "stop"; }

    @Override
    public void execute(@NonNull CommandSender sender, String[] args) {
        plugin.getAudioEngine().stopSong();
        VisualizerManager.stopAll();
        Bukkit.getOnlinePlayers().forEach(p -> p.stopSound(SoundStop.all()));
        SpectraAdminCommand.playersListen.clear();
        sender.sendMessage("§c⏹ Motor y música detenidos.");
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}