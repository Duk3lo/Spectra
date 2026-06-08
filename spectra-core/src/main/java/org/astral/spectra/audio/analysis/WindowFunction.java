package org.astral.spectra.audio.analysis;

import org.jetbrains.annotations.NotNull;

public class WindowFunction {
    public static float @NotNull [] applyHanning(float @NotNull [] samples) {
        int length = samples.length;
        float[] windowed = new float[length];
        for (int i = 0; i < length; i++) {
            windowed[i] = (float) (samples[i] * (0.5 * (1.0 - Math.cos(2 * Math.PI * i / (length - 1)))));
        }
        return windowed;
    }
}