package org.astral.spectra.minecraft.commands.subcommands;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.utils.PackUtils;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

public final class ImportCmd implements SubCommand {
    private final SpectraPlugin plugin;
    public ImportCmd(SpectraPlugin plugin) { this.plugin = plugin; }

    @Override public @NonNull String getName() { return "import"; }

    @Override
    public void execute(CommandSender sender, String @NonNull [] args) {
        if (args.length < 3) {
            sender.sendMessage("§eUso: /spectra import <archivo con espacios.ogg> <nombre_corto>");
            return;
        }

        String targetName = args[args.length - 1];

        String[] fileNameParts = Arrays.copyOfRange(args, 1, args.length - 1);
        String fileName = String.join(" ", fileNameParts);

        if (!fileName.toLowerCase().endsWith(".ogg")) {
            sender.sendMessage("§cEl archivo debe ser formato .ogg");
            return;
        }

        Path src = plugin.getDataFolder().toPath().resolve("import").resolve(fileName);
        Path dst = plugin.getDataFolder().toPath().resolve("sounds").resolve(targetName.toLowerCase() + ".ogg");

        if (!Files.exists(src)) {
            sender.sendMessage("§cNo se encontró el archivo: §7" + fileName);
            return;
        }

        try {
            Files.createDirectories(dst.getParent());
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);

            boolean packUpdated = plugin.getPackManager().buildPack();
            if (packUpdated) {
                PackUtils.sendPackToAll();
            }

            sender.sendMessage("§a✅ '" + fileName + "' importado como '" + targetName.toLowerCase() + "'.");
        } catch (Exception e) {
            sender.sendMessage("§cError al mover el archivo.");
        }
    }

    @Override public List<String> tabComplete(CommandSender sender, String @NonNull [] args) {
        if (args.length >= 2) {
            String input = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase();
            try (var s = Files.list(plugin.getDataFolder().toPath().resolve("import"))) {
                return s.map(p -> p.getFileName().toString())
                        .filter(n -> n.toLowerCase().endsWith(".ogg"))
                        .filter(n -> n.toLowerCase().startsWith(input))
                        .toList();
            } catch (Exception e) { return List.of(); }
        }
        return List.of();
    }
}