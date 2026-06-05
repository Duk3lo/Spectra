package org.astral.spectra.minecraft.events.event;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

public final class DebrisListener implements Listener {

    private final NamespacedKey debrisKey;

    public DebrisListener(SpectraPlugin plugin) {
        this.debrisKey = new NamespacedKey(plugin, "spectra_debris");
    }

    @EventHandler
    public void onFallingBlockLand(@NonNull EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock fb) {
            if (fb.getPersistentDataContainer().has(debrisKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                fb.remove();
            }
        }
    }
}