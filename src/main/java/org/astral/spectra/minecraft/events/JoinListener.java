package org.astral.spectra.minecraft.events;

import org.astral.spectra.minecraft.pack.ResourcePackManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

public final class JoinListener implements Listener {
    private final ResourcePackManager packManager;
    private final String serverIp;

    public JoinListener(ResourcePackManager packManager, String serverIp) {
        this.packManager = packManager;
        this.serverIp = serverIp;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String hash = packManager.getCurrentHash();

        if (hash != null && !hash.isEmpty()) {
            String url = "http://" + serverIp + ":8080/pack.zip";
            event.getPlayer().setResourcePack(url, hash, true, Component.text("Acepta el pack para escuchar la música del servidor."));
        }
    }

    @EventHandler
    public void onResourcePackStatus(@NonNull PlayerResourcePackStatusEvent event) {
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED:
                event.getPlayer().sendMessage("§a✅ ¡Pack de audio cargado! Usa /playmusic");
                break;
            case DECLINED:
                event.getPlayer().sendMessage("§c❌ Rechazaste el pack. No podrás escuchar audios.");
                break;
            case FAILED_DOWNLOAD:
                event.getPlayer().sendMessage("§c⚠️ Falló la descarga del pack. Revisa la conexión.");
                break;
            case ACCEPTED:
                break;
        }
    }
}