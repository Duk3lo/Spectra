package org.astral.spectra.minecraft.commands.subcommands;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.astral.spectra.minecraft.events.visuals.VisualizerData;
import org.astral.spectra.minecraft.events.visuals.VisualizerManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.stream.Collectors;

public final class VisualsCmd implements SubCommand {
    private final SpectraPlugin plugin;

    public VisualsCmd(SpectraPlugin plugin) { this.plugin = plugin; }

    @Override public @NonNull String getName() { return "visuals"; }

    @Override
    public void execute(@NonNull CommandSender sender, String[] args) {
        if (!sender.hasPermission("spectra.use")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§eUsage: /spectra visuals <preset|create|stop|list|remove> ...");
            return;
        }

        String sub = args[1].toLowerCase();

        if (sub.equals("list")) {
            sender.sendMessage("§eActive Visualizers:");
            VisualizerManager.getAllVisualizers().forEach(v ->
                    sender.sendMessage("§7- §f" + v.getName() + " §e[" + v.getPresetName() + "] " + (v.isPersistent() ? "§a(Saved)" : "§b(Temporary)")));
            return;
        }

        if (sub.equals("stop") || sub.equals("remove")) {
            if (args.length > 2) {
                VisualizerManager.stop(args[2]);
                sender.sendMessage("§cVisualizer '" + args[2] + "' stopped.");
            } else {
                VisualizerManager.stopAll();
                sender.sendMessage("§cAll temporary visualizers stopped.");
            }
            return;
        }

        boolean isPermanent = sub.equals("create");
        if (isPermanent && !sender.hasPermission("spectra.admin")) {
            sender.sendMessage("§cOnly admins can create permanent visualizers.");
            return;
        }

        int offset = isPermanent ? 1 : 0;

        if (isPermanent && args.length < 3) {
            sender.sendMessage("§eUsage: /spectra visuals create <preset> [targets/name] ...");
            return;
        }

        String presetKey = isPermanent ? args[2] : sub;
        VisualPreset base = plugin.getConfigManager().getVisualsConfig().getPresetsMap().get(presetKey.toLowerCase());

        if (base == null) {
            sender.sendMessage("§cPreset '" + presetKey + "' does not exist.");
            return;
        }

        VisualPreset custom = new VisualPreset(base);
        Set<UUID> targets = new HashSet<>();
        int currentIndex = 2 + offset;

        String mode = base.getRenderMode().toLowerCase();
        boolean isPlayerCentric = mode.equals("walk") || mode.equals("elytra") || mode.equals("player");

        if (isPlayerCentric) {
            boolean foundTarget = false;
            if (args.length > currentIndex) {
                String selector = args[currentIndex];
                if (selector.startsWith("@")) {
                    try {
                        for (Entity e : Bukkit.selectEntities(sender, selector)) {
                            if (e instanceof Player p) targets.add(p.getUniqueId());
                        }
                        foundTarget = true;
                    } catch (Exception ignored) {}
                } else {
                    Player targetPlayer = Bukkit.getPlayer(selector);
                    if (targetPlayer != null) {
                        targets.add(targetPlayer.getUniqueId());
                        foundTarget = true;
                    }
                }
            }

            if (targets.isEmpty()) {
                targets.add(player.getUniqueId());
            }

            if (foundTarget) {
                currentIndex++;
            }
        }
        Set<String> takenNames = VisualizerManager.getAllVisualizers().stream()
                .map(v -> v.getName().toLowerCase())
                .collect(Collectors.toSet());
        String requestedName = (args.length > currentIndex) ? args[currentIndex] : presetKey;
        String customName = requestedName;
        int counter = 1;

        while (takenNames.contains(customName.toLowerCase())) {
            customName = requestedName + "_" + counter;
            counter++;
        }
        currentIndex++;

        if (args.length > currentIndex) custom.setShape(args[currentIndex].toLowerCase());
        currentIndex++;

        if (args.length > currentIndex) custom.setMaxHeight(tryParseInt(args[currentIndex], custom.getMaxHeight()));
        currentIndex++;

        if (args.length > currentIndex) custom.setSpacing(tryParseDouble(args[currentIndex], custom.getSpacing()));
        currentIndex++;

        if (args.length > currentIndex) custom.setRadius(tryParseDouble(args[currentIndex], custom.getRadius()));
        currentIndex++;

        Location loc = player.getLocation();
        if (args.length > currentIndex + 2) {
            double x = parseCoord(args[currentIndex], loc.getX());
            double y = parseCoord(args[currentIndex + 1], loc.getY());
            double z = parseCoord(args[currentIndex + 2], loc.getZ());
            loc = new Location(player.getWorld(), x, y, z, loc.getYaw(), 0);
        }

        String finalName = VisualizerManager.start(customName, presetKey, custom, loc, isPermanent, targets);

        if (isPlayerCentric) {
            sender.sendMessage("§aVisualizer started: §f" + finalName + " §a(Targets: " + targets.size() + ")");
        } else {
            sender.sendMessage("§aVisualizer started: §f" + finalName);
        }
    }

    private double parseCoord(@NonNull String val, double rel) {
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

        String sub = args[1].toLowerCase();

        if (args.length == 3) {
            if (sub.equals("create")) {
                return new ArrayList<>(plugin.getConfigManager().getVisualsConfig().getPresetsMap().keySet());
            } else if (sub.equals("stop") || sub.equals("remove")) {
                return VisualizerManager.getAllVisualizers().stream()
                        .map(VisualizerData::getName)
                        .collect(Collectors.toList());
            }
        }

        boolean isPermanent = sub.equals("create");
        int offset = isPermanent ? 1 : 0;
        String presetKey = isPermanent ? args[2].toLowerCase() : sub;

        VisualPreset preset = plugin.getConfigManager().getVisualsConfig().getPresetsMap().get(presetKey);
        if (preset == null) return List.of();

        String mode = preset.getRenderMode().toLowerCase();
        boolean isPlayerCentric = mode.equals("walk") || mode.equals("elytra") || mode.equals("player");

        int indexToCheck = 3 + offset;

        if (args.length == indexToCheck) {
            if (isPlayerCentric) {
                List<String> list = new ArrayList<>(List.of("@a", "@p", "@r"));
                list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                return list;
            } else {
                return List.of("<name>");
            }
        }

        if (isPlayerCentric) {
            String potentialSelector = args[indexToCheck - 1];
            if (potentialSelector.startsWith("@") || Bukkit.getPlayer(potentialSelector) != null) {
                indexToCheck++;
            }
        }

        if (args.length == indexToCheck) return List.of("<name>");
        if (args.length == indexToCheck + 1) return List.of("<shape>");
        if (args.length == indexToCheck + 2) return List.of("<height>");
        if (args.length == indexToCheck + 3) return List.of("<spacing>");
        if (args.length == indexToCheck + 4) return List.of("<radius>");
        if (args.length == indexToCheck + 5) return List.of("<x>");
        if (args.length == indexToCheck + 6) return List.of("<y>");
        if (args.length == indexToCheck + 7) return List.of("<z>");

        return List.of();
    }
}