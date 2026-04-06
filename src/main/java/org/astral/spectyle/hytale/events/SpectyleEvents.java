package org.astral.spectyle.hytale.events;

import org.astral.spectyle.hytale.SpectylePlugin;
import org.astral.spectyle.hytale.events.event.Disconnect;

public final class SpectyleEvents {
    private static final SpectylePlugin plugin = SpectylePlugin.getInstance();
    public static void RegisterAll(){
        Disconnect.register(plugin.getEventRegistry(), plugin.getLogger(), plugin.getAudioEngine());
    }
}

