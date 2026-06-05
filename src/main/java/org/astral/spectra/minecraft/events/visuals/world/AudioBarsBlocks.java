package org.astral.spectra.minecraft.events.visuals.world;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.astral.spectra.minecraft.events.visuals.VisualizerData;
import org.astral.spectra.minecraft.utils.VisualMath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;

public final class AudioBarsBlocks {

    public static void draw(VisualizerData data, float[] bars) {
        clearJustBlocks(data);
        if (bars == null) return;
        VisualPreset preset = data.getPreset();

        boolean isKick = AudioAPI.isBassHit();

        for (int i = 0; i < bars.length; i++) {
            float val = Math.clamp(bars[i], 0, 1);
            if (val <= 0.05f) continue;

            Vector offset = VisualMath.getOffset(preset.getShape(), i, bars.length, preset.getRadius(), val, preset.getSpacing());
            Location barBase = data.getPos().clone().add(offset);
            int height = (int) Math.ceil(val * preset.getMaxHeight());

            for (int y = 0; y < height; y++) {
                Location loc = barBase.clone().add(0, y, 0);
                Material mat = (y == height - 1) ? preset.getHitBlock() :
                        ((y % 2 != 0) ? preset.getAccentBlock() : preset.getMainBlock());

                loc.getBlock().setType(mat);
                data.getActiveBlocks().add(loc);
            }

            // LANZAR BLOQUES CON ETIQUETA
            if (isKick && val > 0.85f && i % 15 == 0) {
                spawnTornadoDebris(barBase.clone().add(0, height, 0), preset.getHitBlock());
            }
        }
    }

    private static void spawnTornadoDebris(@NonNull Location loc, Material mat) {
        loc.getWorld().spawn(loc, FallingBlock.class, fb -> {
            fb.setBlockData(mat.createBlockData());
            fb.setDropItem(false);
            fb.setHurtEntities(false);

            // --- ETIQUETAR EL BLOQUE PARA QUE NO SE SOLIDIFIQUE ---
            fb.setMetadata("SpectraDebris", new FixedMetadataValue(SpectraPlugin.getInstance(), true));

            fb.setVelocity(new Vector((Math.random() - 0.5) * 0.5, 0.5, (Math.random() - 0.5) * 0.5));

            // Eliminar en 1.5 segundos si sigue vivo
            Bukkit.getRegionScheduler().runDelayed(SpectraPlugin.getInstance(), loc, task -> {
                if (fb.isValid()) fb.remove();
            }, 30L);
        });
    }

    public static void clearJustBlocks(@NonNull VisualizerData data) {
        data.getActiveBlocks().forEach(loc -> loc.getBlock().setType(Material.AIR));
        data.getActiveBlocks().clear();
    }
}