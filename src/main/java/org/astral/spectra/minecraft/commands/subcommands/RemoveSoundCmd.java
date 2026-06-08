package org.astral.spectra.minecraft.commands.subcommands;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.utils.PackUtils;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class RemoveSoundCmd implements SubCommand {
    private final SpectraPlugin plugin;
    public RemoveSoundCmd(SpectraPlugin plugin) { this.plugin = plugin; }

    @Override
    public @NonNull String getName() { return "removesound"; }

    @Override
    public void execute(@NonNull CommandSender sender, String @NonNull [] args) {
        if (!sender.hasPermission("spectra.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§eUsage: /spectra removesound <short_name>");
            return;
        }

        String targetName = args[1].toLowerCase();
        if (!targetName.endsWith(".ogg")) targetName += ".ogg";

        Path sourceFile = plugin.getDataFolder().toPath().resolve("sounds").resolve(targetName);
        if (!Files.exists(sourceFile)) {
            sender.sendMessage("§cSound '§7" + targetName + "§c' is not installed.");
            return;
        }

        Path returnFile = plugin.getDataFolder().toPath().resolve("import").resolve(targetName);

        try {
            Files.createDirectories(returnFile.getParent());
            Files.move(sourceFile, returnFile, StandardCopyOption.REPLACE_EXISTING);

            if (plugin.getPackManager().buildPack()) {
                PackUtils.sendPackToAll();
            }

            sender.sendMessage("§a✅ Sound '" + targetName + "' removed and moved back to 'import/'.");
        } catch (Exception e) {
            sender.sendMessage("§cAn error occurred while removing the file.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String @NonNull [] args) {
        if (args.length == 2) {
            try (var s = Files.list(plugin.getDataFolder().toPath().resolve("sounds"))) {
                return s.map(p -> p.getFileName().toString().replace(".ogg", ""))
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            } catch (Exception e) { return List.of(); }
        }
        return List.of();
    }
}