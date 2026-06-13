package org.astral.spectra.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.astral.spectra.logging.EngineLogger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class WebVisualizer {

    private static final String webFolder = "/web";

    private HttpServer server;
    private final List<ClientConnection> clients = new CopyOnWriteArrayList<>();

    private final String engineName;
    private final int startPort;
    private final EngineLogger logger;
    private boolean autoOpen = true;
    private volatile float lastBroadcastVolume = -1f;

    private volatile byte[] currentAudioData = null;
    private volatile long currentAudioId = 0;

    private volatile long lastUpdateMs = 0;

    private String currentUrl = "http://localhost:8080";
    private VolumeCallback volumeCallback;
    private final CountDownLatch clientConnectedLatch = new CountDownLatch(1);

    private volatile boolean audioEnabled = true;

    public interface VolumeCallback {
        void onVolumeChange(float level);
    }

    private record ClientConnection(OutputStream os, CountDownLatch keepAliveLatch) {
        public synchronized void send(byte[] data) throws IOException {
            os.write(data);
            os.flush();
        }

        public void close() {
            try { os.close(); } catch (Exception ignored) {}
            if (keepAliveLatch != null) keepAliveLatch.countDown();
        }
    }

    public WebVisualizer(String engineName, int startPort, EngineLogger logger) {
        this.engineName = engineName;
        this.startPort = startPort;
        this.logger = logger;
    }

    public void setAutoOpen(boolean autoOpen) { this.autoOpen = autoOpen; }
    public void setVolumeCallback(VolumeCallback callback) { this.volumeCallback = callback; }
    public String getUrl() { return currentUrl; }

    public void setAudioTrack(byte[] data) {
        this.currentAudioData = data;
        this.currentAudioId = System.currentTimeMillis();
    }

    /**
     * Notifica al browser si debe reproducir audio o solo mostrar la visualización.
     * Llámalo con false cuando OpenAL está activo (para evitar doble reproducción).
     */
    public void setAudioEnabled(boolean enabled) {
        this.audioEnabled = enabled;
        if (server != null && !clients.isEmpty()) {
            String msg = "{\"type\":\"audio_mode\",\"enabled\":" + enabled + "}";
            byte[] bytes = ("data: " + msg + "\n\n").getBytes(StandardCharsets.UTF_8);
            clients.removeIf(client -> {
                try { client.send(bytes); return false; } catch (IOException e) { return true; }
            });
        }
    }

    public void start() {
        int port = startPort;
        boolean started = false;

        while (!started && port <= (startPort + 10)) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                started = true;
            } catch (Exception e) {
                port++;
            }
        }

        if (!started) return;

        try {
            server.createContext("/stream", new StreamHandler());
            server.createContext("/volume", new VolumeHandler());
            server.createContext("/audio_track", new AudioTrackHandler());
            server.createContext("/", new StaticResourceHandler(webFolder));


            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();

            currentUrl = "http://localhost:" + port;
            logger.info("\u001B[32m[" + engineName + "] Web server started at " + currentUrl + "\u001B[0m");

            if (autoOpen) abrirNavegador(currentUrl);

        } catch (Exception e) {
            logger.error("Error loading web server: " + e.getMessage(), e);
        }
    }

    public void waitForConnection() {
        logger.info("\u001B[33m[" + engineName + "] Waiting for browser connection...\u001B[0m");
        try {
            boolean connected = clientConnectedLatch.await(10, TimeUnit.SECONDS);
            if (connected) logger.info("\u001B[32m[" + engineName + "] Browser synchronized!\u001B[0m");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void abrirNavegador(String url) {
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            if (osName.contains("win")) Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            else if (osName.contains("mac")) Runtime.getRuntime().exec(new String[]{"open", url});
            else Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        } catch (Exception e) {
            logger.warn("\u001B[33mOpen your browser at: " + url + "\u001B[0m");
        }
    }

    public void stop() {
        if (server != null) {
            byte[] shutdownMsg = "data: {\"type\":\"shutdown\"}\n\n".getBytes(StandardCharsets.UTF_8);
            for (ClientConnection client : clients) {
                try { client.send(shutdownMsg); } catch (IOException ignored) {}
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            clients.forEach(ClientConnection::close);
            clients.clear();
            server.stop(0);
            server = null;
        }
    }

    public void sendVolumeUpdate(float volume) {
        float v = Math.clamp(volume, 0.0f, 1.0f);
        if (Math.abs(v - lastBroadcastVolume) < 0.001f) return;
        lastBroadcastVolume = v;
        if (server == null || clients.isEmpty()) return;

        String msg = "{\"type\":\"volume_change\", \"value\":" + String.format(Locale.US, "%.2f", v) + "}";
        byte[] bytes = ("data: " + msg + "\n\n").getBytes(StandardCharsets.UTF_8);

        clients.removeIf(client -> {
            try { client.send(bytes); return false; } catch (IOException e) { return true; }
        });
    }

    public void update(float[] bars, float[] beatIntensity, Map<String, Float> features,
                       float energy, int combo, float speed, boolean isPaused,
                       float currentSecs, float totalSecs, float liveVolume) {
        if (clients.isEmpty() || server == null) return;

        long now = System.currentTimeMillis();
        if (now - lastUpdateMs < 50) return;
        lastUpdateMs = now;

        StringBuilder sb = new StringBuilder();
        sb.append("{\"bars\":[");
        for (int i = 0; i < bars.length; i++) {
            sb.append(String.format(Locale.US, "%.3f", bars[i]));
            if (i < bars.length - 1) sb.append(",");
        }

        sb.append("],\"intensities\":[");
        for (int i = 0; i < beatIntensity.length; i++) {
            sb.append(String.format(Locale.US, "%.3f", beatIntensity[i]));
            if (i < beatIntensity.length - 1) sb.append(",");
        }

        sb.append("],\"features\":{");
        int index = 0;
        for (Map.Entry<String, Float> entry : features.entrySet()) {
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":")
                    .append(String.format(Locale.US, "%.3f", entry.getValue()));
            if (index < features.size() - 1) sb.append(",");
            index++;
        }

        sb.append("},\"energy\":").append(String.format(Locale.US, "%.3f", energy))
                .append(",\"combo\":").append(combo)
                .append(",\"speed\":").append(String.format(Locale.US, "%.2f", speed))
                .append(",\"paused\":").append(isPaused)
                .append(",\"current\":").append(String.format(Locale.US, "%.3f", currentSecs))
                .append(",\"total\":").append(String.format(Locale.US, "%.3f", totalSecs))
                .append(",\"audioId\":").append(currentAudioId)
                .append(",\"audioEnabled\":").append(audioEnabled)
                .append(",\"volume\":").append(String.format(Locale.US, "%.3f", liveVolume))
                .append("}");

        byte[] bytes = ("data: " + sb + "\n\n").getBytes(StandardCharsets.UTF_8);
        clients.removeIf(client -> {
            try { client.send(bytes); return false; } catch (IOException e) { return true; }
        });
    }

    private @NotNull String escapeJson(@NotNull String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    class AudioTrackHandler implements HttpHandler {
        @Override
        public void handle(@NotNull HttpExchange exchange) throws IOException {
            byte[] data = currentAudioData;
            if (data == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            exchange.getResponseHeaders().add("Content-Type", "audio/ogg");
            exchange.getResponseHeaders().add("Accept-Ranges", "bytes");

            String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                try {
                    String range = rangeHeader.substring(6);
                    String[] split = range.split("-");
                    int start = Integer.parseInt(split[0]);
                    int end = split.length > 1 && !split[1].isEmpty()
                            ? Integer.parseInt(split[1]) : data.length - 1;

                    if (start >= data.length) {
                        exchange.getResponseHeaders().add("Content-Range", "bytes */" + data.length);
                        exchange.sendResponseHeaders(416, -1);
                        exchange.close();
                        return;
                    }

                    end = Math.min(end, data.length - 1);
                    int length = end - start + 1;

                    exchange.getResponseHeaders().add("Content-Range",
                            "bytes " + start + "-" + end + "/" + data.length);
                    exchange.sendResponseHeaders(206, length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        byte[] buffer = new byte[8192];
                        int bytesWritten = 0;
                        while (bytesWritten < length) {
                            int toWrite = Math.min(buffer.length, length - bytesWritten);
                            os.write(data, start + bytesWritten, toWrite);
                            os.flush();
                            bytesWritten += toWrite;
                        }
                    }
                    return;
                } catch (Exception ignored) {
                }
            }

            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[8192];
                int bytesWritten = 0;
                while (bytesWritten < data.length) {
                    int toWrite = Math.min(buffer.length, data.length - bytesWritten);
                    os.write(data, bytesWritten, toWrite);
                    os.flush();
                    bytesWritten += toWrite;
                }
            } catch (Exception ignored) {}
        }
    }

    class StaticResourceHandler implements HttpHandler {
        private final String baseFolder;
        public StaticResourceHandler(String baseFolder) { this.baseFolder = baseFolder; }

        @Override
        public void handle(@NotNull HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            String resourcePath = baseFolder + path;

            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] responseBytes = is.readAllBytes();
                if (path.endsWith(".html")) {
                    String content = new String(responseBytes, StandardCharsets.UTF_8);
                    content = content.replace("{{ENGINE_NAME}}", engineName.toUpperCase());
                    responseBytes = content.getBytes(StandardCharsets.UTF_8);
                }
                exchange.getResponseHeaders().add("Content-Type", getMimeType(path));
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(responseBytes); }
            }
        }

        private @NotNull String getMimeType(@NotNull String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css"))  return "text/css";
            if (path.endsWith(".js"))   return "application/javascript";
            if (path.endsWith(".png"))  return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".svg"))  return "image/svg+xml";
            if (path.endsWith(".ico"))  return "image/x-icon";
            if (path.endsWith(".json")) return "application/json";
            return "application/octet-stream";
        }
    }

    class VolumeHandler implements HttpHandler {
        @Override
        public void handle(@NotNull HttpExchange exchange) throws IOException {
            String q = exchange.getRequestURI().getQuery();
            if (q != null && q.startsWith("level=")) {
                try {
                    if (volumeCallback != null)
                        volumeCallback.onVolumeChange(Float.parseFloat(q.split("=")[1]));
                } catch (Exception ignored) {}
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        }
    }

    class StreamHandler implements HttpHandler {
        @Override
        public void handle(@NotNull HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream; charset=UTF-8");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().add("Connection", "keep-alive");
            exchange.getResponseHeaders().add("X-Accel-Buffering", "no");
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();
            CountDownLatch keepAliveLatch = new CountDownLatch(1);
            ClientConnection client = new ClientConnection(os, keepAliveLatch);
            clients.add(client);
            clientConnectedLatch.countDown();

            try {
                String initMsg = "{\"type\":\"volume_change\", \"value\":"
                        + String.format(Locale.US, "%.2f",
                        lastBroadcastVolume >= 0 ? lastBroadcastVolume : 0.1f) + "}";
                client.send(("data: " + initMsg + "\n\n").getBytes(StandardCharsets.UTF_8));

                String audioModeMsg = "{\"type\":\"audio_mode\",\"enabled\":" + audioEnabled + "}";
                client.send(("data: " + audioModeMsg + "\n\n").getBytes(StandardCharsets.UTF_8));

                byte[] pingBytes = ":\n\n".getBytes(StandardCharsets.UTF_8);

                while (!Thread.currentThread().isInterrupted()) {
                    boolean shouldExit = keepAliveLatch.await(2, TimeUnit.SECONDS);
                    if (shouldExit) break;
                    client.send(pingBytes);
                }

            } catch (IOException ignored) {
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                clients.remove(client);
                try { os.close(); }       catch (Exception ignored) {}
                try { exchange.close(); } catch (Exception ignored) {}
            }
        }
    }
}
