package org.astral.audio;

public class Main {

    private static final String finalAudio = "src/main/resources/power.ogg";

    static void main() {
        System.out.println("\u001B[35m[Main] Iniciando Motor de Audio: Modo OpenAL Solitario\u001B[0m");

        // --- MINIM DESACTIVADO TEMPORALMENTE ---
        // System.out.println("[Main] Pre-cargando Minim...");
        // MinimAudioEngine.preloadEngine(finalAudio);

        // 1. Iniciar OpenAL en un hilo dedicado
        Thread openALThread = getThread();

        // 2. Hook de apagado limpio
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n\u001B[31m[Main] Deteniendo motor OpenAL...\u001B[0m");
            OpenALAudioEngine.shutdown();
            // MinimAudioEngine.shutdown(); // Desactivado
        }));

        System.out.println("[Main] Inicializando servidores... Revisa http://localhost:8080");

        try {
            openALThread.join();
        } catch (InterruptedException e) {
            System.err.println("[Main] Hilo principal interrumpido.");
            Thread.currentThread().interrupt();
        }
    }

    private static Thread getThread() {
        Thread openALThread = new Thread(() -> {
            // Inicialización del contexto nativo de OpenAL (vía LWJGL)
            OpenALAudioEngine.startEngine();


            // Reproducción y Callback
            OpenALAudioEngine.playNewSong(finalAudio, () -> {
                System.out.println("\u001B[32m[Main] OpenAL ha comenzado la reproducción nativa con éxito.\u001B[0m");
                // MinimAudioEngine.startPlaying(); // Desactivado
            });

            // Mantiene el hilo vivo mientras el audio suene
            OpenALAudioEngine.waitForExit();
        });

        openALThread.start();
        return openALThread;
    }
}