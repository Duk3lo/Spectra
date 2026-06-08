package org.astral.spectra.minecraft.events;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.events.event.DebrisListener;
import org.astral.spectra.minecraft.events.event.DisconnectListener;
import org.astral.spectra.minecraft.events.event.JoinListener;
import org.bukkit.plugin.PluginManager;
import org.jspecify.annotations.NonNull;

public final class RegisterEvents {
    public static void registerAll(@NonNull SpectraPlugin plugin) {
        PluginManager manager = plugin.getServer().getPluginManager();
        manager.registerEvents(new JoinListener(), plugin);
        manager.registerEvents(new DisconnectListener(), plugin);
        manager.registerEvents(new DebrisListener(), plugin);
    }
}