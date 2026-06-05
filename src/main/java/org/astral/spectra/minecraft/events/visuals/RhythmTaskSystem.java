package org.astral.spectra.minecraft.events.visuals;

import org.astral.spectra.audio.api.AudioAPI;
import org.astral.spectra.minecraft.SpectraPlugin;
import org.astral.spectra.minecraft.events.visuals.world.AudioBarsBlocks;
import org.astral.spectra.minecraft.events.visuals.world.AudioBarsParticles;
import org.astral.spectra.minecraft.events.visuals.world.ElytraVisuals;
import org.astral.spectra.minecraft.utils.SchedulerUtil;
import org.astral.spectra.minecraft.utils.VisualMath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RhythmTaskSystem {
    private final SpectraPlugin plugin;
    private boolean isRunning = false;

    public RhythmTaskSystem(SpectraPlugin plugin) { this.plugin = plugin; }

    public void start() {
        if (isRunning) return;
        isRunning = true;

        SchedulerUtil.runGlobalTimer(plugin, () -> {
            if (!AudioAPI.isPlaying()) {
                if (!VisualizerManager.getAllVisualizers().isEmpty()) VisualizerManager.stopAll();
                return;
            }

            VisualMath.updatePhase();

            float[] bars = AudioAPI.getAllBars();
            if (bars == null) return;

            boolean hasBeat = AudioAPI.isBassHit();

            for (VisualizerData data : VisualizerManager.getAllVisualizers()) {
                if (data == null || data.getPreset() == null) continue;

                String mode = data.getPreset().getRenderMode();
                if (mode == null) mode = "mixed";
                mode = mode.toLowerCase();

                // --- SISTEMA MÁGICO DE VUELO ---
                if (mode.equals("elytra")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.isGliding()) {
                            ElytraVisuals.draw(p, data.getPreset(), hasBeat);
                        }
                    }
                    continue;
                }

                // --- SISTEMA NORMAL ---
                if (mode.contains("blocks") || mode.contains("mixed")) {
                    SchedulerUtil.runOnRegion(plugin, data.getPos(), () -> AudioBarsBlocks.draw(data, bars));
                }

                if (mode.contains("particles") || mode.contains("mixed")) {
                    if (hasBeat) {
                        AudioBarsParticles.spawnBeatEffect(data, bars);
                    }
                    AudioBarsParticles.spawn(data, bars);
                }
            }
            if (hasBeat) AudioAPI.consumeBassHit();
        }, 1L, 1L);
    }
}