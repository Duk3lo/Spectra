package org.astral.spectyle.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.astral.spectyle.logging.EngineLogger;
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
import java.util.concurrent.TimeUnit;

public final class WebVisualizer {

    private static final String webFolder = "/web";

    private HttpServer server;
    private final List<OutputStream> clients = new CopyOnWriteArrayList<>();

    private final String engineName;
    private final int startPort;
    private final EngineLogger logger;
    private volatile float lastBroadcastVolume = -1f;

    private String currentUrl = "http://localhost:8080";

    private VolumeCallback volumeCallback;

    private final CountDownLatch clientConnectedLatch = new CountDownLatch(1);

    public interface VolumeCallback {
        void onVolumeChange(float level);
    }

    public WebVisualizer(String engineName, int startPort, EngineLogger logger) {
        this.engineName = engineName;
        this.startPort = startPort;
        this.logger = logger;
    }

    public void setVolumeCallback(VolumeCallback callback) {
        this.volumeCallback = callback;
    }

    public String getUrl() {
        return currentUrl;
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
            server.createContext("/", new StaticResourceHandler(webFolder));

            server.setExecutor(null);
            server.start();

            currentUrl = "http://localhost:" + port;
            logger.info("\u001B[32m[" + engineName + "] Web server started at " + currentUrl + "\u001B[0m");

            abrirNavegador(currentUrl);

        } catch (Exception e) {
            logger.error("Error loading web server: " + e.getMessage(), e);
        }
    }

    public void waitForConnection() {
        logger.info("\u001B[33m[" + engineName + "] Waiting for browser connection...\u001B[0m");
        try {
            boolean connected = clientConnectedLatch.await(10, TimeUnit.SECONDS);
            if (connected) logger.info("\u001B[32m[" + engineName + "] Browser synchronized!\u001B[0m");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void abrirNavegador(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            else if (os.contains("mac")) Runtime.getRuntime().exec(new String[]{"open", url});
            else Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        } catch (Exception e) {
            logger.warn("\u001B[33mOpen your browser at: " + url + "\u001B[0m");
        }
    }

    public void stop() {
        if (server != null) {
            clients.forEach(os -> { try { os.close(); } catch (Exception ignored) {} });
            clients.clear();
            server.stop(0);
            server = null;
        }
    }

    public void sendVolumeUpdate(float volume) {
        if (server == null || clients.isEmpty()) return;

        float v = Math.clamp(volume, 0.0f, 1.0f);
        if (Math.abs(v - lastBroadcastVolume) < 0.001f) return;

        lastBroadcastVolume = v;

        String msg = "{\"type\":\"volume_change\", \"value\":" +
                String.format(Locale.US, "%.2f", v) + "}";

        byte[] bytes = ("data: " + msg + "\n\n").getBytes(StandardCharsets.UTF_8);

        clients.removeIf(os -> {
            try {
                os.write(bytes);
                os.flush();
                return false;
            } catch (IOException e) {
                return true;
            }
        });
    }

    public void update(float[] bars,
                       float[] beatIntensity,
                       Map<String, Float> features,
                       float energy,
                       int combo,
                       float speed,
                       boolean isPaused,
                       float currentSecs,
                       float totalSecs) {
        if (clients.isEmpty() || server == null) return;

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
                .append(",\"current\":").append(String.format(Locale.US, "%.1f", currentSecs))
                .append(",\"total\":").append(String.format(Locale.US, "%.1f", totalSecs))
                .append("}");

        byte[] bytes = ("data: " + sb + "\n\n").getBytes(StandardCharsets.UTF_8);
        clients.removeIf(os -> {
            try {
                os.write(bytes);
                os.flush();
                return false;
            } catch (IOException e) {
                return true;
            }
        });
    }

    private @NotNull String escapeJson(@NotNull String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    class StaticResourceHandler implements HttpHandler {
        private final String baseFolder;

        public StaticResourceHandler(String baseFolder) {
            this.baseFolder = baseFolder;
        }

        @Override
        public void handle(@NotNull HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/")) {
                path = "/index.html";
            }

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

                String mimeType = getMimeType(path);
                exchange.getResponseHeaders().add("Content-Type", mimeType);

                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        }

        private @NotNull String getMimeType(@NotNull String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".svg")) return "image/svg+xml";
            if (path.endsWith(".ico")) return "image/x-icon";
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
                    if (volumeCallback != null) {
                        volumeCallback.onVolumeChange(Float.parseFloat(q.split("=")[1]));
                    }
                } catch (Exception ignored) {}
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        }
    }

    class StreamHandler implements HttpHandler {
        @Override
        public void handle(@NotNull HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);

            OutputStream os = exchange.getResponseBody();
            clients.add(os);
            clientConnectedLatch.countDown();
            String msg = "{\"type\":\"volume_change\", \"value\":" +
                    String.format(Locale.US, "%.2f", lastBroadcastVolume >= 0 ? lastBroadcastVolume : 0.1f) + "}";
            byte[] bytes = ("data: " + msg + "\n\n").getBytes(StandardCharsets.UTF_8);
            os.write(bytes);
            os.flush();
        }
    }
}