package org.astral.audio;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebVisualizer {

    private static HttpServer server;
    private static final List<OutputStream> clients = new CopyOnWriteArrayList<>();

    public static void start() {
        int port = 8080;
        boolean started = false;

        while (!started && port <= 8090) {
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                started = true;
            } catch (Exception e) {
                port++;
            }
        }

        if (!started) return;

        try {
            server.createContext("/", new HtmlHandler());
            server.createContext("/stream", new StreamHandler());
            // --- ADICIÓN: Nueva ruta para recibir el volumen ---
            server.createContext("/volume", new VolumeHandler());

            server.setExecutor(null);
            server.start();

            String url = "http://localhost:" + port;
            System.out.println("\u001B[32m[WebVisualizer] Servidor web iniciado en " + url + "\u001B[0m");

            abrirNavegador(url);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void abrirNavegador(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            System.out.println("\u001B[33mAbre tu navegador y entra a: " + url + "\u001B[0m");
        }
    }

    public static void stop() {
        if (server != null) {
            System.out.println("\u001B[33m[WebVisualizer] Desconectando clientes web y apagando servidor...\u001B[0m");
            for (OutputStream os : clients) {
                try { os.close(); } catch (IOException ignored) {}
            }
            clients.clear();
            server.stop(0);
            server = null;
        }
    }

    public static void update(float[] bars, float[] beatIntensity, float energy, int combo, float speed, boolean isPaused) {
        if (clients.isEmpty() || server == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"bars\":[");
        for (int i = 0; i < bars.length; i++) {
            sb.append(String.format(java.util.Locale.US, "%.3f", bars[i]));
            if (i < bars.length - 1) sb.append(",");
        }
        sb.append("],\"intensities\":[");
        for (int i = 0; i < beatIntensity.length; i++) {
            sb.append(String.format(java.util.Locale.US, "%.3f", beatIntensity[i]));
            if (i < beatIntensity.length - 1) sb.append(",");
        }
        sb.append("],\"energy\":").append(String.format(java.util.Locale.US, "%.3f", energy));
        sb.append(",\"combo\":").append(combo);
        sb.append(",\"speed\":").append(String.format(java.util.Locale.US, "%.2f", speed));
        sb.append(",\"paused\":").append(isPaused);
        sb.append("}");

        String payload = "data: " + sb.toString() + "\n\n";
        byte[] bytes = payload.getBytes();

        for (OutputStream os : clients) {
            try {
                os.write(bytes);
                os.flush();
            } catch (IOException e) {
                clients.remove(os);
            }
        }
    }

    // --- ADICIÓN: Handler para procesar el volumen ---
    static class VolumeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("level=")) {
                try {
                    float level = Float.parseFloat(query.split("=")[1]);
                    BeatAudioEngine.setVolume(level);
                } catch (Exception ignored) {}
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        }
    }

    static class StreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, 0);
            clients.add(exchange.getResponseBody());
        }
    }

    static class HtmlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = getHtmlContent();
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        private String getHtmlContent() {
            return """
                    <!DOCTYPE html>
                    <html lang='es'>
                    <head>
                        <meta charset='UTF-8'>
                        <title>BeatEngine Pro - Visualizer</title>
                        <style>
                            body { background-color: #0d0d12; color: #fff; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; overflow: hidden; }
                            h1 { margin-bottom: 10px; color: #00ffcc; text-shadow: 0 0 10px #00ffcc; }
                            .container { display: flex; align-items: flex-end; justify-content: center; gap: 8px; height: 300px; padding: 20px; background: rgba(255,255,255,0.05); border-radius: 15px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); border: 1px solid rgba(255,255,255,0.1); width: 80%; max-width: 900px; }
                            .bar-wrapper { display: flex; flex-direction: column; align-items: center; justify-content: flex-end; width: 100%; height: 100%; }
                            .bar { width: 100%; background-color: #444; border-radius: 5px 5px 0 0; transition: height 0.05s ease-out, background-color 0.1s, box-shadow 0.1s; height: 5%; }
                            .label { margin-top: 10px; font-size: 10px; font-weight: bold; color: #888; text-transform: uppercase; }
                            .stats { display: flex; gap: 40px; margin-top: 30px; padding: 20px; background: rgba(0,0,0,0.3); border-radius: 10px; width: 80%; max-width: 900px; justify-content: space-around; }
                            .stat-box { text-align: center; }
                            .stat-value { font-size: 24px; font-weight: bold; color: #fff; margin-top: 5px; text-shadow: 0 0 10px rgba(255,255,255,0.5); }
                            .stat-title { font-size: 12px; color: #aaa; }
                            #status { position: absolute; top: 20px; left: 20px; color: #ff3366; font-weight: bold; }
                            #overlay { display: none; position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.9); z-index: 999; flex-direction: column; align-items: center; justify-content: center; }
                            /* --- ADICIÓN: Estilo del Control de Volumen --- */
                            .vol-cont { margin-top: 20px; background: rgba(0,0,0,0.4); padding: 15px 30px; border-radius: 50px; border: 1px solid rgba(0,255,204,0.3); display: flex; align-items: center; gap: 15px; }
                            input[type=range] { cursor: pointer; accent-color: #00ffcc; width: 200px; }
                        </style>
                    </head>
                    <body>
                        <div id='overlay'><h1 style='color:#ff3366; text-shadow: 0 0 20px #ff3366; font-size: 50px;'>JUEGO CERRADO</h1><p>Esta pestaña se puede cerrar de forma segura...</p></div>
                        <div id='status'>EN PAUSA</div>
                        <h1>BEAT ENGINE PRO</h1>
                        <div class='container' id='bars-container'></div>
                        <div class='stats'>
                            <div class='stat-box'><div class='stat-title'>ENERGÍA GLOBAL</div><div class='stat-value' id='energy-val'>0%</div></div>
                            <div class='stat-box'><div class='stat-title'>RACHA HITS</div><div class='stat-value' id='combo-val' style='color:#ff007f;'>0x</div></div>
                            <div class='stat-box'><div class='stat-title'>VELOCIDAD PARTÍCULAS</div><div class='stat-value' id='speed-val'>1.00x</div></div>
                        </div>
                    
                       \s
                        <div class='vol-cont'>
                            <span class='stat-title'>VOLUMEN</span>
                            <input type='range' id='vol-slider' min='0' max='1' step='0.01' value='0.1'>
                            <span id='vol-label' class='stat-value' style='font-size: 16px; min-width: 45px;'>10%</span>
                        </div>
                    
                        <script>
                            const labels = ['SUB', 'KICK', 'BASS', 'LOW-M', 'MID 1', 'SNARE', 'MID 3', 'MID 4', 'HI-M1', 'HI-M2', 'HAT 1', 'HAT 2', 'HIGH 1', 'HIGH 2', 'AIR 1', 'AIR 2'];
                            const container = document.getElementById('bars-container');
                            const barElements = [];
                            for(let i=0; i<16; i++) {
                                let wrap = document.createElement('div'); wrap.className = 'bar-wrapper';
                                let bar = document.createElement('div'); bar.className = 'bar';
                                let lbl = document.createElement('div'); lbl.className = 'label'; lbl.innerText = labels[i];
                                wrap.appendChild(bar); wrap.appendChild(lbl);
                                container.appendChild(wrap); barElements.push(bar);
                            }
                           \s
                            const source = new EventSource('/stream');
                           \s
                            source.onerror = function(event) {
                                 source.close();
                                 document.getElementById('overlay').style.display = 'flex';
                                 setTimeout(() => window.close(), 1500);
                            };
                    
                            // --- ADICIÓN: Lógica del Slider ---
                            const slider = document.getElementById('vol-slider');
                            const volLabel = document.getElementById('vol-label');
                            slider.oninput = function() {
                                const val = this.value;
                                volLabel.innerText = Math.round(val * 100) + '%';
                                fetch('/volume?level=' + val);
                            };
                            // Inicializar volumen bajo en el motor al cargar
                            fetch('/volume?level=0.1');
                    
                            source.onmessage = function(event) {
                                const data = JSON.parse(event.data);
                                document.getElementById('status').innerText = data.paused ? '⏸ EN PAUSA' : '▶ REPRODUCIENDO';
                                document.getElementById('status').style.color = data.paused ? '#ff3366' : '#00ffcc';
                                document.getElementById('energy-val').innerText = Math.round(data.energy * 100) + '%';
                                document.getElementById('combo-val').innerText = data.combo + 'x';
                                document.getElementById('speed-val').innerText = data.speed.toFixed(2) + 'x';
                               \s
                                for(let i=0; i<16; i++) {
                                    let h = Math.pow(Math.min(1.0, data.bars[i]), 1.2) * 100;
                                    if (h < 2) h = 2;
                                    barElements[i].style.height = h + '%';
                                    let intensity = data.intensities[i];
                                    let color, shadow;
                                    if (intensity > 0.8) { color = '#ffffff'; shadow = '0 0 15px #fff'; }
                                    else if (intensity > 0.4) {
                                        if (i < 3) { color = '#ff3366'; shadow = '0 0 15px #ff3366'; }
                                        else if (i < 8) { color = '#00ffcc'; shadow = '0 0 15px #00ffcc'; }
                                        else { color = '#33ccff'; shadow = '0 0 15px #33ccff'; }
                                    } else {
                                        if (i < 3) color = '#881133';
                                        else if (i < 8) color = '#006655';
                                        else color = '#115588';
                                        shadow = 'none';
                                    }
                                    barElements[i].style.backgroundColor = color;
                                    barElements[i].style.boxShadow = shadow;
                                }
                            };
                        </script>
                    </body>
                    </html>""";
        }
    }
}