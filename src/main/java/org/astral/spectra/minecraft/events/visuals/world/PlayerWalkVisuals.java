package org.astral.spectra.minecraft.events.visuals.world;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.astral.spectra.minecraft.utils.GlobalKeys;
import org.astral.spectra.minecraft.utils.SchedulerUtil;
import org.astral.spectra.minecraft.utils.VisualMath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;

public final class PlayerWalkVisuals {

    public static void draw(@NonNull Player player, VisualPreset preset, boolean isKick, boolean isSnare, boolean isHat) {
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return;

        SchedulerUtil.runOnRegion(SpectraPlugin.getInstance(), loc, () -> {
            try {
                if (player.isFlying() || player.isGliding()) return;

                Material centerMat = loc.getBlock().getRelative(BlockFace.DOWN).getType();
                if (!centerMat.isSolid()) return;

                Vector velocity = player.getVelocity();
                double speed = velocity.length();
                float energy = AudioAPI.getGlobalEnergy();
                org.bukkit.Color dynColor = VisualMath.getDynamicColor((int) (Math.random() * 10), 10);

                if (speed > 0.05) {
                    Location trail = loc.clone().add(0, 0.1, 0);
                    AudioBarsParticles.spawnColored(trail, dynColor, 0.8f + energy, 2);

                    if (isHat) {
                        AudioBarsParticles.spawnSafe(preset.getLowParticle(), trail.clone().add(0, 0.4, 0));
                    }
                }

                int arms = 3;
                double baseRadius = preset.getRadius() * 0.8;
                double pulsing = Math.sin(VisualMath.globalPhase * 2) * 0.8;
                double radius = baseRadius + pulsing + (AudioAPI.getKickIntensity() * 2.0);

                for (int i = 0; i < arms; i++) {
                    double angle = (VisualMath.globalPhase * 2.0) + (i * (2 * Math.PI / arms));
                    Location spiralLoc = loc.clone().add(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius);

                    AudioBarsParticles.spawnColored(spiralLoc, dynColor, 1.2f, 1);

                    if (isSnare) {
                        AudioBarsParticles.spawnSafe(preset.getHighParticle(), spiralLoc.clone().add(0, 0.8, 0));
                    }
                }

                if (isKick && AudioAPI.getKickIntensity() > 0.6f) {
                    int explosions = 2 + (int)(energy * 5);

                    for (int i = 0; i < explosions; i++) {
                        double rAngle = Math.random() * Math.PI * 2;
                        double minDistance = preset.getRadius() * 1.5;
                        double rDist = minDistance + (Math.random() * preset.getRadius() * 1.5);
                        Location explosionLoc = loc.clone().add(Math.cos(rAngle) * rDist, 0, Math.sin(rAngle) * rDist);

                        Material floorMat = explosionLoc.getBlock().getRelative(BlockFace.DOWN).getType();
                        if (floorMat.isSolid()) {
                            spawnGroundDebris(explosionLoc, floorMat, dynColor);
                        }
                    }
                }

            } catch (Exception ignored) {}
        });
    }

    private static void spawnGroundDebris(@NonNull Location loc, Material mat, org.bukkit.Color color) {
        int blocksPerExplosion = 1 + (int)(AudioAPI.getKickIntensity() * 3);

        for (int i = 0; i < blocksPerExplosion; i++) {
            loc.getWorld().spawn(loc.clone().add(0, 0.5, 0), FallingBlock.class, fb -> {
                fb.setBlockData(mat.createBlockData());
                fb.setDropItem(false);
                fb.setHurtEntities(false);

                double vx = (Math.random() - 0.5) * 0.8;
                double vy = 0.6 + (AudioAPI.getKickIntensity() * 0.8);
                double vz = (Math.random() - 0.5) * 0.8;
                fb.setVelocity(new Vector(vx, vy, vz));

                fb.getPersistentDataContainer().set(GlobalKeys.getDebrisKey(), PersistentDataType.BYTE, (byte) 1);

                Bukkit.getRegionScheduler().runDelayed(SpectraPlugin.getInstance(), loc, _ -> {
                    if (fb.isValid()) {
                        AudioBarsParticles.spawnColored(fb.getLocation(), color, 1.5f, 4);
                        fb.remove();
                    }
                }, 40L);
            });
        }
    }
}