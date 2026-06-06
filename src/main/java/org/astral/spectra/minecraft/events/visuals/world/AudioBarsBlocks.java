package org.astral.spectra.minecraft.events.visuals.world;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.config.VisualsConfig.VisualPreset;
import org.astral.spectra.minecraft.events.visuals.VisualizerData;
import org.astral.spectra.minecraft.utils.GlobalKeys;
import org.astral.spectra.minecraft.utils.VisualMath;
import org.bukkit.*;
import org.bukkit.entity.FallingBlock;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;

public final class AudioBarsBlocks {

    public static void draw(VisualizerData data, float[] bars) {
        if (bars == null) return;
        VisualPreset preset = data.getPreset();

        if (!preset.isPlatforms()) clearJustBlocks(data);

        boolean isKick = AudioAPI.isBassHit();

        boolean is3D = preset.getShape().equalsIgnoreCase("tornado")
                || preset.getShape().equalsIgnoreCase("helix")
                || preset.getShape().equalsIgnoreCase("sphere")
                || preset.getShape().equalsIgnoreCase("heart");

        double rotationAngle = Math.toRadians(-data.getYaw());

        for (int i = 0; i < bars.length; i++) {
            float val = Math.clamp(bars[i], 0, 1);
            if (val <= 0.05f) continue;

            Vector offset = VisualMath.getOffset(preset.getShape(), i, bars.length, preset.getRadius(), val, preset.getSpacing(), preset.getMaxHeight());

            offset.rotateAroundY(rotationAngle);

            Location barBase = data.getPos().clone().add(offset);
            int height = is3D ? 1 : (int) Math.ceil(val * preset.getMaxHeight());

            if (preset.isPlatforms()) {
                if (isKick && val > 0.5f && i % 2 == 0) {
                    Location platLoc = barBase.clone().add(0, height, 0);
                    org.bukkit.Color dynColor = VisualMath.getDynamicColor(i, bars.length);
                    spawnPlatform(data, platLoc, preset.getHitBlock(), dynColor);
                }
            }
            else {
                for (int y = 0; y < height; y++) {
                    Location loc = barBase.clone().add(0, y, 0);
                    Material mat = (y == height - 1) ? preset.getHitBlock() :
                            ((y % 2 != 0) ? preset.getAccentBlock() : preset.getMainBlock());
                    spawnGravityBlock(loc, mat);
                }

                if (isKick && val > 0.85f && i % 15 == 0) {
                    spawnTornadoDebris(barBase.clone().add(0, height, 0), preset.getHitBlock());
                }
            }
        }
    }

    private static void spawnGravityBlock(@NonNull Location loc, Material mat) {
        loc.getWorld().spawn(loc, FallingBlock.class, fb -> {
            fb.setBlockData(mat.createBlockData());
            fb.setDropItem(false);
            fb.setHurtEntities(false);
            fb.setGravity(false);

            fb.getPersistentDataContainer().set(GlobalKeys.getDebrisKey(), PersistentDataType.BYTE, (byte) 1);

            Bukkit.getRegionScheduler().runDelayed(SpectraPlugin.getInstance(), loc, _ -> {
                if (fb.isValid()) {
                    fb.remove();
                }
            }, 3L);
        });
    }

    private static void spawnPlatform(@NonNull VisualizerData data, @NonNull Location loc, Material mat, Color color) {
        Location[] crossShape = {
                loc,
                loc.clone().add(1, 0, 0), loc.clone().add(-1, 0, 0),
                loc.clone().add(0, 0, 1), loc.clone().add(0, 0, -1)
        };

        boolean spawnedAny = false;
        for (Location l : crossShape) {
            if (l.getBlock().getType() == Material.AIR) {
                l.getBlock().setType(mat);
                data.getActiveBlocks().add(l);
                spawnedAny = true;
            }
        }

        if (!spawnedAny) return;
        AudioBarsParticles.spawnColored(loc.clone().add(0.5, 1.2, 0.5), color, 2.0f, 5);

        long warningTicks = 60L - 20L;
        Bukkit.getRegionScheduler().runDelayed(SpectraPlugin.getInstance(), loc, _ -> {
            for (Location l : crossShape) {
                if (l.getBlock().getType() == mat) {
                    l.getBlock().setType(Material.RED_STAINED_GLASS);
                    AudioBarsParticles.spawnColored(l.clone().add(0.5, 1.2, 0.5), org.bukkit.Color.RED, 1.5f, 3);
                }
            }
        }, warningTicks);

        Bukkit.getRegionScheduler().runDelayed(SpectraPlugin.getInstance(), loc, _ -> {
            for (Location l : crossShape) {
                if (l.getBlock().getType() == Material.RED_STAINED_GLASS) {
                    l.getBlock().setType(Material.AIR);
                    data.getActiveBlocks().remove(l);
                    spawnTornadoDebris(l, Material.RED_STAINED_GLASS);
                }
            }
        }, 60L);
    }

    private static void spawnTornadoDebris(@NonNull Location loc, Material mat) {
        loc.getWorld().spawn(loc, FallingBlock.class, fb -> {
            fb.setBlockData(mat.createBlockData());
            fb.setDropItem(false);
            fb.setHurtEntities(false);

            fb.getPersistentDataContainer().set(GlobalKeys.getDebrisKey(), PersistentDataType.BYTE, (byte) 1);
            fb.setVelocity(new Vector((Math.random() - 0.5) * 0.15, 0.1, (Math.random() - 0.5) * 0.15));

            Bukkit.getRegionScheduler().runDelayed(SpectraPlugin.getInstance(), loc, _ -> {
                if (fb.isValid()) {
                    AudioBarsParticles.spawnColored(fb.getLocation(), org.bukkit.Color.RED, 0.8f, 2);
                    fb.remove();
                }
            }, 15L);
        });
    }

    public static void clearJustBlocks(@NonNull VisualizerData data) {
        data.getActiveBlocks().forEach(loc -> loc.getBlock().setType(Material.AIR));
        data.getActiveBlocks().clear();
    }
}