package org.astral.spectra.minecraft.events.visuals.world;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.astral.spectra.minecraft.utils.SchedulerUtil;
import org.astral.spectra.minecraft.utils.VisualMath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class ElytraVisuals {

    public static void draw(Player player, VisualPreset preset, boolean isKick) {
        Location loc = player.getLocation();
        if (loc == null || loc.getWorld() == null) return;

        // EJECUTA EN EL HILO SEGURO DEL JUGADOR PARA EVITAR LAG O CRASHES
        SchedulerUtil.runOnRegion(SpectraPlugin.getInstance(), loc, () -> {
            try {
                Vector dir = loc.getDirection().normalize();
                if (Double.isNaN(dir.getX())) dir = new Vector(0, 0, 1);

                Vector right = VisualMath.getRightVector(dir);
                Vector up = VisualMath.getUpVector(dir, right);

                org.bukkit.Color dynColor = VisualMath.getDynamicColor((int) (Math.random() * 10), 10);
                Material blockMat = preset.getMainBlock();

                // 1. RASTRO "STARDUST" MAGICO: Deja partículas brillantes atrás al volar
                Location trailLoc = loc.clone().subtract(dir.clone().multiply(1.5));
                AudioBarsParticles.spawnSafe(preset.getLowParticle(), trailLoc);
                AudioBarsParticles.spawnColored(trailLoc, dynColor, 1.0f, 3);

                // 2. TÚNEL ESPIRAL DE 3 BRAZOS (Efecto Galáctico)
                double distanceAhead = 12.0;
                double spiralRadius = (preset.getRadius() / 2) + AudioAPI.getGlobalEnergy();

                for (int i = 0; i < 3; i++) {
                    double angle = (VisualMath.globalPhase * 18.0) + (i * (2 * Math.PI / 3));
                    Vector spiralOffset = right.clone().multiply(Math.cos(angle) * spiralRadius)
                            .add(up.clone().multiply(Math.sin(angle) * spiralRadius));

                    Location spiralLoc = loc.clone().add(dir.clone().multiply(distanceAhead)).add(spiralOffset);

                    AudioBarsParticles.spawnColored(spiralLoc, dynColor, 1.5f, 2);
                    AudioBarsParticles.spawnSafe(preset.getHighParticle(), spiralLoc);
                }

                // 3. ANILLOS LUMINOSOS FLOTANTES EN EL KICK (Bajo)
                if (isKick) {
                    double ringDist = 25.0; // Se crea lejos para que te dé tiempo de atravesarlo
                    double ringRadius = preset.getRadius();
                    Location ringCenter = loc.clone().add(dir.clone().multiply(ringDist));

                    for (int i = 0; i < 16; i++) {
                        double rAngle = (2 * Math.PI / 16) * i;
                        Vector rOffset = right.clone().multiply(Math.cos(rAngle) * ringRadius)
                                .add(up.clone().multiply(Math.sin(rAngle) * ringRadius));
                        Location pLoc = ringCenter.clone().add(rOffset);

                        AudioBarsParticles.spawnColored(pLoc, dynColor, 2.5f, 5);

                        // Genera luces flotantes alternadas
                        if (i % 2 == 0) {
                            spawnGhostBlock(pLoc, blockMat);
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private static void spawnGhostBlock(Location loc, Material mat) {
        loc.getWorld().spawn(loc, FallingBlock.class, fb -> {
            fb.setBlockData(mat.createBlockData());
            fb.setGravity(false); // No caen, se quedan estáticos en el aire
            fb.setDropItem(false);
            fb.setHurtEntities(false);

            NamespacedKey key = new NamespacedKey(SpectraPlugin.getInstance(), "spectra_debris");
            fb.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

            // Desaparecen solos en 1.5s, soltando polvillo blanco
            Bukkit.getRegionScheduler().runDelayed(SpectraPlugin.getInstance(), loc, task -> {
                if (fb.isValid()) {
                    AudioBarsParticles.spawnColored(fb.getLocation(), org.bukkit.Color.WHITE, 1.0f, 2);
                    fb.remove();
                }
            }, 30L);
        });
    }
}