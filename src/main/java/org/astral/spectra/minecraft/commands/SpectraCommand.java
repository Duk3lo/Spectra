package org.astral.spectra.minecraft.commands;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.commands.subcommands.*;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

public final class SpectraCommand implements CommandExecutor, TabCompleter {
    public static final Set<UUID> playersListen = new HashSet<>();
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public SpectraCommand(SpectraPlugin plugin) {
        register(new PlayCmd(plugin));
        register(new StopCmd(plugin));
        register(new AdjustCmd(plugin));
        register(new ImportCmd(plugin));
        register(new ReloadCmd(plugin));
        register(new VisualsCmd(plugin));
        register(new RemoveSoundCmd(plugin));
    }

    private void register(SubCommand cmd) { subCommands.put(cmd.getName().toLowerCase(), cmd); }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        if (args.length == 0 || !subCommands.containsKey(args[0].toLowerCase())) {
            sender.sendMessage("§eAvailable commands: §f" + subCommands.keySet());
            return true;
        }
        subCommands.get(args[0].toLowerCase()).execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        if (args.length == 1) return subCommands.keySet().stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        SubCommand sub = subCommands.get(args[0].toLowerCase());
        return sub != null ? sub.tabComplete(sender, args) : List.of();
    }
}