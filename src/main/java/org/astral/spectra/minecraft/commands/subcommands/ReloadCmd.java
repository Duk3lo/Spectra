package org.astral.spectra.minecraft.commands.subcommands;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.events.visuals.VisualizerManager;
import org.astral.spectra.minecraft.utils.PackUtils;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class ReloadCmd implements SubCommand {
    private final SpectraPlugin plugin;
    private long lastReload = 0;

    public ReloadCmd(SpectraPlugin plugin) { this.plugin = plugin; }

    @Override public @NonNull String getName() { return "reload"; }

    @Override
    public void execute(@NonNull CommandSender sender, String[] args) {
        if (!sender.hasPermission("spectra.admin")) return;

        long now = System.currentTimeMillis();
        if (now - lastReload < 5000) return;
        lastReload = now;

        try {
            plugin.getConfigManager().loadAllConfigs();
            if (plugin.getAudioEngine() != null) {
                plugin.getAudioEngine().reloadConfiguration(plugin.getConfigManager().getAudioConfig());
            }

            // Evaluamos si el pack realmente detectó cambios
            boolean packUpdated = plugin.getPackManager().buildPack();

            VisualizerManager.init(plugin);

            // Solo reenviamos el pack si se generó uno nuevo
            if (packUpdated) {
                PackUtils.sendPackToAll();
                sender.sendMessage("§a✅ Spectra recargado. §eSe detectaron nuevos audios (Paquete reenviado).");
            } else {
                sender.sendMessage("§a✅ Spectra recargado. §7(Audios sin cambios, no se reenvió paquete).");
            }

        } catch (Exception e) {
            sender.sendMessage("§cError al recargar Spectra.");
        }
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}