package org.astral.audio;

public class MyHytaleModLogic {

    public void onModStart() {
        OpenALAudioEngine.startEngine();
        OpenALAudioEngine.setVolume(0.8f);
    }

    public void onPlayerInput(String key) {
        switch (key) {
            case "P":
                if (RhythmAPI.isPaused) OpenALAudioEngine.resumeSong();
                else OpenALAudioEngine.pauseSong();
                break;
            case "L":
                OpenALAudioEngine.setLooping(true);
                break;
            case "S":
                OpenALAudioEngine.stopSong();
                break;
            case "N":
                //BeatAudioEngine.playNewSong("src/main/resources/nivel_boss.ogg");
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
        OpenALAudioEngine.shutdown();
    }
}