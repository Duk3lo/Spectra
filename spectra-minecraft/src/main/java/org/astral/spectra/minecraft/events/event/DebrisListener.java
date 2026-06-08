package org.astral.spectra.minecraft.events.event;

import org.astral.spectra.minecraft.events.visuals.world.AudioBarsParticles;
import org.astral.spectra.minecraft.utils.GlobalKeys;
import org.bukkit.Color;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

public final class DebrisListener implements Listener {

    @EventHandler
    public void onFallingBlockLand(@NonNull EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock fb) {
            if (fb.getPersistentDataContainer().has(GlobalKeys.getDebrisKey(), PersistentDataType.BYTE)) {
                event.setCancelled(true);
                AudioBarsParticles.spawnColored(fb.getLocation(), Color.WHITE, 1.5f, 5);
                fb.remove();
            }
        }
    }
}