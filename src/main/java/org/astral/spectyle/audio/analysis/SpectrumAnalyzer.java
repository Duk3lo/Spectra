package org.astral.spectyle.audio.analysis;

import org.astral.spectyle.config.AudioConfig;
import org.astral.spectyle.audio.state.AudioBuffer;
import org.jetbrains.annotations.NotNull;
import org.jtransforms.fft.FloatFFT_1D;

public class SpectrumAnalyzer {

    private FloatFFT_1D fft;
    private int[] boundaries;
    private final AudioConfig config;

    private float[] samplesBuffer;
    private float[] magnitudesBuffer;
    private float[] barsBuffer;

    public SpectrumAnalyzer(@NotNull AudioConfig config) {
        this.config = config;
        rebuild();
    }

    public void rebuild() {
        int fftSize = config.getVisualizer().getFftSize();
        int numBars = config.getVisualizer().getNumBars();

        this.fft = new FloatFFT_1D(fftSize);
        this.boundaries = computeBoundaries(fftSize, numBars);

        this.samplesBuffer = new float[fftSize];
        this.magnitudesBuffer = new float[fftSize / 2];
        this.barsBuffer = new float[numBars];
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

            if (bounds[i] <= bounds[i - 1]) {
                bounds[i] = bounds[i - 1] + 1;
            }
        }

        if (bounds[numBars] > fftSize / 2) {
            bounds[numBars] = fftSize / 2;
        }

        return bounds;
    }

    public float[] extractSamples(@NotNull AudioBuffer buffer, int startIdx) {
        int fftSize = config.getVisualizer().getFftSize();
        int channels = buffer.channels();

        for (int j = 0; j < fftSize; j++) {
            int idx = startIdx + (j * channels);

            if (idx + (channels - 1) < buffer.pcmData().capacity()) {
                float s = (channels == 2)
                        ? (buffer.pcmData().get(idx) + buffer.pcmData().get(idx + 1)) / 65536f
                        : buffer.pcmData().get(idx) / 32768f;
                samplesBuffer[j] = s;
            } else {
                samplesBuffer[j] = 0;
            }
        }

        return WindowFunction.applyHanning(samplesBuffer);
    }

    public float[] computeFFT(float[] windowedSamples) {
        int fftSize = config.getVisualizer().getFftSize();
        int numBars = boundaries.length - 1;

        fft.realForward(windowedSamples);

        magnitudesBuffer[0] = Math.abs(windowedSamples[0]);
        for (int k = 1; k < fftSize / 2; k++) {
            int reIndex = 2 * k;
            int imIndex = 2 * k + 1;

            if (imIndex >= windowedSamples.length) break;

            float re = windowedSamples[reIndex];
            float im = windowedSamples[imIndex];

            magnitudesBuffer[k] = (float) Math.sqrt(re * re + im * im);
        }

        for (int i = 0; i < numBars; i++) {
            int start = clamp(boundaries[i], 0, magnitudesBuffer.length - 1);
            int end = clamp(boundaries[i + 1], start + 1, magnitudesBuffer.length);

            float sum = 0;
            for (int j = start; j < end; j++) {
                sum += magnitudesBuffer[j];
            }

            barsBuffer[i] = sum / Math.max(1, (end - start));
        }

        return barsBuffer;
    }

    private int clamp(int v, int min, int max) {
        return Math.clamp(v, min, max);
    }
}