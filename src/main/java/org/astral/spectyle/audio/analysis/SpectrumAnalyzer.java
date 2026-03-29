package org.astral.spectyle.audio.analysis;

import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.audio.state.AudioBuffer;
import org.jetbrains.annotations.NotNull;
import org.jtransforms.fft.FloatFFT_1D;

public class SpectrumAnalyzer {
    private final FloatFFT_1D fft;
    private final int[] boundaries;
    private final AudioConfig config;

    public SpectrumAnalyzer(@NotNull AudioConfig config) {
        this.config = config;
        this.fft = new FloatFFT_1D(config.getVisualizer().getFftSize());
        this.boundaries = computeBoundaries(config.getVisualizer().getFftSize(), config.getVisualizer().getNumBars());
    }

    private int @NotNull [] computeBoundaries(int fftSize, int numBars) {
        int[] bounds = new int[numBars + 1];
        bounds[0] = 1;
        double maxBin = fftSize / 2.0;
        double logMin = Math.log10(1);
        double logMax = Math.log10(maxBin);

        for (int i = 1; i <= numBars; i++) {
            double val = logMin + (logMax - logMin) * ((double) i / numBars);
            bounds[i] = (int) Math.round(Math.pow(10, val));
            if (bounds[i] <= bounds[i - 1]) bounds[i] = bounds[i - 1] + 1;
        }
        if (bounds[numBars] > fftSize / 2) bounds[numBars] = fftSize / 2;
        return bounds;
    }

    public float[] extractSamples(@NotNull AudioBuffer buffer, int startIdx) {
        int fftSize = config.getVisualizer().getFftSize();
        int channels = buffer.channels();
        float[] samples = new float[fftSize];

        for (int j = 0; j < fftSize; j++) {
            int idx = startIdx + (j * channels);
            float s = (channels == 2)
                    ? (buffer.pcmData().get(idx) + buffer.pcmData().get(idx + 1)) / 65536f
                    : buffer.pcmData().get(idx) / 32768f;
            samples[j] = s;
        }
        return WindowFunction.applyHanning(samples);
    }

    public float[] computeFFT(float[] windowedSamples) {
        int fftSize = config.getVisualizer().getFftSize();
        int numBars = config.getVisualizer().getNumBars();

        fft.realForward(windowedSamples);
        float[] magnitudes = new float[fftSize / 2];
        magnitudes[0] = Math.abs(windowedSamples[0]);

        for (int k = 1; k < fftSize / 2; k++) {
            float re = windowedSamples[2 * k];
            float im = windowedSamples[2 * k + 1];
            magnitudes[k] = (float) Math.sqrt(re * re + im * im);
        }

        float[] bars = new float[numBars];
        for (int i = 0; i < numBars; i++) {
            int start = boundaries[i];
            int end = boundaries[i + 1];
            float sum = 0;
            for (int j = start; j < end; j++) sum += magnitudes[j];
            bars[i] = sum / Math.max(1, (end - start));
        }
        return bars;
    }
}