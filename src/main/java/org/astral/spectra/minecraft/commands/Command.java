package org.astral.spectra.minecraft.commands;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class Command implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        // 1. Verificamos que quien ejecuta el comando sea un jugador (la consola no puede escuchar sonidos)
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        // 2. Definimos la Key del sonido.
        // "astral" es el namespace (la carpeta en el resource pack)
        // "custom_music" es el nombre del evento de sonido definido en sounds.json
        Key soundKey = Key.key("astral", "custom_music");

        // 3. Creamos el objeto Sound de la API de Adventure
        // Sound.Source.MASTER indica el canal de volumen (puede ser MUSIC, RECORD, etc)
        // 1.0f es el volumen, 1.0f es el pitch (velocidad/tono)
        Sound customSound = Sound.sound(soundKey, Sound.Source.MASTER, 1.0f, 1.0f);

        // 4. Reproducimos el sonido al jugador
        player.playSound(customSound);
        player.sendMessage("¡Reproduciendo música custom!");

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
        return List.of(); // Sin autocompletado por ahora
    }
}