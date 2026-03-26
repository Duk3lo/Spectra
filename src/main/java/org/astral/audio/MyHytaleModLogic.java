package org.astral.audio;

public class MyHytaleModLogic {

    public void onModStart() {
        BeatAudioEngine.startEngine();
        BeatAudioEngine.setVolume(0.8f);
    }

    public void onPlayerInput(String key) {
        switch (key) {
            case "P":
                if (RhythmAPI.isPaused) BeatAudioEngine.resumeSong();
                else BeatAudioEngine.pauseSong();
                break;
            case "L":
                BeatAudioEngine.setLooping(true);
                break;
            case "S":
                BeatAudioEngine.stopSong();
                break;
            case "N":
                BeatAudioEngine.playNewSong("src/main/resources/nivel_boss.ogg");
                break;
        }
    }

    // Método simulado del juego que corre cada frame/tick
    public void onUpdate(float deltaTime) {
        if (RhythmAPI.isPaused) return;

        if (RhythmAPI.popBassEvent()) {
            // spawnGiantBlock();
        }

        if (RhythmAPI.popSnareEvent()) {
            // givePlayerSpeedBoost();
        }

        float multiplicadorParticulas = RhythmAPI.particleSpeedMultiplier;
        // particle.setSpeed(baseSpeed * multiplicadorParticulas);
    }

    // Se ejecuta al cerrar el servidor o salir del mapa
    public void onModStop() {
        BeatAudioEngine.shutdown();
    }
}