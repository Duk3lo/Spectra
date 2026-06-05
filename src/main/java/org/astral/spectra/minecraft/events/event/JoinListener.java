package org.astral.spectra.minecraft.events.event;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

public final class JoinListener implements Listener {
    private final SpectraPlugin plugin = SpectraPlugin.getInstance();

    @EventHandler
    public void onPlayerJoin(@NonNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String serverIp = plugin.getConfigManager().getServerIp();
        int serverPort = plugin.getConfigManager().getServerPort();

        String hash = plugin.getPackManager().getCurrentHash();

        if (hash != null && !hash.isEmpty()) {

            String packUrl = "http://" + serverIp + ":" + serverPort + "/pack.zip";

            player.setResourcePack(
                    packUrl,
                    hash,
                    true,
                    Component.text("Acepta el pack para escuchar la música del servidor.")
            );
        }
    }

    @EventHandler
    public void onResourcePackStatus(@NonNull PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED:
                player.sendMessage("§a✅ ¡Pack de audio cargado! Usa /playmusic");
                break;
            case DECLINED:
                player.sendMessage("§c❌ Rechazaste el pack. No podrás escuchar audios.");
                break;
            case FAILED_DOWNLOAD:
                player.sendMessage("§c⚠️ Falló la descarga del pack. Revisa la conexión.");
                break;
            case ACCEPTED, DOWNLOADED:
                break;
        }
    }
}