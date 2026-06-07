package org.astral.spectra.minecraft.events.event;

import org.astral.spectra.minecraft.utils.PackUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.jspecify.annotations.NonNull;

public final class JoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(@NonNull PlayerJoinEvent event) {
        PackUtils.sendPack(event.getPlayer());
    }

    @EventHandler
    public void onResourcePackStatus(@NonNull PlayerResourcePackStatusEvent event) {
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED -> event.getPlayer().sendMessage("§a✅ Resource pack loaded.");
            case DECLINED -> event.getPlayer().sendMessage("§c❌ Resource pack is required to hear the music.");
            case FAILED_DOWNLOAD -> event.getPlayer().sendMessage("§c⚠️ Failed to download the resource pack.");
            default -> {}
        }
    }
}