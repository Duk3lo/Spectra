package org.astral.audio;

public class Main {

    private static final String finalAudio = "src/main/resources/miss.ogg";
    private static final WebVisualizer webVisualizer = new WebVisualizer("OpenAL (AudioSpectrum)", 8080);

    static void main() {
        AudioEngineConfig config = new AudioEngineConfig();
        System.out.println("\u001B[35m[Main] Iniciando Motor de Audio: Modo OpenAL Solitario\u001B[0m");
        webVisualizer.setVolumeCallback(OpenALAudioEngine::setVolume);
        webVisualizer.start();
        webVisualizer.waitForConnection();
        OpenALAudioEngine.startEngine();
        OpenALAudioEngine.playNewSong(finalAudio, () ->
                System.out.println("\u001B[32m[Main] OpenAL ha comenzado la reproducción nativa con éxito.\u001B[0m")
        ,config.getDelayedTaskTimePlaySong(), config.getDelayedTaskTimeUnitPlaySong());
        OpenALAudioEngine.waitForExit();
    }
    public static WebVisualizer getWebVisualizer() {
        return webVisualizer;
    }
}