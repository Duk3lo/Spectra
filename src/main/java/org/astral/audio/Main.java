package org.astral.audio;

public class Main {

    static void main() {
        BeatAudioEngine.startEngine();
        BeatAudioEngine.setLooping(false);
        BeatAudioEngine.playNewSong("src/main/resources/perfect.ogg");
        BeatAudioEngine.waitForExit();
    }
}