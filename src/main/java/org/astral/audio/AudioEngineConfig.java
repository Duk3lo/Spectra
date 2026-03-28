package org.astral.audio;

import java.util.concurrent.TimeUnit;

public final class AudioEngineConfig {
    // Volumen
    private float currentVolume = 0.5f;

    // Configuración para el evento retrasado
    private long delayedTaskTimePlaySong = 0L;
    private TimeUnit delayedTaskTimeUnitPlaySong = TimeUnit.SECONDS;

    // Tamaños y Barras
    private int fftSize = 2048; // Debe ser potencia de 2 (1024, 2048, 4096)
    private int numBars = 32;   // Cantidad de barras en el visualizador

    // Suavizado (Smoothing)
    private float attack = 0.75f;
    private float decay = 0.20f;

    // Multiplicadores de umbral para detección de ritmos (Beat Detection)
    private float bassJumpThreshold = 1.50f;
    private float snareJumpThreshold = 1.35f;
    private float hatJumpThreshold = 1.25f;

    // Cooldowns de eventos en milisegundos (para evitar spam de partículas)
    private long bassCooldownMs = 250;
    private long snareCooldownMs = 150;
    private long hatCooldownMs = 100;

    // Tasa de actualización del executor (ms)
    private int updateRateMs = 16; // ~60 FPS

    public AudioEngineConfig() {
    }

    // ================= GETTERS =================
    public float getCurrentVolume() {return currentVolume; }
    public int getFftSize() { return fftSize; }
    public int getNumBars() { return numBars; }
    public float getAttack() { return attack; }
    public float getDecay() { return decay; }
    public float getBassJumpThreshold() { return bassJumpThreshold; }
    public float getSnareJumpThreshold() { return snareJumpThreshold; }
    public float getHatJumpThreshold() { return hatJumpThreshold; }
    public long getBassCooldownMs() { return bassCooldownMs; }
    public long getSnareCooldownMs() { return snareCooldownMs; }
    public long getHatCooldownMs() { return hatCooldownMs; }
    public int getUpdateRateMs() { return updateRateMs; }
    public long getDelayedTaskTimePlaySong() { return delayedTaskTimePlaySong; }
    public TimeUnit getDelayedTaskTimeUnitPlaySong() { return delayedTaskTimeUnitPlaySong; }

    // ================= SETTERS =================
    public void setCurrentVolume(float currentVolume){ this.currentVolume = currentVolume; }
    public void setFftSize(int fftSize) { this.fftSize = fftSize; }
    public void setNumBars(int numBars) { this.numBars = numBars; }
    public void setAttack(float attack) { this.attack = attack; }
    public void setDecay(float decay) { this.decay = decay; }
    public void setBassJumpThreshold(float bassJumpThreshold) { this.bassJumpThreshold = bassJumpThreshold; }
    public void setSnareJumpThreshold(float snareJumpThreshold) { this.snareJumpThreshold = snareJumpThreshold; }
    public void setHatJumpThreshold(float hatJumpThreshold) { this.hatJumpThreshold = hatJumpThreshold; }
    public void setBassCooldownMs(long bassCooldownMs) { this.bassCooldownMs = bassCooldownMs; }
    public void setSnareCooldownMs(long snareCooldownMs) { this.snareCooldownMs = snareCooldownMs; }
    public void setHatCooldownMs(long hatCooldownMs) { this.hatCooldownMs = hatCooldownMs; }
    public void setUpdateRateMs(int updateRateMs) { this.updateRateMs = updateRateMs; }
    public void setDelayedTaskTimePlaySong(long delayedTaskTimePlaySong) { this.delayedTaskTimePlaySong = delayedTaskTimePlaySong; }
    public void setDelayedTaskTimeUnitPlaySong(TimeUnit delayedTaskTimeUnitPlaySong) { this.delayedTaskTimeUnitPlaySong = delayedTaskTimeUnitPlaySong; }
}