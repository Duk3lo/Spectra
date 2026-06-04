package org.astral.spectra.minecraft.commands.command;

import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class PlayMusicCommand implements CommandExecutor, TabCompleter {
    private final Plugin plugin;

    public PlayMusicCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo los jugadores pueden escuchar música.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§c¡Falta el nombre del audio!");
            player.sendMessage("§eUso correcto: §f/playmusic <nombre>");
            player.sendMessage("§7Usa la tecla TAB para ver los audios disponibles.");
            return true;
        }

        String soundName = args[0].toLowerCase().replaceAll("[^a-z0-9_]", "");
        NamespacedKey soundKey = new NamespacedKey("astral", soundName);
        Sound customSound = Sound.sound(soundKey, Sound.Source.MASTER, 1.0f, 1.0f);

        player.playSound(customSound);
        player.sendMessage("§a🎶 Reproduciendo: §f" + soundName);

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        List<String> completados = new ArrayList<>();

        if (args.length == 1) {
            Path soundsDir = plugin.getDataFolder().toPath().resolve("sounds");

            if (Files.exists(soundsDir)) {
                try (Stream<Path> paths = Files.list(soundsDir)) {
                    paths.filter(p -> p.toString().toLowerCase().endsWith(".ogg"))
                            .map(p -> p.getFileName().toString().replace(".ogg", ""))
                            .filter(name -> name.startsWith(args[0].toLowerCase()))
                            .forEach(completados::add);
                } catch (IOException e) {
                    plugin.getLogger().warning("Error leyendo carpeta para autocompletado: " + e.getMessage());
                }
            }
        }
        return completados;
    }
}