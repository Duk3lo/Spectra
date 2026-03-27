package org.astral.audio;

public class Main {

    private static final String finalAudio = "src/main/resources/ashite.ogg";
    private static final WebVisualizer webVisualizer = new WebVisualizer("OpenAL (AudioSpectrum)", 8080, OpenALAudioEngine::setVolume);

    static void main() {
        System.out.println("\u001B[35m[Main] Iniciando Motor de Audio: Modo OpenAL Solitario\u001B[0m");
        webVisualizer.start();
        webVisualizer.waitForConnection();
        OpenALAudioEngine.startEngine();
        OpenALAudioEngine.playNewSong(finalAudio, () ->
                System.out.println("\u001B[32m[Main] OpenAL ha comenzado la reproducción nativa con éxito.\u001B[0m")
        );
        OpenALAudioEngine.waitForExit();
    }
    public static WebVisualizer getWebVisualizer() {
        return webVisualizer;
    }
}