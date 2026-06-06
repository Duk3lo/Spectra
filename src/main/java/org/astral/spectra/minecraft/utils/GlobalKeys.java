package org.astral.spectra.minecraft.utils;

import org.astral.spectra.minecraft.SpectraPlugin;
import org.bukkit.NamespacedKey;

public final class GlobalKeys {
    private static NamespacedKey DEBRIS_KEY;

    public static NamespacedKey getDebrisKey() {
        if (DEBRIS_KEY == null) {
            DEBRIS_KEY = new NamespacedKey(SpectraPlugin.getInstance(), "spectra_debris");
        }
        return DEBRIS_KEY;
    }
}