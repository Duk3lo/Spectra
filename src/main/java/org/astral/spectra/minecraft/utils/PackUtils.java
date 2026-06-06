package org.astral.spectra.minecraft.utils;

import net.kyori.adventure.text.Component;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class PackUtils {

    public static void sendPack(@NonNull Player player) {
        SpectraPlugin plugin = SpectraPlugin.getInstance();
        String ip = plugin.getConfigManager().getServerIp();
        int port = plugin.getConfigManager().getServerPort();
        String hash = plugin.getPackManager().getCurrentHash();

        if (hash == null || hash.isEmpty()) return;

        String url = "http://" + ip + ":" + port + "/pack.zip";
        player.setResourcePack(url, hash, true, Component.text("§bSpectra: §fSincronizando audios..."));
    }

    public static void sendPackToAll() {
        Bukkit.getOnlinePlayers().forEach(PackUtils::sendPack);
    }
}