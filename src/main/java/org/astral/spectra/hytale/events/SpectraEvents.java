package org.astral.spectra.hytale.events;

import org.astral.spectra.hytale.SpectraPlugin;
import org.astral.spectra.hytale.events.event.Disconnect;
import org.astral.spectra.hytale.events.visuals.RhythmBlockSystem;

public final class SpectraEvents {
    private static final SpectraPlugin plugin = SpectraPlugin.getInstance();
    public static void RegisterAll(){
        Disconnect.register(plugin.getEventRegistry(), plugin.getLogger(), plugin.getAudioEngine());
        plugin.getEntityStoreRegistry().registerSystem(new RhythmBlockSystem());
    }
}

