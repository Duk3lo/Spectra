package org.astral.spectra.minecraft.commands.subcommands;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.astral.spectra.minecraft.events.visuals.VisualizerData;
import org.astral.spectra.minecraft.events.visuals.VisualizerManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import java.util.*;

public class VisualsCmd implements SubCommand {
    private final SpectraPlugin plugin;

    public VisualsCmd(SpectraPlugin plugin) { this.plugin = plugin; }

    @Override public String getName() { return "visuals"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo jugadores pueden usar este comando.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§eUso: /spectra visuals <preset|create|stop|list|remove> ...");
            return;
        }

        String sub = args[1].toLowerCase();

        if (sub.equals("list")) {
            sender.sendMessage("§eVisualizadores activos en el mundo:");
            VisualizerManager.getAllVisualizers().forEach(v ->
                    sender.sendMessage("§7- §f" + v.getName() + " §e[" + v.getPresetName() + "] " + (v.isPersistent() ? "§a(Guardado)" : "§b(Temporal)")));
            return;
        }

        if (sub.equals("stop") || sub.equals("remove")) {
            if (args.length > 2) {
                VisualizerManager.stop(args[2]);
                sender.sendMessage("§cVisualizador '" + args[2] + "' detenido y removido.");
            } else {
                VisualizerManager.stopAll();
                sender.sendMessage("§cTodos los visualizadores temporales han sido detenidos.");
            }
            return;
        }

        boolean isPermanent = sub.equals("create");
        int offset = isPermanent ? 1 : 0;

        if (isPermanent && args.length < 3) {
            sender.sendMessage("§eUso: /spectra visuals create <preset> [nombre] [forma] [altura] [espaciado] [radio] [x y z]");
            return;
        }

        String presetKey = isPermanent ? args[2] : sub;
        VisualPreset base = plugin.getConfigManager().getVisualsConfig().getPresetsMap().get(presetKey.toLowerCase());

        if (base == null) {
            sender.sendMessage("§cEl preset '" + presetKey + "' no existe en presets.yml");
            return;
        }

        VisualPreset custom = new VisualPreset(base);

        // Parseo de argumentos opcionales
        String customName = (args.length > 2 + offset) ? args[2 + offset] : (isPermanent ? "Saved_" : "Vis_") + System.currentTimeMillis();
        if (args.length > 3 + offset) custom.setShape(args[3 + offset].toLowerCase());
        if (args.length > 4 + offset) custom.setMaxHeight(tryParseInt(args[4 + offset], custom.getMaxHeight()));
        if (args.length > 5 + offset) custom.setSpacing(tryParseDouble(args[5 + offset], custom.getSpacing())); // USO DE SETSPACING
        if (args.length > 6 + offset) custom.setRadius(tryParseDouble(args[6 + offset], custom.getRadius()));   // USO DE SETRADIUS

        Location loc = player.getLocation();
        if (args.length >= 10 + offset) { // x y z están al final
            double x = parseCoord(args[7 + offset], loc.getX());
            double y = parseCoord(args[8 + offset], loc.getY());
            double z = parseCoord(args[9 + offset], loc.getZ());
            loc = new Location(player.getWorld(), x, y, z, loc.getYaw(), 0);
        }

        String finalName = VisualizerManager.start(customName, presetKey, custom, loc, isPermanent);
        sender.sendMessage("§aVisualizador " + (isPermanent ? "§lPERMANENTE" : "§bTEMPORAL") + " §ainiciado: §f" + finalName);
    }

    private double parseCoord(String val, double rel) {
        if (val.startsWith("~")) return rel + (val.length() > 1 ? Double.parseDouble(val.substring(1)) : 0);
        return Double.parseDouble(val);
    }

    private int tryParseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private double tryParseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String @NonNull [] args) {
        if (args.length == 2) {
            List<String> list = new ArrayList<>(List.of("create", "stop", "list", "remove"));
            list.addAll(plugin.getConfigManager().getVisualsConfig().getPresetsMap().keySet());
            return list;
        }
        return List.of();
    }
}