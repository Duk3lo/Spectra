package org.astral.spectra.minecraft.commands.subcommands;

import net.kyori.adventure.sound.Sound;
import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.commands.SpectraCommand;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PlayCmd implements SubCommand {
    private final SpectraPlugin plugin;

    public PlayCmd(SpectraPlugin plugin) { this.plugin = plugin; }

    @Override public @NonNull String getName() { return "play"; }

    @Override
    public void execute(@NonNull CommandSender sender, String @NonNull [] args) {
        if (!sender.hasPermission("spectra.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /spectra play <sound> [player|@a]");
            return;
        }
        if (AudioAPI.isPlaying()) {
            sender.sendMessage("§cEngine is already playing.");
            return;
        }

        String name = args[1].toLowerCase().replaceAll("[^a-z0-9_]", "");
        Path path = plugin.getDataFolder().toPath().resolve("sounds").resolve(name + ".ogg");

        if (!Files.exists(path)) {
            sender.sendMessage("§cSound '§f" + name + "§c' does not exist.");
            return;
        }

        plugin.getAudioEngine().playSong(path);
        SpectraCommand.playersListen.clear();

        NamespacedKey key = new NamespacedKey("astral", name);
        Sound sound = Sound.sound(key, Sound.Source.MASTER, 1f, 1f);
        String target = args.length >= 3 ? args[2] : "@a";

        if (target.equalsIgnoreCase("@a")) {
            Bukkit.getOnlinePlayers().forEach(p -> {
                p.playSound(sound);
                SpectraCommand.playersListen.add(p.getUniqueId());
            });
            sender.sendMessage("§a🎶 Playing for everyone.");
        } else {
            Player p = Bukkit.getPlayer(target);
            if (p != null) {
                p.playSound(sound);
                SpectraCommand.playersListen.add(p.getUniqueId());
                sender.sendMessage("§a🎶 Playing for " + p.getName());
            } else {
                sender.sendMessage("§cPlayer not found.");
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String @NonNull [] args) {
        if (args.length == 2) {
            try (var s = Files.list(plugin.getDataFolder().toPath().resolve("sounds"))) {
                return s.map(p -> p.getFileName().toString().replace(".ogg", "")).filter(n -> n.startsWith(args[1])).toList();
            } catch (Exception e) { return List.of(); }
        }
        if (args.length == 3) return List.of("@a");
        return List.of();
    }
}