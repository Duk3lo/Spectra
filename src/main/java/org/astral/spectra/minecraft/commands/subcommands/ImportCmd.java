package org.astral.spectra.minecraft.commands.subcommands;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.utils.PackUtils;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.nio.file.*;
import java.util.List;

public final class ImportCmd implements SubCommand {
    private final SpectraPlugin plugin;
    public ImportCmd(SpectraPlugin plugin) { this.plugin = plugin; }

    @Override public @NonNull String getName() { return "import"; }

    @Override
    public void execute(CommandSender sender, String @NonNull [] args) {
        if (args.length < 3) return;

        String fileName = args[1];
        if (!fileName.toLowerCase().endsWith(".ogg")) return;

        Path src = plugin.getDataFolder().toPath().resolve("import").resolve(fileName);
        Path dst = plugin.getDataFolder().toPath().resolve("sounds").resolve(args[2].toLowerCase() + ".ogg");

        if (!Files.exists(src)) return;

        try {
            Files.createDirectories(dst.getParent());
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
            plugin.getPackManager().buildPack();
            PackUtils.sendPackToAll();
            sender.sendMessage("§a✅ Importado correctamente.");
        } catch (Exception e) {
            sender.sendMessage("§cError.");
        }
    }

    @Override public List<String> tabComplete(CommandSender sender, String @NonNull [] args) {
        if (args.length == 2) {
            try (var s = Files.list(plugin.getDataFolder().toPath().resolve("import"))) {
                return s.map(p -> p.getFileName().toString())
                        .filter(n -> n.toLowerCase().endsWith(".ogg"))
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
            } catch (Exception e) { return List.of(); }
        }
        return List.of();
    }
}