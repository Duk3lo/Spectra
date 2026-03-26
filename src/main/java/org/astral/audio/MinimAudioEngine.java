package org.astral.audio;

import ddf.minim.*;
import ddf.minim.analysis.*;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.Arrays;
import java.util.concurrent.*;
import javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader; // Importación directa para forzar carga

public class MinimAudioEngine {

    private static Minim minim;
    private static AudioPlayer player;
    private static BeatDetect beat;
    private static FFT fft;
    private static WebVisualizer webVisualizer;

    private static final int NUM_BARS = 16;
    private static final float[] smoothedBars = new float[NUM_BARS];
    private static final float[] beatIntensity = new float[NUM_BARS];
    private static final float[] bandMaxes = new float[NUM_BARS];
    private static final float[] previousFrame = new float[NUM_BARS];
    private static final long[] lastEventTime = new long[NUM_BARS];

    private static ScheduledExecutorService executor;
    private static boolean engineRunning = false;

    static {
        Arrays.fill(bandMaxes, 1.0f);

        // --- REGISTRO FORZADO DE SPI ---
        // Esto obliga a Java a incluir el decodificador OGG si está en el classpath
        try {
            System.out.println("[Minim] Forzando registro de VorbisSPI...");
            VorbisAudioFileReader v = new VorbisAudioFileReader();
            // Si la línea de arriba no falla, el driver está presente.
        } catch (Throwable t) {
            System.err.println("[Minim] CRÍTICO: No se pudo instanciar VorbisAudioFileReader.");
        }

        System.out.println("\u001B[36m[Minim] Formatos detectados en AudioSystem:\u001B[0m");
        // Verificación real de lo que Java Sound "ve"
        var services = AudioSystem.getAudioFileTypes();
        if (services.length == 0) System.out.println(" -> Ninguno (SPI fallando)");
        for (var type : services) {
            System.out.println(" -> " + type.getExtension());
        }
    }

    public static void preloadEngine(String filePath) {
        // Asegurar que el hilo de carga tenga el ClassLoader correcto
        Thread.currentThread().setContextClassLoader(MinimAudioEngine.class.getClassLoader());

        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            System.err.println("[Minim] Error: Archivo no encontrado en " + audioFile.getAbsolutePath());
            return;
        }

        // Cargador simplificado pero robusto
        minim = new Minim(new Object() {
            public String sketchPath(String fileName) {
                return new File(fileName).getAbsolutePath();
            }
            public InputStream createInput(String fileName) {
                try {
                    // El buffer es vital para que Vorbis lea las cabeceras (header marks)
                    return new BufferedInputStream(new FileInputStream(new File(sketchPath(fileName))));
                } catch (Exception e) {
                    return null;
                }
            }
        });

        System.out.println("[Minim] Intentando cargar: " + audioFile.getName());

        try {
            // TRUCO: A veces Minim falla con rutas absolutas en Linux. Intentamos normalizar.
            player = minim.loadFile(audioFile.getCanonicalPath(), 1024);

            if (player == null) {
                throw new Exception("Minim devolvió null tras intentar decodificar.");
            }

            fft = new FFT(player.bufferSize(), player.sampleRate());
            beat = new BeatDetect(player.bufferSize(), player.sampleRate());
            beat.setSensitivity(250);

            webVisualizer = new WebVisualizer("Minim (Stems)", 8081, level -> {
                if (player != null) {
                    float db = (level <= 0.01f) ? -80f : (float)(20 * Math.log10(level));
                    player.setGain(db);
                }
            });
            webVisualizer.start();
            System.out.println("\u001B[32m[MinimEngine] OK: Sistema listo.\u001B[0m");

        } catch (Exception e) {
            System.err.println("\u001B[31m[Minim] ERROR DE CARGA: " + e.getMessage() + "\u001B[0m");
        }
    }

    public static void startPlaying() {
        if (player != null) {
            player.play();
            engineRunning = true;
            runLoop();
        }
    }

    private static void runLoop() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if (!engineRunning || player == null || !player.isPlaying()) return;

            fft.forward(player.mix);
            beat.detect(player.mix);
            float totalEnergy = 0;
            long currentTime = System.currentTimeMillis();

            for (int i = 0; i < NUM_BARS; i++) {
                float lowFreq = i * (fft.specSize() / (float)NUM_BARS);
                float hiFreq = (i + 1) * (fft.specSize() / (float)NUM_BARS);
                float rawValue = fft.calcAvg(lowFreq, hiFreq);

                if (rawValue > bandMaxes[i]) bandMaxes[i] = rawValue;
                else bandMaxes[i] *= 0.995f;

                float normalizedValue = rawValue / Math.max(0.1f, bandMaxes[i]);
                float speed = (normalizedValue > smoothedBars[i]) ? 0.8f : 0.2f;
                smoothedBars[i] += (normalizedValue - smoothedBars[i]) * speed;
                totalEnergy += smoothedBars[i];

                if (normalizedValue > previousFrame[i] * 1.4f && (currentTime - lastEventTime[i] > 200)) {
                    beatIntensity[i] = 1.0f;
                    lastEventTime[i] = currentTime;
                } else {
                    beatIntensity[i] = Math.max(0, beatIntensity[i] - 0.08f);
                }
                previousFrame[i] = normalizedValue;
            }

            if (webVisualizer != null) {
                webVisualizer.update(smoothedBars, beatIntensity, totalEnergy/NUM_BARS, 0, 1.0f, !player.isPlaying(), player.position()/1000f, player.length()/1000f);
            }
        }, 0, 16, TimeUnit.MILLISECONDS);
    }

    public static void shutdown() {
        engineRunning = false;
        if (executor != null) executor.shutdownNow();
        if (player != null) player.close();
        if (minim != null) minim.stop();
        if (webVisualizer != null) webVisualizer.stop();
    }
}