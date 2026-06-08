package org.astral.spectra.minecraft.events.visuals.world;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.astral.spectra.minecraft.utils.GlobalKeys;
import org.astral.spectra.minecraft.utils.SchedulerUtil;
import org.astral.spectra.minecraft.utils.VisualMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;

public final class ElytraVisuals {

    public static void draw(@NonNull Player player, VisualPreset preset, boolean isKick, boolean isSnare, boolean isHat) {
        Location loc = player.getEyeLocation();
        if (loc.getWorld() == null) return;

        SchedulerUtil.runOnEntity(SpectraPlugin.getInstance(), player, () -> {
            try {
                if (!player.isGliding()) return;

                Vector velocity = player.getVelocity();
                Vector lookDir = player.getLocation().getDirection();
                double speed = velocity.length();

                Vector dir = speed > 0.05 ? velocity.clone().normalize() : lookDir.normalize();
                if (Double.isNaN(dir.getX())) dir = new Vector(0, 0, 1);

                Vector right = VisualMath.getRightVector(dir);
                Vector up = VisualMath.getUpVector(dir, right);

                float energy = AudioAPI.getGlobalEnergy();
                Color dynColor = VisualMath.getDynamicColor((int) (Math.random() * 10), 10);
                Material blockMat = preset.getMainBlock();

                Location trailLoc = loc.clone().subtract(dir.clone().multiply(1.5));
                AudioBarsParticles.spawnColored(trailLoc, dynColor, 1.0f + (energy * 2), 5);

                if (isHat) {
                    AudioBarsParticles.spawnSafe(preset.getLowParticle(), trailLoc);
                }

                double distanceAhead = 6.0 + (speed * 15.0);

                if (isSnare) {
                    for(int i = 0; i < 4; i++) {
                        double spread = 3.0;
                        Vector offset = right.clone().multiply((Math.random() - 0.5) * spread)
                                .add(up.clone().multiply((Math.random() - 0.5) * spread));
                        Location windLoc = loc.clone().add(dir.clone().multiply(distanceAhead)).add(offset);
                        AudioBarsParticles.spawnSafe(preset.getHighParticle(), windLoc);
                    }
                }

                if (isKick) {
                    double ringRadius = Math.max(preset.getRadius(), 4.0) + (AudioAPI.getKickIntensity() * 3);
                    Location ringCenter = loc.clone().add(dir.clone().multiply(distanceAhead));

                    int points = 24;
                    for (int i = 0; i < points; i++) {
                        double rAngle = (2 * Math.PI / points) * i;
                        Vector rOffset = right.clone().multiply(Math.cos(rAngle) * ringRadius)
                                .add(up.clone().multiply(Math.sin(rAngle) * ringRadius));
                        Location pLoc = ringCenter.clone().add(rOffset);

                        AudioBarsParticles.spawnColored(pLoc, dynColor, 2.5f, 2);
                        if (preset.isDebris() && i % 3 == 0) {
                            Vector explosionDir = rOffset.clone().normalize().multiply(0.2 + (energy * 0.4));
                            spawnGhostBlock(pLoc, blockMat, explosionDir, dynColor);
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private static void spawnGhostBlock(@NonNull Location loc, Material mat, Vector velocity, Color color) {
        loc.getWorld().spawn(loc, FallingBlock.class, fb -> {
            fb.setBlockData(mat.createBlockData());
            fb.setGravity(false);
            fb.setDropItem(false);
            fb.setHurtEntities(false);
            fb.setVelocity(velocity);

            fb.getPersistentDataContainer().set(GlobalKeys.getDebrisKey(), PersistentDataType.BYTE, (byte) 1);
            SchedulerUtil.runDelayedOnEntity(SpectraPlugin.getInstance(), fb, () -> {
                if (fb.isValid()) {
                    AudioBarsParticles.spawnColored(fb.getLocation(), color, 2.0f, 6);
                    fb.remove();
                }
            }, 30L);
        });
    }
}