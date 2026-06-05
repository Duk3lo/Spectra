package org.astral.spectra.minecraft.events.event;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.commands.SpectraAdminCommand;
import org.astral.spectra.minecraft.events.visuals.VisualizerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public final class DisconnectListener implements Listener {
    private final SpectraPlugin plugin = SpectraPlugin.getInstance();

    @EventHandler
    public void onPlayerQuit(@NonNull PlayerQuitEvent event) {
        UUID disconnectedPlayerUuid = event.getPlayer().getUniqueId();
        SpectraAdminCommand.playersListen.remove(disconnectedPlayerUuid);
        if (SpectraAdminCommand.playersListen.isEmpty()) {
            if (AudioAPI.isPlaying()) {
                plugin.getLogger().info("Último oyente desconectado, deteniendo música y limpiando visualizadores...");
                plugin.getAudioEngine().stopSong();
            }
            VisualizerManager.stopAll();
        }
    }
}