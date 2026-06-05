package org.astral.spectra.minecraft.commands.subcommands;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.events.visuals.VisualizerManager;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ReloadCmd implements SubCommand {
    private final SpectraPlugin plugin;
    public ReloadCmd(SpectraPlugin plugin) { this.plugin = plugin; }

    @Override public String getName() { return "reload"; }

    @Override
    public void execute(@NonNull CommandSender sender, String[] args) {
        plugin.getConfigManager().loadAllConfigs();
        if (plugin.getAudioEngine() != null) {
            plugin.getAudioEngine().reloadConfiguration(plugin.getConfigManager().getAudioConfig());
        }
        plugin.getPackManager().buildPack();
        VisualizerManager.init(plugin);
        sender.sendMessage("§a✅ Spectra: Configuración, Presets y Resource Pack recargados correctamente.");
    }
    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}