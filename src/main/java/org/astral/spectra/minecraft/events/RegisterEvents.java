package org.astral.spectra.minecraft.events;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.events.event.JoinListener;
import org.astral.spectra.minecraft.pack.ResourcePackManager;
import org.bukkit.plugin.PluginManager;
import org.jspecify.annotations.NonNull;

public final class RegisterEvents {

    public static void registerAll(SpectraPlugin plugin, String serverIp) {
        registerEvents(plugin, plugin.getPackManager(), serverIp);
    }

    private static void registerEvents(@NonNull SpectraPlugin plugin, ResourcePackManager packManager, String serverIp) {
        PluginManager manager = plugin.getServer().getPluginManager();
        manager.registerEvents(new JoinListener(packManager, serverIp), plugin);
    }
}