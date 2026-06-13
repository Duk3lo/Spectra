package org.astral.spectra.config;

public final class AudioConfig {
    private General general = new General();
    private Visualizer visualizer = new Visualizer();
    private Smoothing smoothing = new Smoothing();
    private BeatDetection beatDetection = new BeatDetection();

    public General getGeneral() { return general; }
    public Visualizer getVisualizer() { return visualizer; }
    public Smoothing getSmoothing() { return smoothing; }
    public BeatDetection getBeatDetection() { return beatDetection; }

    public void setGeneral(General general) { this.general = general; }
    public void setVisualizer(Visualizer visualizer) { this.visualizer = visualizer; }
    public void setSmoothing(Smoothing smoothing) { this.smoothing = smoothing; }
    public void setBeatDetection(BeatDetection beatDetection) { this.beatDetection = beatDetection; }

    public static class General {
        private boolean forceWebAudio = false;
        private float currentVolume = 0.0f;
        private int updateRateMs = 16;
        private String delayedTask = "0s";
        private boolean autoOpenBrowser = true;

        public boolean isForceWebAudio() { return forceWebAudio; }
        public void setForceWebAudio(boolean forceWebAudio) { this.forceWebAudio = forceWebAudio; }

        public float getCurrentVolume() { return currentVolume; }
        public void setCurrentVolume(float currentVolume) { this.currentVolume = currentVolume; }
        public int getUpdateRateMs() { return updateRateMs; }
        public void setUpdateRateMs(int updateRateMs) { this.updateRateMs = updateRateMs; }
        public String getDelayedTask() { return delayedTask; }
        public void setDelayedTask(String delayedTask) { this.delayedTask = delayedTask; }
        public boolean isAutoOpenBrowser() { return autoOpenBrowser; }
        public void setAutoOpenBrowser(boolean autoOpenBrowser) { this.autoOpenBrowser = autoOpenBrowser; }

        public long getDelayedTaskInMs() {
            if (delayedTask == null || delayedTask.isEmpty()) return 0;
            try {
                String unit = delayedTask.substring(delayedTask.length() - 1).toLowerCase();
                long value = Long.parseLong(delayedTask.substring(0, delayedTask.length() - 1));
                return switch (unit) {
                    case "s" -> value * 1000;
                    case "m" -> value * 60 * 1000;
                    case "h" -> value * 60 * 60 * 1000;
                    case "d" -> value * 24 * 60 * 60 * 1000;
                    default -> Long.parseLong(delayedTask);
                };
            } catch (NumberFormatException e) { return 0; }
        }
    }

    public static class Visualizer {
        private int fftSize = 2048;
        private int numBars = 32;
        public int getFftSize() { return fftSize; }
        public void setFftSize(int fftSize) { this.fftSize = fftSize; }
        public int getNumBars() { return numBars; }
        public void setNumBars(int numBars) { this.numBars = numBars; }
    }

    public static class Smoothing {
        private float attack = 0.85f;
        private float decay = 0.18f;
        public float getAttack() { return attack; }
        public void setAttack(float attack) { this.attack = attack; }
        public float getDecay() { return decay; }
        public void setDecay(float decay) { this.decay = decay; }
    }

    public static class BeatDetection {
        private float bassJumpThreshold = 1.35f;
        private float snareJumpThreshold = 1.25f;
        private float hatJumpThreshold = 1.15f;
        private long bassCooldownMs = 110;
        private long snareCooldownMs = 80;
        private long hatCooldownMs = 50;

        public float getBassJumpThreshold() { return bassJumpThreshold; }
        public void setBassJumpThreshold(float v) { this.bassJumpThreshold = v; }
        public float getSnareJumpThreshold() { return snareJumpThreshold; }
        public void setSnareJumpThreshold(float v) { this.snareJumpThreshold = v; }
        public float getHatJumpThreshold() { return hatJumpThreshold; }
        public void setHatJumpThreshold(float v) { this.hatJumpThreshold = v; }
        public long getBassCooldownMs() { return bassCooldownMs; }
        public void setBassCooldownMs(long v) { this.bassCooldownMs = v; }
        public long getSnareCooldownMs() { return snareCooldownMs; }
        public void setSnareCooldownMs(long v) { this.snareCooldownMs = v; }
        public long getHatCooldownMs() { return hatCooldownMs; }
        public void setHatCooldownMs(long v) { this.hatCooldownMs = v; }
    }
}