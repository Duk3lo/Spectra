package org.astral.spectra.minecraft.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class SchedulerUtil {
    private static final boolean IS_FOLIA = isClassPresent();

    private static boolean isClassPresent() {
        try { Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler"); return true; } catch (ClassNotFoundException e) { return false; }
    }

    public static void runGlobalTimer(Plugin plugin, Runnable runnable, long delay, long period) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, _ -> runnable.run(), delay, period);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
        }
    }

    public static void runOnRegion(Plugin plugin, Location loc, Runnable runnable) {
        if (IS_FOLIA) {
            Bukkit.getRegionScheduler().execute(plugin, loc, runnable);
        } else {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            } else {
                runnable.run();
            }
        }
    }

    public static void runDelayedOnRegion(Plugin plugin, Location loc, Runnable runnable, long delayTicks) {
        if (IS_FOLIA) {
            Bukkit.getRegionScheduler().runDelayed(plugin, loc, _ -> runnable.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static void runOnEntity(Plugin plugin, Entity entity, Runnable runnable) {
        if (IS_FOLIA) {
            entity.getScheduler().execute(plugin, runnable, null, 1L);
        } else {
            if (!Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            } else {
                runnable.run();
            }
        }
    }

    public static void runDelayedOnEntity(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        if (IS_FOLIA) {
            entity.getScheduler().execute(plugin, runnable, null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }
}